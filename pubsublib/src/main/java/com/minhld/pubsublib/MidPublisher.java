package com.minhld.pubsublib;

import android.os.AsyncTask;
import android.os.Handler;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

import java.util.Date;

/**
 * Created by minhld on 8/4/2016.
 */
public abstract class MidPublisher extends Thread {
    private final int PUB_INTERVAL = 1500;
    private ZMQ.Context context;
    private ZMQ.Socket publisher;

    private String groupIp;
    private int sendInterval;

    public MidPublisher() {
        this.groupIp = "*";
        this.sendInterval = PUB_INTERVAL;
        this.start();
    }

    public MidPublisher(String _groupIp) {
        this.groupIp = _groupIp;
        this.sendInterval = PUB_INTERVAL;
        this.start();
    }

    public MidPublisher(String _groupIp, int interval) {
        this.groupIp = _groupIp;
        this.sendInterval = interval;
        this.start();
    }

    public void run() {
        try {
            context = ZMQ.context(1);
            publisher = context.socket(ZMQ.PUB);
            String bindGroupStr = "tcp://" + this.groupIp + ":" + Utils.BROKER_XPUB_PORT;
            publisher.connect(bindGroupStr);

            // loop until the thread is disposed
            while (!Thread.currentThread().isInterrupted()) {
                send();

                // and sleep
                try {
                    Thread.sleep(this.sendInterval);
                } catch (Exception e) {
                }
            }

            publisher.close();
            context.term();
        } catch (Exception e) {
            // exception there - leave it for now

        }
    }

    /**
     * fill in the abstract send function to implement what the publisher
     * wants to send to the broker
     */
    protected abstract void send();

    protected void sendTopic(String topic) {
        publisher.sendMore(topic);
    }

    protected void sendMessage(byte[] msg) {
        publisher.send(msg);
    }

    protected void sendMessage(String msg) {
        publisher.send(msg);
    }

    protected void sendFrame(String topic, byte[] msg) {
        sendTopic(topic);
        sendMessage(msg);
    }

    protected void sendFrame(String topic, String msg) {
        sendTopic(topic);
        sendMessage(msg);
    }
}
