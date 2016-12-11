package com.minhld.pubsublibex;

import com.minhld.utils.Utils;

import org.zeromq.ZMQ;

import java.util.HashMap;

/**
 * This sub broker is to listen to the status message from clients
 */
public class Broker2 extends Thread {
    private String brokerIp = "*";

    private HashMap<String, ClientInfo> clientList;
    private int listenPort;

    public Broker2(String brokerIp, int port) {
        this.brokerIp = brokerIp;
        this.listenPort = port;
        this.start();
    }

    public void run() {
        // this switch
        initRouterMode();
    }

    /**
     * this function is called when developer invoke router mode
     * which is for job distribution
     */
    private void initRouterMode() {
        ZMQ.Context context = ZMQ.context(1);

        // initiate publish socket
        String frontendPort = "tcp://" + this.brokerIp + ":" + this.listenPort;
        ZMQ.Socket frontend = context.socket(ZMQ.ROUTER);
        frontend.bind(frontendPort);

        // Queue of available workers
        clientList = new HashMap<String, ClientInfo>();

        String workerId, clientId;
        byte[] empty, request, reply;
        while (!Thread.currentThread().isInterrupted()) {
            clientId = frontend.recvStr();

            // check 2nd frame
            empty = frontend.recv();
            assert (empty.length == 0);

            // get 3rd frame
            request = frontend.recv();

            frontend.sendMore(clientId);
            frontend.sendMore(Utils.BROKER_DELIMITER);
            frontend.send("OK");
        }

        frontend.close();
        context.term();
    }

    /**
     * this class contains information about status of a worker
     * at the moment worker is requested for DRL
     */
    class ClientInfo {
        public String clientId;
        public float DRL;

        public ClientInfo(String clientId) {
            this.clientId = clientId;
            this.DRL = 0;
        }

        public ClientInfo(String clientId, float drl) {
            this.clientId = clientId;
            this.DRL = drl;
        }
    }

}
