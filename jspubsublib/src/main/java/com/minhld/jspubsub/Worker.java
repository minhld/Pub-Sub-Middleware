package com.minhld.jspubsub;

import android.app.Activity;
import android.content.Context;

import com.minhld.httpd.JSEngine;
import com.minhld.httpd.JavaScriptInterface;
import com.minhld.jobex.Job;
import com.minhld.jobex.JobDataParser;
import com.minhld.jobex.JobPackage;
import com.minhld.jobimpls.WordDataParserImpl;
import com.minhld.jobimpls.WordJobImpl;
import com.minhld.pbsbjob.AckClient;
import com.minhld.utils.Utils;

import org.zeromq.ZMQ;

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

    Context context;
    private ExAckClient ackClient;

    public String workerId = "";

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
            ZMQ.Context zmqCtx = ZMQ.context(1);

            //  Socket to talk to clients and set its Id
            ZMQ.Socket worker = zmqCtx.socket(ZMQ.REQ);
            ZHelper.setId (worker);
            this.workerId = new String(worker.getIdentity());
            worker.connect("tcp://" + this.groupIp + ":" + this.port);

            // to report worker has finished the initialization
            workerStarted(this.workerId);

            // inform broker that i am ready
            worker.send(Utils.WORKER_READY);

            ((Activity) this.context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // init local server here
                    // initiate pre-existing files the local web server
                    JSEngine.initJs((Activity) context);
                    JSEngine.startServer(context);
                }
            });

            // initiate ACK client - to listen to DRL request from brokers
            ackClient = new ExAckClient(zmqCtx, this.groupIp, worker.getIdentity());

            // this part is to wait for broker to send job to execute
            String clientAddr;
            byte[] request, result, empty;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // get client address
                    clientAddr = worker.recvStr();

                    // set start job clock
                    long startTime = System.currentTimeMillis();

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

                    // end the job execution clock
                    long durr = System.currentTimeMillis() - startTime;
                    TaskDone taskInfo = new TaskDone();
                    taskInfo.durration = durr;
                    workerFinished(workerId, taskInfo);

                } catch (Exception d) {
                    d.printStackTrace();
                }
            }
            worker.close();
            zmqCtx.term();
        } catch (Exception e) {
            // exception there - leave it for now
            e.printStackTrace();
        }
    }

    /**
     * default
     *
     * @param packageBytes
     * @return
     */
    protected byte[] defaultResolveRequest(byte[] packageBytes) {
        try {
            // get back the job package sent by broker
            JobPackage request = (JobPackage) Utils.deserialize(packageBytes);

            // report out that worker receives a request
            receivedTask(request.clientId, request.dataBytes.length);

            // ====== ====== ====== EXAMPLE SECTION ====== ====== ======

            // ====== image-processing example ======

            // ====== word-count example ======
            JobDataParser dataParser = new WordDataParserImpl();
//            Job job = new WordJobImpl();

            // ====== ====== ====== END EXAMPLE ====== ====== ======

            // execute the job
            Object dataObject = dataParser.parseBytesToObject(request.dataBytes);
            JSEngine.data = dataObject;
//            Object result = job.exec(dataObject);

            // download job file
            String containerPath = Utils.getDownloadPath();
            String jobPath = Utils.copyAssets2Storage((Activity) context, "job.js", containerPath);

            JSEngine.setJobDoneListener(new JavaScriptInterface.JobDoneListener() {
                @Override
                public void jobDone(Object resObj) {
                    try {
                        // release the original data
                        JSEngine.data = null;
                        System.gc();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            JSEngine.execJob();


            return null; // dataParser.parseObjectToBytes(result);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * initiate worker without broker at the middle.
     * worker also takes the role of broker
     */
    private void initWithoutBroker() { }

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
     * similar to the <i>clientStarted</i> event in client, this event will be happened
     * when worker completes the initiation.
     *
     * @param workerId
     */
    public abstract void workerStarted(String workerId);

    /**
     * this event occurs when worker has completed receiving a task from client
     *
     * @param clientId
     * @param dataSize
     */
    public abstract void receivedTask(String clientId, int dataSize);

    public abstract void workerFinished(String workerId, TaskDone taskDone);

    /**
     * this abstract function needs to be filled. this is to
     * define how worker will complete the work
     *
     * @param packageBytes
     * @return
     */
    public abstract byte[] resolveRequest(byte[] packageBytes);

    /**
     * this class contains information of the task of which has just been
     * executed by the worker
     */
    public class TaskDone {
        public long durration;

    }
}
