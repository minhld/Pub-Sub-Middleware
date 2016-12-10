package com.minhld.pbsbjob;

import org.zeromq.ZMQ;

/**
 * This class initiates ACK requests to clients and listen for their
 * ACK responses.
 *
 * Created by minhld on 8/30/2016.
 */

public abstract class AckServer extends Thread {
    private static final int LISTENER_PORT = 5555;
    private static final int RESPONDER_PORT = 5556;

    ZMQ.Context parentContext;
    /**
     * socket to talk to workers
     */
    private ZMQ.Socket requester;
    private AckListener ackListener;

    protected String brokerIp;
    protected int listenerPort, responderPort;
    protected int workerNumber;

    public AckServer(ZMQ.Context _parentContext, String _brokerIp, AckListener _ackListener) {
        this.parentContext = _parentContext != null ? _parentContext : ZMQ.context(1);
        this.brokerIp = _brokerIp;
        this.ackListener = _ackListener;
        this.listenerPort = LISTENER_PORT;
        this.responderPort = RESPONDER_PORT;
        this.start();
    }

    public AckServer(ZMQ.Context _parentContext, String _brokerIp, int _listPort, int _respPort, AckListener _ackListener) {
        this.parentContext = _parentContext != null ? _parentContext : ZMQ.context(1);
        this.brokerIp = _brokerIp;
        this.ackListener = _ackListener;
        this.listenerPort = _listPort;
        this.responderPort = _respPort;
        this.start();
    }

    /**
     * when server receive a request from a client, it will quickly
     * contact with its nearby workers to find out capability. this
     * function is to send ACKs to the workers to discover their
     * capability for the current task.
     */
    public void sendAck() {
        requester.sendMore("request");
        requester.send("ack_request");
    }

    /**
     * the number of workers must be updated so that it will be used
     * in the loop entrance to determine when a loop is finished and
     * we can send out an event message.
     *
     * @param workerNumber
     */
    public void updateWorkerNumbers(int workerNumber) {
        this.workerNumber = workerNumber;
    }

    public void run() {
        try {
            ZMQ.Context context = ZMQ.context(1);

            //  Socket to talk to workers
            requester = context.socket(ZMQ.PUB);
            requester.setIdentity("broker_ack".getBytes());
            requester.bind("tcp://" + this.brokerIp + ":" + this.listenerPort);

            ZMQ.Socket inquirer = context.socket(ZMQ.REP);
            inquirer.bind("tcp://" + this.brokerIp + ":" + this.responderPort);

            ZMQ.Poller poller = new ZMQ.Poller(1);
            poller.register(inquirer, ZMQ.Poller.POLLIN);

            byte[] resp;

            while (!isInterrupted()) {

                int totalPoll = 0;
                while (totalPoll < this.workerNumber && poller.poll(100) > 0) {
                    // received the response from client
                    resp = inquirer.recv();

                    // do something with the response
                    receiveResponse(resp);

                    // send a trigger signal back to the client so that it can
                    // restart the listening loop
                    inquirer.send("");

                    totalPoll++;

                    // if the total poll number reaches the number of workers,
                    // we will return a message for that event
                    if (totalPoll == this.workerNumber && this.ackListener != null) {
                        this.ackListener.allAcksReceived();
                    }
                }
            }

            requester.close();
            inquirer.close();
            context.term();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this function defines how to solve the responses from a worker.
     * normally, ACK server will summary information from all the responses
     * and evaluate it into a single value before sending it back to the
     * client device.
     *
     * @param resp
     */
    public abstract void receiveResponse(byte[] resp);

    /**
     * this class listens to the updates relevant to ACK messages
     */
    public interface AckListener {
        /**
         * this event occurs when all the ACKs from workers are received
         * broker will consider the DRL values and divide job to the tasks
         * that match with DRL values
         */
        public void allAcksReceived();
    }
}
