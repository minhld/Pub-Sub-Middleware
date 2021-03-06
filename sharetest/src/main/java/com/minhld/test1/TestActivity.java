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
import com.minhld.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TestActivity extends AppCompatActivity {
    @BindView(R.id.toActiveMqBtn)
    Button createGroupBtn;

    @BindView(R.id.to0mqBtn)
    Button discoverBtn;

    @BindView(R.id.toRabbitMqBtn)
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
                        // get package size and label
                        final String[] msgParts = new String(msg).trim().split(" ");

//                        String sizeStr = packSizeEdit.getText().toString();
//                        packageSize = Integer.parseInt(sizeStr);
                        fileLabel = msgParts[0] + "k_package";

                        long startProcessTime = Long.parseLong(msgParts[1]);
                        long ntpTime = getNtpTime();
                        long diffTime = ntpTime - startProcessTime;
                        Utils.appendTestInfo(fileLabel, "" + startProcessTime, diffTime);
                        UITools.writeLog(TestActivity.this, infoText, "pksize: " + msg.length + "; size: " + msgParts[0] + "; time: " + diffTime);

//                        // use local NTP server
//                        new GetTimeServer(new TimeListener() {
//                            @Override
//                            public void timeReceived(long time) {
//                                long startProcessTime = Long.parseLong(new String(msg).trim());
//                                Utils.appendTestInfo(fileLabel, "" + startProcessTime, time);
//
//                                long diffTime = time - startProcessTime;
//                                UITools.writeLog(TestActivity.this, infoText, "diff: " + diffTime);
//                            }
//                        });

                    }
                });

            }
        });

        Utils.grandWritePermission(this);

        // calculate offset time
        NtpUtils.computeOffset();
    }

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
//                // client.connect("time-nw.nist.gov");
//                client.connect("time.nist.gov");
//
//                client.setTcpNoDelay(true);
//                long serverTime = client.getDate().getTime();
//                deltaTimeClientServer = new Date().getTime() - serverTime;
//                client.disconnect();
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

    private long getNtpTime() {
        return NtpUtils.genericOffsetTime + System.currentTimeMillis();
//        return NtpUtils.getTime();
//        return NtpUtils.getMinhTime();
    }

    // TEST:
//    int packageSize = 1;
    int runCount = 0;
    int pkgSizeCount = 0;   // warming up from zero
    int count = 0;
    String fileLabel = "";

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
            // get package size and label
//            String sizeStr = packSizeEdit.getText().toString();
//            packageSize = Integer.parseInt(sizeStr);

            final int packageSize = increaseNumber(pkgSizeCount);

//            runOnUiThread(new Runnable() {
//                public void run() {
//                    packSizeEdit.setText(packageSize);
//                }
//            });

            if (runCount < 30) {
                runCount++;
            } else {
                runCount = 0;
                pkgSizeCount++;
            }

            fileLabel = packageSize + "k_package";

            long ntpTime = getNtpTime();

            String message = packageSize + " " + Long.toString(ntpTime);

            StringBuffer data = new StringBuffer(message);
            data.setLength(packageSize * 1024);

            // start noting
            Utils.appendTestInfo(fileLabel, "" + ntpTime, ntpTime);

            // and send
            sendFrame("ADFB", data.toString().getBytes());

            UITools.writeLog(TestActivity.this, infoText, ++count + "; size: " + packageSize + " start: " + ntpTime);

//            // use local NTP server
//            // prepare data
//            new GetTimeServer(new TimeListener() {
//                @Override
//                public void timeReceived(long time) {
//                    StringBuffer data = new StringBuffer(Long.toString(time));
//                    data.setLength(packageSize * 1024);
//
//                    // start noting
//                    Utils.appendTestInfo(fileLabel, "" + time, time);
//
//                    // and send
//                    sendFrame("ADFB", data.toString().getBytes());
//
//                    UITools.writeLog(TestActivity.this, infoText, ++count + " " + data.toString().trim());
//                }
//            });


        };
    }

//    // TEST:
//    class GetTimeServer extends Thread {
//        TimeListener timeListener;
//
//        public GetTimeServer(TimeListener _timeListener) {
//            this.timeListener = _timeListener;
//            this.start();
//        }
//
//        public void run() {
//            try {
//                long startTime = System.currentTimeMillis();
//                String timeServer = "http://129.123.7.172:3883/sm/getTime";
//                HttpURLConnection conn = (HttpURLConnection) new URL(timeServer).openConnection();
//                int code = conn.getResponseCode();
//                if (code == 200) {
//                    String lTime = IOUtils.toString(conn.getInputStream());
//                    long time = Long.parseLong(lTime);
//
//                    if (timeListener != null) {
//                        long consumeTime = System.currentTimeMillis() - startTime;
//                        long currentNTPTime = time - consumeTime;
//                        timeListener.timeReceived(currentNTPTime);
//                    }
//
//                } else {
//                    UITools.writeLog(TestActivity.this, infoText, "error: server unreached");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    interface TimeListener {
//        public void timeReceived(long time);
//    }

    private int increaseNumber(int input) {
        if (input == 0) return 1;   // warming up
        if (input == 1) return 1;
        if (input == 2) return 10;
        if (input == 3) return 50;
        if (input >= 4 && input <= 13) return (input - 3) * 100;
        if (input == 14) return 1200;
        if (input == 15) return 1500;
        return 0;
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
