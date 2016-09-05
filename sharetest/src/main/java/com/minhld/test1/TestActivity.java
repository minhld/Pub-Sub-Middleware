package com.minhld.test1;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.minhld.pbsbmid.MidSupporter;
import com.minhld.pbsbmid.R;
import com.minhld.pbsbmid.UITools;
import com.minhld.pubsublib.Publisher;
import com.minhld.pubsublib.Subscriber;
import com.minhld.wfd.Utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.TimeTCPClient;
import org.apache.commons.net.TimeUDPClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TestActivity extends AppCompatActivity {
    @BindView(R.id.createGroupBtn)
    Button createGroupBtn;

    @BindView(R.id.discoverBtn)
    Button discoverBtn;

    @BindView(R.id.pubBtn)
    Button pubBtn;

    @BindView(R.id.subBtn)
    Button subBtn;

    @BindView(R.id.packSizeEdit)
    EditText packSizeEdit;

    @BindView(R.id.deviceList)
    ListView deviceList;

    @BindView(R.id.infoText)
    TextView infoText;

    Handler mainUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Utils.MESSAGE_INFO: {
                    String strMsg = (String) msg.obj;
                    UITools.writeLog(TestActivity.this, infoText, strMsg);
                    break;
                }
            }
        }
    };

    MidSupporter midSupporter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        ButterKnife.bind(this);
        infoText.setMovementMethod(new ScrollingMovementMethod());

        midSupporter = new MidSupporter(this, mainUiHandler);

        // bind device list
        deviceList.setAdapter(midSupporter.getDeviceListAdapter());

        // --- BUTTON EVENT HANDLERS ---

        createGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                midSupporter.createGroup();
            }
        });

        discoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                midSupporter.discoverPeers();
            }
        });

        pubBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ExPublisher(UITools.GO_IP);
            }
        });

        subBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Subscriber subscriber = new Subscriber(UITools.GO_IP, Utils.BROKER_XPUB_PORT, new String[] { "ADFB" });
                subscriber.setMessageListener(new Subscriber.MessageListener() {
                    @Override
                    public void msgReceived(String topic, final byte[] msg) {
//                        //
//                        long time = getNTPTime();

                        // get package size and label
                        String sizeStr = packSizeEdit.getText().toString();
                        packageSize = Integer.parseInt(sizeStr);
                        fileLabel = packageSize + "k_package";

                        new GetTimeServer(new TimeListener() {
                            @Override
                            public void timeReceived(long time) {
                                long startProcessTime = Long.parseLong(new String(msg).trim());
                                Utils.appendTestInfo(fileLabel, "" + startProcessTime, time);

                                long diffTime = time - startProcessTime;
                                UITools.writeLog(TestActivity.this, infoText, "diff: " + diffTime);
                            }
                        });

//                        long startTime = Long.parseLong(new String(msg).trim());
//                        Utils.appendTestInfo(fileLabel, "" + startTime, time);

                        // print out
//                        long durr = time - startTime;
//                        UITools.writeLog(TestActivity.this, infoText, topic + ": total " + durr);
                    }
                });

            }
        });

//        // connect to NTP server
//        new GetTimeServer();

        //
        Utils.grandWritePermission(this);
    }

    // TEST:
//    long deltaTimeClientServer = 0;



//    /**
//     * this class is to get the NTP time to synchronize
//     */
//    class GetTimeServer extends Thread {
//        public GetTimeServer() {
//            this.start();
//        }
//
//        public void run() {
//            try {
//                TimeTCPClient client = new TimeTCPClient();
//                client.setDefaultTimeout(10000);
////                client.connect("time-nw.nist.gov");
//                client.connect("time.nist.gov");
////                client.connect("time-a.timefreq.bldrdoc.gov");
//                client.setTcpNoDelay(true);
//                long serverTime = client.getDate().getTime();
//                deltaTimeClientServer = new Date().getTime() - serverTime;
//                client.disconnect();
////                TimeUDPClient client = new TimeUDPClient();
////                client.setDefaultTimeout(5000);
////
////                InetAddress hostAddr = InetAddress.getByName("time-nw.nist.gov");
////                long currentTime = client.getTime(hostAddr);
////                client.close();
////
////            } catch (UnknownHostException uhEx) {
////                uhEx.printStackTrace();
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

    // TEST:
    int packageSize = 10;
    int count = 0;
    String fileLabel = ""; // packageSize + "k_package";

//    private long getNTPTime() {
//        return new Date().getTime() - deltaTimeClientServer;
//    }

    class ExPublisher extends Publisher {

        @Override
        protected void prepare() {

        }

        public ExPublisher(String _groupIp) {
            super(_groupIp);
            setSendInterval(2000);
        }

        @Override
        public void send() {
//            String time = "" + new Date().getTime();
//            long lTime = getNTPTime();
//            String time = "" + lTime;

            // get package size and label
            String sizeStr = packSizeEdit.getText().toString();
            packageSize = Integer.parseInt(sizeStr);
            fileLabel = packageSize + "k_package";

            // prepare data
            new GetTimeServer(new TimeListener() {
                @Override
                public void timeReceived(long time) {
                    StringBuffer data = new StringBuffer(Long.toString(time));
                    data.setLength(packageSize * 1024);

                    // start noting
                    Utils.appendTestInfo(fileLabel, "" + time, time);

                    // and send
                    sendFrame("ADFB", data.toString().getBytes());

                    UITools.writeLog(TestActivity.this, infoText, ++count + " " + data.toString().trim());
                }
            });


        };
    }

    // TEST:
    class GetTimeServer extends Thread {
        TimeListener timeListener;

        public GetTimeServer(TimeListener _timeListener) {
            this.timeListener = _timeListener;
            this.start();
        }

        public void run() {
            try {
                long startTime = System.currentTimeMillis();
                String timeServer = "http://129.123.7.172:3883/sm/getTime";
                HttpURLConnection conn = (HttpURLConnection) new URL(timeServer).openConnection();
                int code = conn.getResponseCode();
                if (code == 200) {
                    String lTime = IOUtils.toString(conn.getInputStream());
                    long time = Long.parseLong(lTime);

                    if (timeListener != null) {
                        timeListener.timeReceived(time);
                    }

//                    long durr = System.currentTimeMillis() - startTime;
//                    deltaTimeClientServer = new Date().getTime() - Long.parseLong(lTime);
//                    UITools.writeLog(TestActivity.this, infoText, "reached server. delta = " + deltaTimeClientServer);

                } else {
                    UITools.writeLog(TestActivity.this, infoText, "error: server unreached");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    interface TimeListener {
        public void timeReceived(long time);
    }

    @Override
    protected void onPause() {
        super.onPause();
        midSupporter.actOnPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        midSupporter.actOnResume();
    }

}