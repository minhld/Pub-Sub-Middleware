package com.minhld.pubsublib;

import android.content.Context;

import com.minhld.jobex.Job;
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

            // handle worker activity on back-end
            if (items.pollin(0)) {
                // queue worker address for LRU routing
                workerId = backend.recvStr();
                workerList.put(workerId, new WorkerInfo());
                ackServer.updateWorkerNumbers(workerList.size());

                // second frame is a delimiter, empty
                empty = backend.recv();
                assert (empty.length == 0);

                // third frame is READY or else a client reply address
                clientId = backend.recvStr();

                // if client reply, send rest back to front-end
                if (!clientId.equals(Utils.WORKER_READY)) {
                    // check the delimiter again
                    empty = backend.recv();
                    assert (empty.length == 0);

                    // collect the main data frame
                    reply = backend.recv();

                    // flush them out to the front-end
                    frontend.sendMore(clientId);
                    frontend.sendMore(Utils.BROKER_DELIMITER);
                    frontend.send(reply);
                }
            }

            // handle client activity at front-end
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
                    // when all returning ACKs are received, this event will
                    // be raised to dispatch tasks (pieces) to the workers
                    byte[] jobBytes = AckServerListener.request.jobBytes;

                    // ONGOING ONGOING ONGOING ONGOING ONGOING ONGOING ONGOING
                    try {
                        Utils.getObject(parentContext, jobBytes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    byte[] dataBytes = AckServerListener.request.dataBytes;

                    // send job to worker
                    JobPackage taskPkg;
                    for (String workerId : AckServerListener.advancedWorkerList.keySet()) {
                        taskPkg = new JobPackage(0, data, jobBytes);

                        backend.sendMore(workerId);
                        backend.sendMore(Utils.BROKER_DELIMITER);
                        backend.sendMore(AckServerListener.clientId);
                        backend.sendMore(Utils.BROKER_DELIMITER);
                        backend.send(taskPkg.toByteArray());
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

    class WorkerInfo {
        public double cpu;
        public double cpuUsed;
        public double memory;
        public double memoryUsed;
        public double battery;
        public double batteryUsed;
    }
}
