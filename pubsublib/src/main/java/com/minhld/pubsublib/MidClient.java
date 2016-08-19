package com.minhld.pubsublib;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 *
 * Created by minhld on 8/18/2016.
 */

public class MidClient extends Thread {
    private ZMQ.Socket requester;
    private String groupIp = "*";
    private int port = Utils.BROKER_XSUB_PORT;

    public void run() {
        try {
            ZMQ.Context context = ZMQ.context(1);
            requester = context.socket(ZMQ.REQ);
            ZHelper.setId(requester);
            String clientPort = "tcp://" + groupIp + ":" + Utils.BROKER_XSUB_PORT;
            requester.connect(clientPort);

            requester.send("HELLO");
            byte[] reply = requester.recv();
            System.out.println("Client: " + new String(reply));

            requester.close();
            context.term();
        } catch (Exception e) {
            // exception there - leave it for now
            e.printStackTrace();
        }
    }
}
