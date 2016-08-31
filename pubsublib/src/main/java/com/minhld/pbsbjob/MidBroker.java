package com.minhld.pbsbjob;

import com.minhld.pubsublib.Broker;

import org.zeromq.ZMQ;

/**
 * The middleware broker receives execution task request from client,
 * inquiry resource information from workers and return to client for
 * decision making, and dispatch tasks from client to the workers.
 *
 * Created by minhld on 8/24/2016.
 */

public class MidBroker extends Broker {

    public MidBroker(String _myIp) {
        super(_myIp);
    }

    class BrokerAckHandler extends AckServer {

        public BrokerAckHandler(ZMQ.Context _parent, String _ip) {
            super(_parent, _ip, new AckListener() {
                @Override
                public void allAcksReceived() {

                }
            });
        }

        @Override
        public void receiveResponse(byte[] resp) {

        }
    }
}
