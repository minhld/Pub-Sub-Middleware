package com.minhld.pubsublib;

import android.content.Context;

import com.minhld.jobex.JobDataParser;
import com.minhld.pbsbjob.AckClient;
import com.minhld.utils.Utils;

import org.zeromq.ZMQ;

import java.lang.reflect.Method;

/**
 * The worker serves as a servant for the broker. It receives tasks
 * from the broker and alternatively executes those tasks. The worker
 * only work with Broker running in Router mode
 *
 * Created by minhld on 8/18/2016.
 */
public abstract class Worker extends Thread {
    private String groupIp = "*";
    private int port = Utils.BROKER_XPUB_PORT;

    private Context context;
    private ExAckClient ackClient;

    public Worker(Context context) {
        this.context = context;
        this.start();
    }

    public Worker(Context context, String groupIp) {
        this.context = context;
        this.groupIp = groupIp;
        this.start();
    }

    public Worker(Context context, String groupIp, int port) {
        this.context = context;
        this.groupIp = groupIp;
        this.port = port;
        this.start();
    }

    public void run() {
        initWithBroker();
    }

    /**
     * initiate worker with broker at the middle
     */
    private void initWithBroker() {
        try {
            ZMQ.Context context = ZMQ.context(1);

            //  Socket to talk to clients and set its Id
            ZMQ.Socket worker = context.socket(ZMQ.REQ);
            ZHelper.setId (worker);
            worker.connect("tcp://" + this.groupIp + ":" + this.port);

            // inform broker that i am ready
            worker.send(Utils.WORKER_READY);

            // initiate ACK client - to listen to DRL request from brokers
            ackClient = new ExAckClient(context, this.groupIp, worker.getIdentity());

            // this part is to wait for broker to send job to execute
            String clientAddr;
            byte[] request, result, empty;
            while (!Thread.currentThread().isInterrupted()) {
                try {
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
                } catch (Exception d) {
                    d.printStackTrace();
                }
            }
            worker.close();
            context.term();
        } catch (Exception e) {
            // exception there - leave it for now
            e.printStackTrace();
        }
    }

    /**
     * default
     *
     * @param jobRequest
     * @return
     */
    protected byte[] defaultResolveRequest(byte[] jobRequest) {
        try {
//            Class dataParserClass = Utils.getObject(this.context, jobRequest);
//            Object dataParser = dataParserClass.newInstance();
//            // get the original data
//            Method getBytesToObject = dataParserClass.getMethod("getBytesToObject", JobDataParser.class);
//            getBytesToObject.invoke(dataParser, jobRequest);
//
//            // initiate the Job algorithm class & execute it
//            // suppose that job was download to Download folder in local device
//            String jobPath = Utils.getDownloadPath() + "/" + Utils.JOB_FILE_NAME;

            // Utils.runRemote(this.context, jobPath, orgObj, dataParser.getDataClass());
            return new byte[0];
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

//    /**
//     * initiate worker without broker at the middle.
//     * worker also takes the role of broker
//     */
//    private void initWithoutBroker() {
//
//    }

    class ExAckClient extends AckClient {
        public ExAckClient(ZMQ.Context _context, String _ip, byte[] _id) {
            super(_context, _ip, _id);
        }

        @Override
        public void sendResponse(String topic, byte[] request) {
            // this delegate function is called when client detects a DRL request
            // from server and try responding to it with DRL info

            // this is the place to send back device info
            String reqStr = new String(request);
            if (reqStr.equals("ack_request")) {
                // receive device resource information, and calculate DRL value here
                String drl = Utils.genDRL(new String(this.clientId), 1.5f, 0.6f, 1f, 0.25f, 2.5f, 0.7f);
                this.sendMessage(drl.getBytes());
            }
        }
    }

    /**
     * this abstract function needs to be filled. this is to
     * define how worker will complete the work
     *
     * @param request
     * @return
     */
    public abstract byte[] resolveRequest(byte[] request);
}
