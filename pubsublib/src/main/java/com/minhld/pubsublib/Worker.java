package com.minhld.pubsublib;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 * The worker serves as a servant for the broker. It receives tasks
 * from the broker and alternatively executes those tasks. The worker
 * only work with Broker running in Router mode
 *
 * Created by minhld on 8/18/2016.
 */
public abstract class Worker extends Thread {
    private String groupIp = "*";
    private int port = Utils.BROKER_XPUB_PORT;

    // default type of the broker is publish-subscribe mode
    private Utils.BrokerType brokerType = Utils.BrokerType.Broker;

    public Worker() {
        this.start();
    }

    public Worker(String _groupIp) {
        this.groupIp = _groupIp;
        this.start();
    }

    public Worker(String _groupIp, int _port) {
        this.groupIp = _groupIp;
        this.port = _port;
        this.start();
    }

    public void run() {
        switch (this.brokerType) {
            case Broker: {
                initWithBroker();
                break;
            }

            case Brokerless: {
                initWithoutBroker();
                break;
            }
        }
    }

    public void setBrokerType(Utils.BrokerType _brokerType) {
        this.brokerType = _brokerType;
    }

    /**
     * initiate worker with broker at the middle
     */
    private void initWithBroker() {
        try {
            ZMQ.Context context = ZMQ.context(1);

            //  Socket to talk to clients and set its Id
            ZMQ.Socket worker = context.socket(ZMQ.REQ);
            ZHelper.setId (worker);
            worker.connect("tcp://" + this.groupIp + ":" + this.port);

            // inform broker that i am ready
            worker.send(Utils.WORKER_READY);

            String clientAddr;
            byte[] request, result, empty;
            while (!Thread.currentThread().isInterrupted()) {
                // get client address
                clientAddr = worker.recvStr();

                // delimiter
                empty = worker.recv();
                assert (empty.length == 0);

                // get request, send reply
                request = worker.recv();
                result = resolveRequest(request);

                // return result back to front-end
                worker.sendMore(clientAddr);
                worker.sendMore(Utils.BROKER_DELIMITER);
                worker.send(result);
            }
            worker.close();
            context.term();
        } catch (Exception e) {
            // exception there - leave it for now
            e.printStackTrace();
        }
    }

    /**
     * initiate worker without broker at the middle.
     * worker also takes the role of broker
     */
    private void initWithoutBroker() {

    }

    /**
     * this abstract function needs to be filled. this is to
     * define how worker will complete the work
     *
     * @param request
     * @return
     */
    public abstract byte[] resolveRequest(byte[] request);
}
