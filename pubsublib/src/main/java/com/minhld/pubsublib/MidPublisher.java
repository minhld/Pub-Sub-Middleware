package com.minhld.pubsublib;

import android.os.AsyncTask;
import android.os.Handler;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 * Created by minhld on 8/4/2016.
 */
public class MidPublisher extends Thread {
    private ZMQ.Context context;
    private ZMQ.Socket socket;

    private String groupIp;
    private Handler uiHandler;

    public MidPublisher(String _groupIp, Handler _uiHandler) {
        this.groupIp = _groupIp;
        this.uiHandler = _uiHandler;
    }


    public void run() {
        context = ZMQ.context(1);
        socket = context.socket(ZMQ.REQ);
        String bindGroupStr = "tcp://" + this.groupIp + ":" + Utils.BROKER_PORT;
        socket.connect(bindGroupStr);

        while(!Thread.currentThread().isInterrupted()) {
            byte[] result = socket.recv(0);
        }
//        byte[] result = socket.recv(0);
//        this.uiHandler.obtainMessage(Utils.MESSAGE_READ_CLIENT, result).sendToTarget();
    }

    /**
     * send data to the broker
     * @param data
     */
    public void send(byte[] data) {
        socket.send(data, 0);
    }

    public void dispose() {
        socket.close();
        context.term();
    }
}
