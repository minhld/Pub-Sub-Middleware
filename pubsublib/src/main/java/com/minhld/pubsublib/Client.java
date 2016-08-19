package com.minhld.pubsublib;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 * this Client is used in Router mode
 * Created by minhld on 8/18/2016.
 */

public abstract class Client extends Thread {

    private String groupIp = "*";
    private int port = Utils.BROKER_XSUB_PORT;

    private ZMQ.Socket requester;

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
            String clientPort = "tcp://" + this.groupIp + ":" + this.port;
            requester.connect(clientPort);

            // send a request to the broker/worker
            send();

            // get the response from broker/worker
            byte[] response = requester.recv();
            resolveResult(response);

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
     * this function defines an abstract interface to developers so that
     * they can define how and what to send to the broker/worker
     */
    public abstract void send();

    /**
     * this function defines resolve interface for developers to
     * manipulate with the output result
     *
     * @param result
     */
    public abstract void resolveResult(byte[] result);
}
