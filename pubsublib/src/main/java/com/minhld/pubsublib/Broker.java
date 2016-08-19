package com.minhld.pubsublib;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by minhld on 8/4/2016.
 * This class work as a broker among the devices in mobile network.
 * The broker has several modes. To switch mode, please use the mode
 * type switcher:
 *  - Publish-Subscribe mode
 *  - Router mode
 *
 */
public class Broker extends Thread {
    private String brokerIp = "*";

    // default type of the broker is publish-subscribe mode
    private Utils.PubSubType pubSubType = Utils.PubSubType.PubSub;

    public Broker(String _brokerIp) {
        this.brokerIp = _brokerIp;
        this.start();
    }

    public Broker(Utils.PubSubType _pubSubType) {
        this.pubSubType = _pubSubType;
        this.start();
    }

    public Broker(String _brokerIp, Utils.PubSubType _pubSubType) {
        this.brokerIp = _brokerIp;
        this.pubSubType = _pubSubType;
        this.start();
    }

    public void run() {
        // this switch
        switch (pubSubType) {
            case PubSub: {
                initPubSubMode();
                break;
            }

            case Router: {
                initRouterMode();
                break;
            }
        }

    }

    public void setPubSubMode(Utils.PubSubType psType) {
        this.pubSubType = psType;
    }

    /**
     * this function is only called when developer invoke pub-sub mode (default)
     * which is for data transmission only
     */
    private void initPubSubMode() {
        ZMQ.Context context = ZMQ.context(1);

        // initiate publish socket
        String xpubUri = "tcp://" + this.brokerIp + ":" + Utils.BROKER_XSUB_PORT;
        ZMQ.Socket xpubSk = context.socket(ZMQ.XPUB);
        xpubSk.bind(xpubUri);

        // initiate subscribe socket
        String xsubUri = "tcp://" + this.brokerIp + ":" + Utils.BROKER_XPUB_PORT;
        ZMQ.Socket xsubSk = context.socket(ZMQ.XSUB);
        xsubSk.bind(xsubUri);

        // bind the two sockets together - this will suspend here to listen
        ZMQ.proxy(xsubSk, xpubSk, null);

        xsubSk.close();
        xpubSk.close();
        context.term();
    }

    /**
     * this function is called when developer invoke router mode
     * which is for job distribution
     */
    private void initRouterMode() {
        ZMQ.Context context = ZMQ.context(1);

        // initiate publish socket
        String frontendPort = "tcp://" + this.brokerIp + ":" + Utils.BROKER_XSUB_PORT;
        ZMQ.Socket frontend = context.socket(ZMQ.ROUTER);
        frontend.bind(frontendPort);

        // initiate subscribe socket
        String backendPort = "tcp://" + this.brokerIp + ":" + Utils.BROKER_XPUB_PORT;
        ZMQ.Socket backend = context.socket(ZMQ.ROUTER);
        backend.bind(backendPort);

        // Queue of available workers
        Queue<String> workerQueue = new LinkedList<>();

        String workerAddr, clientAddr;
        byte[] empty, request, reply;
        while (!Thread.currentThread().isInterrupted()) {
            ZMQ.Poller items = new ZMQ.Poller(2);
            items.register(backend, ZMQ.Poller.POLLIN);
            items.register(frontend, ZMQ.Poller.POLLIN);

            if (items.poll() < 0)
                break;

            // handle worker activity on back-end
            if (items.pollin(0)) {
                // queue worker address for LRU routing
                workerAddr = backend.recvStr();
                workerQueue.add(workerAddr);

                // second frame is a delimiter, empty
                empty = backend.recv();
                assert (empty.length == 0);

                // third frame is READY or else a client reply address
                clientAddr = backend.recvStr();

                // if client reply, send rest back to front-end
                if (!clientAddr.equals(Utils.WORKER_READY)) {
                    // check the delimiter again
                    empty = backend.recv();
                    assert (empty.length == 0);

                    // collect the main data frame
                    reply = backend.recv();

                    // flush them out to the front-end
                    frontend.sendMore(clientAddr);
                    frontend.sendMore(Utils.BROKER_DELIMITER);
                    frontend.send(reply);
                }
            }

            // handle client activity at front-end
            if (items.pollin(1)) {
                // now get next client request, route to LRU worker
                // client request is [address][empty][request]
                clientAddr = frontend.recvStr();

                // check 2nd frame
                empty = frontend.recv();
                assert (empty.length == 0);

                // get 3rd frame
                request = frontend.recv();

                // get worker address from the queue
                workerAddr = workerQueue.poll();

                backend.sendMore(workerAddr);
                backend.sendMore(Utils.BROKER_DELIMITER);
                backend.sendMore(clientAddr);
                backend.sendMore(Utils.BROKER_DELIMITER);
                backend.send (request);
            }

        }

        frontend.close();
        backend.close();
        context.term();
    }

}
