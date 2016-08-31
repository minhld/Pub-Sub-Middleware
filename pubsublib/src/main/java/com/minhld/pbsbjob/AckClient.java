package com.minhld.pbsbjob;

import org.zeromq.ZMQ;

/**
 * This class <b>ACK Client</b> listens to the ACK requests from server
 * and returns the ACK responses
 *
 * Created by minhld on 8/30/2016.
 */
public abstract class AckClient extends Thread {
    private static final int LISTENER_PORT = 5555;
    private static final int RESPONDER_PORT = 5556;

    private ZMQ.Context context;
    private ZMQ.Socket responder;
    protected String brokerIp;
    protected String workerId;
    protected int listenerPort, responderPort;

    public AckClient(ZMQ.Context _parentContext, String _ip, byte[] _workerId) {
        this.context = _parentContext;
        this.brokerIp = _ip;
        this.workerId = new String(_workerId);
        this.listenerPort = LISTENER_PORT;
        this.responderPort = RESPONDER_PORT;
        this.start();
    }

    public void setPorts(int listPort, int respPort) {
        this.listenerPort = listPort;
        this.responderPort = respPort;
    }

    public void run() {
        try {
            ZMQ.Socket listener = this.context.socket(ZMQ.SUB);
            listener.setIdentity(("ack_" + this.workerId).getBytes());
            listener.connect("tcp://" + this.brokerIp + ":" + this.listenerPort);
            listener.subscribe("request".getBytes());

            responder = this.context.socket(ZMQ.REQ);
            responder.connect("tcp://" + this.brokerIp + ":" + this.responderPort);

            byte[] topic, req;
            while (!Thread.currentThread().isInterrupted()) {
                // receives ACK requests
                topic = listener.recv();
                req = listener.recv();

                // this is the place to send back the ACK responses
                sendResponse(topic, req);

                // waits until it receive a new request
                responder.recv();

            }

            listener.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this one helps sending a response to server
     * this must be called from <i>sendResponse</i> function to send
     * a message to server
     *
     * @param response
     */
    protected void sendMessage(byte[] response) {
        responder.send(response);
    }

    /**
     * this function is called when client receives a request for (whatever)
     * example resource info. in this function, call
     *
     * @param request
     */
    public abstract void sendResponse(byte[] topic, byte[] request);
}
