package com.minhld.pubsublibex;

import com.minhld.pubsublib.ZHelper;
import com.minhld.utils.Utils;

import org.zeromq.ZMQ;

/**
 * this Client is used to send jobs to server (broker)
 * Created by minhld on 8/18/2016.
 */

public abstract class Client2 extends Thread {

    private String groupIp = "*";
    private int port = Utils.BROKER_XSUB_PORT;

    private ZMQ.Socket requester;
    public String clientId;

    public Client2() {
        this.start();
    }

    public Client2(String _groupIp) {
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
            requester.setReceiveTimeOut(Utils.BROKER_TIMEOUT);

            // get the response from broker/worker
            while (!Thread.currentThread().isInterrupted()) {
                sendMessage("hello!");
                byte[] response = requester.recv();

                if (response != null) {
                    resolveResult(response);
                } else {
                    networkDisconnected();
                }

                // wait for WAIT_TIME seconds
                try {
                    Thread.sleep(Utils.CLIENT_WAIT_TIME);
                } catch (Exception e) {}
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
     * this function is invoked when client receives result of the
     * task it requested. this function must be overrode to define
     * how to manipulate with the output.
     *
     * @param result
     */
    public abstract void resolveResult(byte[] result);

    public abstract void networkDisconnected();
}
