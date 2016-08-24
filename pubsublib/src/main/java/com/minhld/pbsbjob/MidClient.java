package com.minhld.pbsbjob;

import com.minhld.pubsublib.Client;
import com.minhld.pubsublib.ZHelper;
import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 * This client
 * Created by minhld on 8/24/2016.
 */

public abstract class MidClient extends Thread {
    private String groupIp = "*";
    private int port = Utils.BROKER_XSUB_PORT;

    private ZMQ.Socket requester;

    public MidClient() {
        this.start();
    }

    public MidClient(String _groupIp) {
        this.groupIp = _groupIp;
        this.start();
    }

    public MidClient(String _groupIp, int _port) {
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

            // request for resource information
            requester.send(Utils.CLIENT_REQ_RES);

            // get the response for resource request and check the availability
            // if the amount is sufficient, dispatch the task
            byte[] resResponse = requester.recv();
            if (resReqResponse(resResponse)) {
                // send a request to the broker/worker
                send();

                // get the response from broker/worker
                byte[] response = requester.recv();
                resolveResult(response);

            } else {
                // if the amount of resource is not sufficient to execute
                // task, it will be performed locally
                // empty result will be return.
                resolveResult(new byte[0]);
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
     * this function defines what task to send to the broker/worker
     */
    public abstract void send();

    /**
     * this function is invoked when a response of resource info is
     * received. depends on the response, it should return TRUE if
     * resource amount is sufficient to start execution. otherwise
     * it will return FALSE.
     *
     * @param response
     * @return
     */
    public abstract boolean resReqResponse(byte[] response);

    /**
     * this function is invoked when client receives result of the
     * task it requested. this function must be overrode to define
     * how to manipulate with the output.
     *
     * @param result
     */
    public abstract void resolveResult(byte[] result);
}
