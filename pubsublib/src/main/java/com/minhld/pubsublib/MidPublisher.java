package com.minhld.pubsublib;

import android.os.AsyncTask;
import android.os.Handler;

import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 * Created by minhld on 8/4/2016.
 */
public class MidPublisher extends AsyncTask {
    private String groupIp;
    private Handler uiHandler;

    public MidPublisher(String _groupIp, Handler _uiHandler) {
        this.groupIp = _groupIp;
        this.uiHandler = _uiHandler;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REQ);
        String bindGroupStr = "tcp://" + this.groupIp + ":" + Utils.BROKER_PORT;
        socket.connect(bindGroupStr);

        socket.send("Hello from client".getBytes(), 0);
        byte[] result = socket.recv(0);
        this.uiHandler.obtainMessage(Utils.MESSAGE_READ_CLIENT, result).sendToTarget();

        socket.close();
        context.term();

        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {

    }
}
