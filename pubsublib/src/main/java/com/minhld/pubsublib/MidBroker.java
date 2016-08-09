package com.minhld.pubsublib;

import android.os.AsyncTask;
import android.os.Handler;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 * Created by minhld on 8/4/2016.
 * This class is to simulate an ActiveMQ on Android
 */
public class MidBroker extends AsyncTask {
    private String brokerIp;
    private final Handler uiHandler;

    public MidBroker(String _brokerIp, Handler _uiHandler) {
        this.brokerIp = _brokerIp;
        this.uiHandler = _uiHandler;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        ZMQ.Context context = ZMQ.context(1);

        // initiate publish socket
        String xpubUri = "tcp://" + this.brokerIp + ":" + Utils.BROKER_XPUB_PORT;
        ZMQ.Socket xpubSk = context.socket(ZMQ.XPUB);
        xpubSk.bind(xpubUri);

        // initiate subscribe socket
        String xsubUri = "tcp://" + this.brokerIp + ":" + Utils.BROKER_XSUB_PORT;
        ZMQ.Socket xsubSk = context.socket(ZMQ.XSUB);
        xsubSk.bind(xsubUri);

        // bind the two sockets together - this will suspend here to listen
        this.uiHandler.obtainMessage(Utils.MESSAGE_INFO, "broker started...").sendToTarget();
        ZMQ.proxy(xsubSk, xpubSk, null);

        xsubSk.close();
        xpubSk.close();
        context.term();

        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        // temporarily unused
    }
}
