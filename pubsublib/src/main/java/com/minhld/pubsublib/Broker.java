package com.minhld.pubsublib;

import android.content.Context;

import com.minhld.jobex.Job;
import com.minhld.jobex.JobDataParser;
import com.minhld.jobex.JobPackage;
import com.minhld.pbsbjob.AckServer;
import com.minhld.utils.Utils;

import org.zeromq.ZMQ;

import java.util.HashMap;

/**
 * Created by minhld on 8/4/2016.
 * This class work as a broker among the devices in mobile network.
 * The broker has several modes. To switch mode, please use the mode
 * type switcher:
 *  - Publish-Subscribe mode
 *  - Router mode
 *
 */
public class Broker extends Thread {
    private String brokerIp = "*";

    private HashMap<String, WorkerInfo> workerList;
    HashMap<String, JobMergeInfo> jobMergeList;

    private Context parentContext;
    private static ZMQ.Socket backend;
    private AckServerListener ackServer;

    public Broker(Context parentContext, String brokerIp) {
        this.parentContext = parentContext;
        this.brokerIp = brokerIp;
        this.start();
    }

    public void run() {
        // this switch
        initRouterMode();
    }

    /**
     * this function is only called when developer invoke pub-sub mode (default)
     * which is for data transmission only
     */
    private void initPubSubMode() {
        ZMQ.Context context = ZMQ.context(1);

        // initiate publish socket
        String xpubUri = "tcp://" + this.brokerIp + ":" + Utils.BROKER_XSUB_PORT;
        ZMQ.Socket xpubSk = context.socket(ZMQ.XPUB);
        xpubSk.bind(xpubUri);

        // initiate subscribe socket
        String xsubUri = "tcp://" + this.brokerIp + ":" + Utils.BROKER_XPUB_PORT;
        ZMQ.Socket xsubSk = context.socket(ZMQ.XSUB);
        xsubSk.bind(xsubUri);

        // bind the two sockets together - this will suspend here to listen
        ZMQ.proxy(xsubSk, xpubSk, null);

        xsubSk.close();
        xpubSk.close();
        context.term();
    }

    /**
     * this function is called when developer invoke router mode
     * which is for job distribution
     */
    private void initRouterMode() {
        ZMQ.Context context = ZMQ.context(1);

        // initiate publish socket
        String frontendPort = "tcp://" + this.brokerIp + ":" + Utils.BROKER_XSUB_PORT;
        ZMQ.Socket frontend = context.socket(ZMQ.ROUTER);
        frontend.bind(frontendPort);

        // initiate subscribe socket
        String backendPort = "tcp://" + this.brokerIp + ":" + Utils.BROKER_XPUB_PORT;
        this.backend = context.socket(ZMQ.ROUTER);
        backend.bind(backendPort);

        // Queue of available workers
        workerList = new HashMap<String, WorkerInfo>();

        // Map of job results
        jobMergeList = new HashMap<String, JobMergeInfo>();

        // initiate ACK server
        ackServer = new AckServerListener(parentContext, context, this.brokerIp);

        String workerId, clientId;
        byte[] empty, request, reply;
        while (!Thread.currentThread().isInterrupted()) {
            ZMQ.Poller items = new ZMQ.Poller(2);
            items.register(backend, ZMQ.Poller.POLLIN);
            items.register(frontend, ZMQ.Poller.POLLIN);

            if (items.poll() < 0)
                break;

            // HANDLE WORKER'S ACTIVITY ON BACK-END
            if (items.pollin(0)) {
                // queue worker address for LRU routing
                // FIRST FRAME is WORKER ID
                workerId = backend.recvStr();

                // SECOND FRAME is a DELIMITER, empty
                empty = backend.recv();
                assert (empty.length == 0);

                // get THIRD FRAME
                //  - is READY (worker reports with DRL)
                //  - or CLIENT ID (worker returns results)
                clientId = backend.recvStr();

                if (clientId.equals(Utils.WORKER_READY)) {
                    // WORKER has finished loading, returned DRL value
                    // update worker list
                    workerList.put(workerId, new WorkerInfo(workerId));
                    ackServer.updateWorkerNumbers(workerList.size());

                } else {
                    // WORKER has completed the task, returned the results

                    // get FORTH FRAME, should be EMPTY - check the delimiter again
                    empty = backend.recv();
                    assert (empty.length == 0);

                    // get LAST FRAME - main result from worker
                    reply = backend.recv();

                    // flush them out to the front-end
                    frontend.sendMore(clientId);
                    frontend.sendMore(Utils.BROKER_DELIMITER);
                    frontend.send(reply);
                }
            }

            // HANDLE CLIENT'S ACTIVITIES AT FRONT-END
            if (items.pollin(1)) {
                // now get next client request, route to LRU worker
                // client request is [address][empty][request]
                clientId = frontend.recvStr();

                // check 2nd frame
                empty = frontend.recv();
                assert (empty.length == 0);

                // get 3rd frame
                request = frontend.recv();

                ackServer.queryDRL(clientId, request);

            }

        }

        frontend.close();
        backend.close();
        context.term();
    }

    static class AckServerListener extends AckServer {
        static String clientId;
        // static byte[] request;
        static JobPackage request;
        static float totalDRL = 0;
        static HashMap<String, Float> advancedWorkerList;

        public AckServerListener(final Context parentContext, ZMQ.Context context, String brokerIp) {
            super(context, brokerIp, new AckListener() {
                @Override
                public void allAcksReceived() {
                    // when all returning ACKs are received, this event will be invoked to dispatch
                    // tasks (pieces) to the workers

                    try {
                        // get job classes from the JAR (in binary format)
                        byte[] jobBytes = AckServerListener.request.jobBytes;

                        // initiate the data parser from the JAR
                        JobDataParser dataParser = new JobDataParserImpl(); // JobHelper.getDataParser(parentContext, AckServerListener.clientId, jobBytes);

                        // get the whole object sent from client
                        Object dataObject = null;
                        try {
                            dataObject = dataParser.parseBytesToObject(AckServerListener.request.dataBytes);
                        } catch (Exception e) {
                            // this case shouldn't be happened
                            e.printStackTrace();
                        }

                        // before dividing job into parts, a placeholder to hold cumulative results
                        // must be created
                        Object emptyPlaceholder = dataParser.createPlaceHolder(dataObject);
                        JobMergeInfo jobMergeInfo = new JobMergeInfo(AckServerListener.request.clientId, emptyPlaceholder);


                        // send job to worker
                        JobPackage taskPkg;
                        float currCummDRL = 0, newCummDRL = 0;
                        int currCummDRLNum = 0, newCummDRLNum = 0;
                        byte[] dataPart;
                        String useClientId;

                        for (String workerId : AckServerListener.advancedWorkerList.keySet()) {
                            // create parts with size proportional to the DRL value of each worker
                            newCummDRL = currCummDRL + AckServerListener.advancedWorkerList.get(workerId).floatValue();

                            // convert to integer number
                            currCummDRLNum = (int) (currCummDRL * 100 / totalDRL);
                            newCummDRLNum = (int) (newCummDRL * 100 / totalDRL);

                            dataPart = dataParser.getPartFromObject(dataObject, currCummDRLNum, newCummDRLNum);

                            // reassign the cumulative DRL
                            currCummDRL = newCummDRL;

                            // and wrap up as a task
                            taskPkg = new JobPackage(0, AckServerListener.clientId, dataPart, jobBytes);

                            // wrap up and send to the appropriate worker
                            useClientId = AckServerListener.clientId + Utils.ID_DELIMITER + currCummDRLNum + Utils.ID_DELIMITER + newCummDRLNum;

                            backend.sendMore(workerId);
                            backend.sendMore(Utils.BROKER_DELIMITER);
                            backend.sendMore(useClientId); // backend.sendMore(AckServerListener.clientId);
                            backend.sendMore(Utils.BROKER_DELIMITER);
                            backend.send(taskPkg.toByteArray());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // remove all existences from current work - to prepare serving form the new work
                        AckServerListener.advancedWorkerList.clear();
                    }
                }
            });

            advancedWorkerList = new HashMap<>();
        }

        public void queryDRL(String clientId, byte[] request) {
            AckServerListener.clientId = clientId;
            try {
                AckServerListener.request = (JobPackage) Utils.deserialize(request);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // query resource information from remote workers
            this.sendAck();
        }

        @Override
        public void receiveResponse(byte[] resp) {
            String respStr = new String(resp);

            // remote device's resource info received
            System.out.println("received " + respStr);

            // analyze response and add it to the array of WorkerInfos
            // to do here
            float drl = (float) Utils.getResponse(respStr, "drl");
            String workerId = (String) Utils.getResponse(respStr, "id");

            // compare DRL with client's DRL and add to the list
            // will not add more than 3 devices
            // if (drl > 0 && advancedWorkerList.size() < Utils.MAX_WORKERS_PER_JOB) {
            if (drl > AckServerListener.request.DRL && advancedWorkerList.size() < Utils.MAX_WORKERS_PER_JOB) {
                totalDRL = (advancedWorkerList.size() == 0) ? drl : totalDRL + drl;
                advancedWorkerList.put(workerId, drl);
            }
        }
    }

    /**
     *
     *
     * @param useClientId
     * @param taskBytes
     * @param currPos
     * @param newPos
     */
    private JobMergeInfo mergeTaskResults(String useClientId, byte[] taskBytes, int currPos, int newPos) {
        String[] idParts = useClientId.split(Utils.ID_DELIMITER);
        String clientId = idParts[0];

        JobMergeInfo jobInfo = jobMergeList.get(clientId);

        return jobInfo;
    }

    class WorkerInfo {
        public String workerId;
        public float DRL;

        public WorkerInfo(String workerId) {
            this.workerId = workerId;
            this.DRL = 0;
        }

        public WorkerInfo(String workerId, float drl) {
            this.workerId = workerId;
            this.DRL = drl;
        }
    }

    class JobMergeInfo {
        public String clientId;
        public int partCummNum;
        public int partNum;
        public Object placeholder;

        public JobMergeInfo(String clientId) {
            this.clientId = clientId;
            this.partCummNum = 0;
            this.partNum = 0;
        }

        public JobMergeInfo(String clientId, int partCummNum, int partNum) {
            this.clientId = clientId;
            this.partCummNum = partCummNum;
            this.partNum = partNum;
        }

        public JobMergeInfo(String clientId, Object emptyPlaceholder) {
            this.clientId = clientId;
            this.partCummNum = 0;
            this.partNum = 0;
            this.placeholder = emptyPlaceholder;
        }
    }
}
