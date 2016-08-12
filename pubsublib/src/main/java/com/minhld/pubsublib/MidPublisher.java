package com.minhld.pubsublib;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 * Publisher - push data to the brokers or subscribers
 * Supports two mode
 *
 * Created by minhld on 8/4/2016.
 */
public abstract class MidPublisher extends Thread {
    private final int PUB_INTERVAL = 1500;
    private ZMQ.Context context;
    private ZMQ.Socket publisher;

    private String groupIp = "*";
    private int port = Utils.BROKER_XPUB_PORT;
    private int sendInterval = PUB_INTERVAL;
    private boolean neededBroker = false;

    public void setSendInterval(int _interval) {
        this.sendInterval = _interval;
    }

    public void setAlsoGroupOwner(boolean _neededBroker) {
        this.neededBroker = _neededBroker;
    }

    public MidPublisher() {
        this.start();
    }

    public MidPublisher(String _groupIp) {
        this.groupIp = _groupIp;
        this.start();
    }

    public MidPublisher(String _groupIp, int _port) {
        this.groupIp = _groupIp;
        this.port = _port;
        this.start();
    }

    public MidPublisher(String _groupIp, boolean _neededBroker) {
        this.groupIp = _groupIp;
        this.neededBroker = _neededBroker;
        this.start();
    }

    public MidPublisher(String _groupIp, int _port, boolean _neededBroker) {
        this.groupIp = _groupIp;
        this.port = _port;
        this.neededBroker = _neededBroker;
        this.start();
    }

    public void run() {
        try {
            context = ZMQ.context(1);
            publisher = context.socket(ZMQ.PUB);
            String bindGroupStr = "tcp://" + this.groupIp + ":" + Utils.BROKER_XPUB_PORT;
            if (this.neededBroker) {
                // this will connect to a broker
                publisher.connect(bindGroupStr);
            } else {
                // this will set it as a self-control publisher
                publisher.bind(bindGroupStr);
            }

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
