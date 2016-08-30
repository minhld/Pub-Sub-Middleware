package com.minhld.pbsbjob;

import com.minhld.pubsublib.Worker;
import com.minhld.pubsublib.ZHelper;
import com.minhld.wfd.Utils;

import org.zeromq.ZMQ;

/**
 * Created by minhld on 8/24/2016.
 */

public class MidWorker extends Worker {

    public MidWorker() {
        super();
    }

    public MidWorker(String _groupIp) {
        super(_groupIp);
    }

    public MidWorker(String _groupIp, int _port) {
        super(_groupIp, _port);
    }

    /**
     * this abstract function needs to be filled. this is to
     * define how worker will complete the work
     *
     * @param request
     * @return
     */
    public byte[] resolveRequest(byte[] request) {
        String reqStr = new String(request);
        if (reqStr.equals(Utils.CLIENT_REQ_RES)) {
            // request for resource information

        } else {
            // execute task

        }
        return null;
    }

    /**
     * info:
     * @author minhld
     *
     */
    class WorkerAckHandler extends Thread {
        ZMQ.Context parentContext;
        String brokerIp;
        byte[] workerId;

        public WorkerAckHandler(ZMQ.Context _parentContext, String _ip, byte[] _workerId) {
            this.parentContext = _parentContext;
            this.brokerIp = _ip;
            this.workerId = _workerId;
            this.start();
        }

        public void run() {
            try {
                ZMQ.Context context = ZMQ.context(1);

                //  Socket to talk to server
                System.out.println("Connecting to server...");

                ZMQ.Socket listener = context.socket(ZMQ.SUB);
                listener.setIdentity(("ack_" + workerId).getBytes());
                listener.connect("tcp://" + this.brokerIp + ":5555");
                listener.subscribe("request".getBytes());

                ZMQ.Socket responder = context.socket(ZMQ.REQ);
                responder.connect("tcp://" + this.brokerIp + ":5556");

                while (!Thread.currentThread().isInterrupted()) {
                    byte[] topic = listener.recv();
                    byte[] resp1 = listener.recv();
                    System.out.println(this.brokerIp + " received " + new String(resp1));
                    responder.send(": client " + this.brokerIp + " report");

                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String svrResp = responder.recvStr();
                    System.out.println(": client " + this.brokerIp + " refresh because " + svrResp);
                }

                listener.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
