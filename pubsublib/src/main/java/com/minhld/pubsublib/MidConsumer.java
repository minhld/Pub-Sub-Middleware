package com.minhld.pubsublib;

import android.os.Handler;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

import java.util.Date;

/**
 * Created by minhld on 8/4/2016.
 */

public class MidConsumer extends Thread {
    private final int PUB_INTERVAL = 1500;
    private ZMQ.Context context;
    private ZMQ.Socket socket;

    private String groupIp;
    private Handler uiHandler;

    public MidConsumer(String _groupIp, Handler _uiHandler) {
        this.groupIp = _groupIp;
        this.uiHandler = _uiHandler;
    }

    public void run() {
        try {
            context = ZMQ.context(1);
            socket = context.socket(ZMQ.SUB);
            String bindGroupStr = "tcp://" + this.groupIp + ":" + Utils.BROKER_XPUB_PORT;
            socket.connect(bindGroupStr);

            // loop until the thread is disposed
            while (!Thread.currentThread().isInterrupted()) {
                if (socket.hasReceiveMore()) {
                    byte[] msg = socket.recv();
                    this.uiHandler.obtainMessage(Utils.MESSAGE_INFO, "client received: " + new String(msg)).sendToTarget();
                }
            }

            socket.close();
            context.term();
        } catch (Exception e) {
            // exception there - leave it for now

        }
    }
}
