package com.minhld.pubsublib;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 * Created by minhld on 8/4/2016.
 */

public class MidSubscriber extends Thread {
    private ZMQ.Context context;
    private ZMQ.Socket subscriber;

    private String groupIp = "*";
    private int port = Utils.BROKER_XSUB_PORT;
    private String[] topics = new String[] { "" };
    private MessageListener mListener;

    public void setMessageListener(MessageListener _listener) {
        this.mListener = _listener;
    }

    public MidSubscriber() {
        this.start();
    }

    public MidSubscriber(String _groupIp) {
        this.groupIp = _groupIp;
        this.start();
    }

    public MidSubscriber(String _groupIp, int _port) {
        this.groupIp = _groupIp;
        this.port = _port;
        this.start();
    }

    public MidSubscriber(String _groupIp, String[] _topics) {
        this.groupIp = _groupIp;
        this.topics = _topics;
        this.start();
    }

    public MidSubscriber(String _groupIp, int _port, String[] _topics) {
        this.groupIp = _groupIp;
        this.port = _port;
        this.topics = _topics;
        this.start();
    }

    public void run() {
        try {
            context = ZMQ.context(1);
            subscriber = context.socket(ZMQ.SUB);
            String bindGroupStr = "tcp://" + this.groupIp + ":" + Utils.BROKER_XSUB_PORT;
            subscriber.connect(bindGroupStr);

            // subscribe all the available topics
            for (int i = 0; i < this.topics.length; i++) {
                subscriber.subscribe(topics[i].getBytes());
            }

            // loop until the thread is disposed
            String topic;
            byte[] msg = null;
            while (!Thread.currentThread().isInterrupted()) {
                topic = subscriber.recvStr();
                msg = subscriber.recv();
                this.mListener.msgReceived(topic, msg);
            }

            subscriber.close();
            context.term();
        } catch (Exception e) {
            // exception there - leave it for now

        }
    }

    public interface MessageListener {
        public void msgReceived(String topic, byte[] msg);
    }
}
