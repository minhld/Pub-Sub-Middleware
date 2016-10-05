package com.minhld.pbsbjob;

import com.minhld.pubsublib.Worker;
import com.minhld.utils.Utils;

import org.zeromq.ZMQ;

/**
 * The worker receives job from broker and execute it. The worker
 * also reports by sending responses back to the broker to describe
 * its resource status (including RAM, CPU, battery and network
 * condition).
 *
 * Created by minhld on 8/24/2016.
 */

public class MidWorker extends Worker {

    public MidWorker(String _groupIp) {
        super(_groupIp);
    }

    /**
     * this abstract function needs to be filled. this is to
     * define how worker will complete the work
     *
     * @param request
     * @return
     */
    @Override
    public byte[] resolveRequest(byte[] request) {
        // this is the place for worker to resolve request
        // from client pass to it by broker.

        // if developer is unsure how to do, simply use the inner predefined function
        return resolveRequestInner(request);
    }

}
