package com.minhld.pubsublibex;

import android.content.Context;

import com.minhld.jobex.JobDataParser;
import com.minhld.jobex.JobPackage;
import com.minhld.jobimpls.NetDataParserImpl;
import com.minhld.pbsbjob.AckServer;
import com.minhld.pubsublib.ZHelper;
import com.minhld.utils.Utils;

import org.zeromq.ZMQ;

import java.util.HashMap;

/**
 * this Client is used to send jobs to server (broker)
 * Created by minhld on 8/18/2016.
 */

public abstract class Client2 extends Thread {
    private Context parentContext;
    private String groupIp = "*";
    private int port = Utils.BROKER_XSUB_PORT;

    private ZMQ.Socket requester;

    private AckServerListener2 ackServer;
    public String clientId;

    public Client2() {
        this.start();
    }

    public Client2(Context _parentContext, String _groupIp) {
        this.parentContext = _parentContext;
        this.groupIp = _groupIp;
        this.start();
    }

    public Client2(String _groupIp, int _port) {
        this.groupIp = _groupIp;
        this.port = _port;
        this.start();
    }

    public void run() {
        try {
            // create context and connect client to the broker/worker
            // with a pre-defined Id
            ZMQ.Context context = ZMQ.context(1);
            requester = context.socket(ZMQ.REQ);
            ZHelper.setId(requester);
            this.clientId = new String(this.requester.getIdentity());
            String clientPort = "tcp://" + this.groupIp + ":" + this.port;
            requester.connect(clientPort);

            // initiate status request server
            ackServer = new AckServerListener2(parentContext, context, this.clientId);

            // client has been started, throwing an event to the holder
            clientStarted(this.clientId);

            // send a request to the broker/worker
            send();

            // get the response from broker/worker
            while (!Thread.currentThread().isInterrupted()) {
                byte[] response = requester.recv();
                resolveResult(response);
            }

            requester.close();
            context.term();
        } catch (Exception e) {
            // exception there - leave it for now
            e.printStackTrace();
        }
    }

    protected void sendMessage(byte[] msg) {
        requester.send(msg);
    }

    protected void sendMessage(String msg) {
        requester.send(msg);
    }

    /**
     * this event occurs when client finished starting
     * @param clientId
     */
    public abstract void clientStarted(String clientId);

    /**
     * this function defines what task to send to the broker/worker
     */
    public abstract void send();

    /**
     * this function is invoked when client receives result of the
     * task it requested. this function must be overrode to define
     * how to manipulate with the output.
     *
     * @param result
     */
    public abstract void resolveResult(byte[] result);

    static class AckServerListener2 extends AckServer {
        static String clientId;
        static JobPackage request;
        static float totalDRL = 0;
        static HashMap<String, String> advancedWorkerList;

        public AckServerListener2(final Context parentContext, ZMQ.Context context, String brokerIp) {
            super(context, brokerIp, Utils.LIST_PORT, Utils.RESP_PORT, new AckListener() {
                @Override
                public void allAcksReceived() {
                    try {
                        //

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // remove all existences from current work - to prepare serving form the new work
                        Broker2.AckServerListener.advancedWorkerList.clear();
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
            // query resource information from remote workers
            this.sendAck();
        }

        @Override
        public void receiveResponse(byte[] resp) {
            String respStr = new String(resp);
            String[] resps = respStr.split(",");

            // remote device's resource info received
            String brokerId = resps[0];
            String brokerStatus = resps[1];
            System.out.println("[client] received status from " + brokerId + ": " + brokerStatus);
            advancedWorkerList.put(brokerId, brokerStatus);
        }
    }
}
