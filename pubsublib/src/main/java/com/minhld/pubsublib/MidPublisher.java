package com.minhld.pubsublib;

import android.os.AsyncTask;
import android.os.Handler;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

import java.util.Date;

/**
 * Created by minhld on 8/4/2016.
 */
public class MidPublisher extends Thread {
    private final int PUB_INTERVAL = 1500;
    private ZMQ.Context context;
    private ZMQ.Socket socket;

    private String groupIp;
    private Handler uiHandler;

    public MidPublisher(String _groupIp, Handler _uiHandler) {
        this.groupIp = _groupIp;
        this.uiHandler = _uiHandler;
    }

    public void run() {
        try {
            context = ZMQ.context(1);
            socket = context.socket(ZMQ.PUB);
            String bindGroupStr = "tcp://" + this.groupIp + ":" + Utils.BROKER_XSUB_PORT;
            socket.connect(bindGroupStr);

            // loop until the thread is disposed
            while (!Thread.currentThread().isInterrupted()) {
                String newDate = new Date().toString();
                socket.send(newDate.getBytes());

                // and sleep
                try {
                    Thread.sleep(PUB_INTERVAL);
                } catch (Exception e) {
                }
            }

            socket.close();
            context.term();
        } catch (Exception e) {
            // exception there - leave it for now

        }
    }

    /**
     * send data to the broker
     * @param data
     */
    public void send(byte[] data) {
        socket.send(data, 0);
    }

}
