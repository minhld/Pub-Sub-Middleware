package com.minhld.pubsublib;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 * Created by minhld on 8/4/2016.
 */

public class MidSubscriber extends Thread {
    private ZMQ.Context context;
    private ZMQ.Socket subscriber;

    private String groupIp;
    private String[] topics;
    private MessageListener mListener;

    public MidSubscriber(MessageListener _listener) {
        this.groupIp = "*";
        this.topics = new String[] { "" };
        this.mListener = _listener;
        this.start();
    }

    public MidSubscriber(String _groupIp, MessageListener _listener) {
        this.groupIp = _groupIp;
        this.topics = new String[] { "" };
        this.mListener = _listener;
        this.start();
    }

    public MidSubscriber(String _groupIp, String[] _topics, MessageListener _listener) {
        this.groupIp = _groupIp;
        this.topics = _topics;
        this.mListener = _listener;
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
