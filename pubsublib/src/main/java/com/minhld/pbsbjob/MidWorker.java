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
     * this class refers to: AckClient
     *
     * @author minhld
     *
     */
    class WorkerAckHandler extends AckClient {
        public WorkerAckHandler(ZMQ.Context _parent, String _ip, byte[] _workerId) {
            super(_parent, _ip, _workerId);
        }

        @Override
        public void sendResponse(byte[] topic, byte[] request) {
            System.out.println(this.workerId + " received " + new String(request));

            sendMessage((this.workerId + " " + this.brokerIp + " report").getBytes());
        }
    }
}
