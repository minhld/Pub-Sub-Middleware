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
        String brokerUri = "tcp://" + this.brokerIp + ":" + Utils.BROKER_PORT;
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        socket.bind(brokerUri);

        while(!Thread.currentThread().isInterrupted()) {
            byte[] msg = socket.recv(0);
            // spread out the message to all the members

            socket.send(msg, 0);
//            uiHandler.obtainMessage(Utils.MESSAGE_READ_SERVER, msg).sendToTarget();
        }
        socket.close();
        context.term();

        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {

    }


}
