package com.minhld.pubsublib;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 * Created by minhld on 8/4/2016.
 * This class work as a broker among the devices in mobile network.
 * The broker has several modes. To switch mode, please use the mode
 * type switcher:
 *  - Publish-Subscribe mode
 *  - Router mode
 *
 */
public class MidBroker extends Thread {
    private String brokerIp;

    // default type of the broker is publish-subscribe mode
    private Utils.PubSubType pubSubType = Utils.PubSubType.PubSub;

    public MidBroker() {
        this.brokerIp = "*";
        this.start();
    }

    public MidBroker(String _brokerIp) {
        this.brokerIp = _brokerIp;
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

    }

}
