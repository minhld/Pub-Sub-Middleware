package com.minhld.pubsublib;

import com.minhld.utils.Utils;

import org.zeromq.ZMQ;

/**
 * this Client is used to send jobs to server (broker)
 * Created by minhld on 8/18/2016.
 */

public abstract class Client extends Thread {

    private String groupIp = "*";
    private int port = Utils.BROKER_XSUB_PORT;

    private ZMQ.Socket requester;
    public String clientId;

    public Client() {
        this.start();
    }

    public Client(String _groupIp) {
        this.groupIp = _groupIp;
        this.start();
    }

    public Client(String _groupIp, int _port) {
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

            // client has been started, throwing an event to the holder
            clientStarted(this.clientId);

            // send a request to the broker/worker
            send();

            // get the response from broker/worker
            while (!Thread.interrupted()) {
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
}
