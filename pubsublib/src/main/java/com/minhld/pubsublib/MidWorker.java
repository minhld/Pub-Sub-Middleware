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
public abstract class MidWorker extends Thread {
    private String groupIp = "*";

    public MidWorker() {
        this.start();
    }

    public MidWorker(String _groupIp) {
        this.groupIp = _groupIp;
        this.start();
    }

    public void run() {
        try {
            ZMQ.Context context = ZMQ.context(1);

            //  Socket to talk to clients
            ZMQ.Socket worker = context.socket(ZMQ.REQ);
            ZHelper.setId (worker);
            worker.connect("tcp://" + groupIp + ":" + Utils.BROKER_XPUB_PORT);

            // inform broker i am ready
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

    public abstract byte[] resolveRequest(byte[] request);
}
