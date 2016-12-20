package com.minhld.pubsublib;

import android.content.Context;

import com.minhld.jobex.JobDataParser;
import com.minhld.jobex.JobPackage;
import com.minhld.jobimpls.EmptyDataParserImpl;
import com.minhld.jobimpls.JobDataParserImpl;
import com.minhld.jobimpls.NetDataParserImpl;
import com.minhld.jobimpls.WordDataParserImpl;
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
    private static HashMap<String, JobMergeInfo> jobMergeList;

    private Context parentContext;
    private static ZMQ.Socket backend;
    private AckServerListener ackServer;

    long startTime = 0;
    static long startRLRequestTime = 0;


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

        // Map of job placeholders - hold the placeholders of all the current executing jobs
        Broker.jobMergeList = new HashMap<String, JobMergeInfo>();

        // initiate ACK server
        ackServer = new AckServerListener(parentContext, context, this.brokerIp);

        String workerId, clientId;
        byte[] empty, request, reply;
        while (!Thread.currentThread().isInterrupted()) {
            ZMQ.Poller items = new ZMQ.Poller(2);
            items.register(backend, ZMQ.Poller.POLLIN);
            items.register(frontend, ZMQ.Poller.POLLIN);

            // hold until there is any messages from workers or clients
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

                    // retrieve the job's placeholder
                    JobMergeInfo jobMergeInfo = mergeTaskResults(clientId, reply);

                    // check if the job's placeholder is fully filled
                    // if so, return the placeholder back to the client
                    // otherwise, skip this one and wait for more parts to come
                    if (jobMergeInfo.isPlaceholderFilled()) {
                        // flush them out to the front-end
                        frontend.sendMore(jobMergeInfo.clientId);
                        frontend.sendMore(Utils.BROKER_DELIMITER);
                        frontend.send(jobMergeInfo.getPlaceholder());

                        // remove the job result out of the result map
                        Broker.jobMergeList.remove(jobMergeInfo.clientId);

                        // end the clock of total job
                        long jobDurr = System.currentTimeMillis() - startTime;
                        System.out.println("[broker] total time doing job is: " + jobDurr + "ms");
                    }
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

                // send the requests to all the nearby workers for DRL values. After receiving
                // all DRL values, it will consider DRLs and divide job into tasks with
                // proportional data amounts to the DRL values.
                ackServer.queryDRL(clientId, request);

            }

        }

        frontend.close();
        backend.close();
        context.term();
    }

    static class AckServerListener extends AckServer {
        static String clientId;
        static JobPackage request;
        static float totalDRL = 0;
        static HashMap<String, Float> advancedWorkerList;

        public AckServerListener(final Context parentContext, ZMQ.Context context, String brokerIp) {
            super(context, brokerIp, new AckListener() {
                @Override
                public void allAcksReceived() {
                    // end the RL request clock
                    long requestRLDurr = System.currentTimeMillis() - startRLRequestTime;
                    System.out.println("total RL request time: " + requestRLDurr + "ms");

                    // when all returning ACKs are received, this event will be invoked to dispatch
                    // tasks (pieces) to the workers

                    try {
                        // get job classes from the JAR (in binary format)
                        byte[] jobBytes = AckServerListener.request.jobBytes;

                        // initiate the data parser from the JAR

                        // ====== ====== ====== EXAMPLE SECTION ====== ====== ======

                        // // ====== image-processing example ======
                        // JobDataParser dataParser = new JobDataParserImpl(); // JobHelper.getDataParser(parentContext, AckServerListener.clientId, jobBytes);

                        // // ====== word-count example ======
                        JobDataParser dataParser = new WordDataParserImpl();

                        // // ====== internet-share example ======
                        // JobDataParser dataParser = new NetDataParserImpl();

                        // // ====== empty-job example ======
                        // JobDataParser dataParser = new EmptyDataParserImpl();

                        // ====== ====== ====== ====== ====== ======

                        // get the whole object sent from client
                        Object dataObject = null;
                        try {
                            dataObject = dataParser.parseBytesToObject(AckServerListener.request.dataBytes);
                        } catch (Exception e) {
                            // this case shouldn't be happened
                            e.printStackTrace();
                        }

                        // before dividing job into parts, a placeholder to hold cumulative results
                        // must be created and stored into the map
                        Object emptyPlaceholder = dataParser.createPlaceHolder(dataObject);
                        JobMergeInfo jobMergeInfo = new JobMergeInfo(AckServerListener.request.clientId, emptyPlaceholder, dataParser);

                        // the total number of parts is equal to the total number of accepted workers
                        jobMergeInfo.totalPartNum = AckServerListener.advancedWorkerList.size();

                        Broker.jobMergeList.put(AckServerListener.request.clientId, jobMergeInfo);

                        // send job to worker
                        JobPackage taskPkg;
                        float currCummDRL = 0, newCummDRL = 0;
                        int currCummDRLNum = 0, newCummDRLNum = 0;
                        byte[] dataPart;
                        String useClientId;

                        for (String workerId : AckServerListener.advancedWorkerList.keySet()) {
                            // create parts with size proportional to the DRL value of each worker
                            newCummDRL = currCummDRL + AckServerListener.advancedWorkerList.get(workerId).floatValue();

                            // convert to the actual number of percentage
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

        /**
         * this function deserialize the request into Job Package object
         * and send ACKs to the nearby workers.
         *
         * @param clientId
         * @param request
         */
        public void queryDRL(String clientId, byte[] request) {
            AckServerListener.clientId = clientId;
            try {
                AckServerListener.request = (JobPackage) Utils.deserialize(request);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // start the RL request clock
            startRLRequestTime = System.currentTimeMillis();

            // query resource information from remote workers
            this.sendAck();
        }

        @Override
        public void receiveResponse(byte[] resp) {
            String respStr = new String(resp);

            // remote device's resource info received
            System.out.println("[broker] received " + respStr);

            // analyze response and add it to the array of WorkerInfos
            // to do here
            float drl = (float) Utils.getResponse(respStr, "drl");
            String workerId = (String) Utils.getResponse(respStr, "id");

            // compare DRL with client's DRL and add to the list
            // will not add more than MAX_WORKERS_PER_JOB devices (default is 3)
            // if (drl > 0 && advancedWorkerList.size() < Utils.MAX_WORKERS_PER_JOB) {
            if (drl >= AckServerListener.request.DRL && advancedWorkerList.size() < Utils.MAX_WORKERS_PER_JOB) {
                totalDRL = (advancedWorkerList.size() == 0) ? drl : totalDRL + drl;
                advancedWorkerList.put(workerId, drl);
            }
        }
    }

    /**
     * this function merge
     *
     * @param useClientId
     * @param taskBytes
     */
    private JobMergeInfo mergeTaskResults(String useClientId, byte[] taskBytes) {
        String[] idParts = useClientId.split(Utils.ID_DELIMITER);
        String clientId = idParts[0];
        int firstPercent = Integer.parseInt(idParts[1]);
        int lastPercent = Integer.parseInt(idParts[2]);

        JobMergeInfo jobInfo = Broker.jobMergeList.get(clientId);

        // add part to the client job's placeholder
        jobInfo.addPart(taskBytes, firstPercent, lastPercent);

        if (!jobInfo.isPlaceholderFilled()) {
            // if the placeholder of this job has not been fully filled,
            // it will still be open for adding more parts
            Broker.jobMergeList.put(clientId, jobInfo);
        }

        return jobInfo;
    }

    /**
     * this class contains information about status of a worker
     * at the moment worker is requested for DRL
     */
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

    /**
     * this class contains information of job combination
     */
    static class JobMergeInfo {
        public String clientId;
        public int cummPartNum;
        public int totalPartNum;
        public Object placeholder;
        private JobDataParser dataParser;

        public JobMergeInfo(String clientId, Object emptyPlaceholder, JobDataParser dataParser) {
            this.clientId = clientId;
            this.cummPartNum = 0;
            this.totalPartNum = 0;
            this.placeholder = emptyPlaceholder;
            this.dataParser = dataParser;
        }

        /**
         * copy the data part to the placeholder
         *
         * @param partBytes
         * @param firstPercent
         * @param lastPercent
         */
        public void addPart(byte[] partBytes, int firstPercent, int lastPercent) {
            // copy the data part to the placeholder, the position of the part
            // is defined by the firstPercent and lastPercent
            dataParser.copyPartToHolder(this.placeholder, partBytes, firstPercent, lastPercent);

            // and increase the number of parts that have been received.
            this.cummPartNum++;
        }

        /**
         * check if the placeholder is fully filled by all the parts
         *
         * @return
         */
        public boolean isPlaceholderFilled() {
            // by checking if the cumulative part number is equal to the total part number
            return this.cummPartNum == this.totalPartNum;
        }

        public byte[] getPlaceholder() {
            try {
                return dataParser.parseObjectToBytes(this.placeholder);
            } catch (Exception e) {
                e.printStackTrace();
                return new byte[0];
            }
        }
    }
}
