package com.minhld.pbsbmid;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.minhld.pubsublib.Client;
import com.minhld.pubsublib.Worker;
import com.minhld.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.toActiveMqBtn)
    Button createGroupBtn;

    @BindView(R.id.to0mqBtn)
    Button discoverBtn;

    @BindView(R.id.toRabbitMqBtn)
    Button pubBtn;

    @BindView(R.id.subBtn)
    Button subBtn;

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
                    UITools.writeLog(MainActivity.this, infoText, strMsg);
                    break;
                }
            }
        }
    };

//    MidSupporter midSupporter;
    MidSupporter2 midSupporter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        infoText.setMovementMethod(new ScrollingMovementMethod());

//        midSupporter = new MidSupporter(this, mainUiHandler);
        midSupporter = new MidSupporter2(this, mainUiHandler);

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
//                new ExPublisher(UITools.GO_IP);
                new ExWorker();
            }
        });

        subBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Subscriber subscriber = new Subscriber(UITools.GO_IP);
//                subscriber.setMessageListener(new Subscriber.MessageListener() {
//                    @Override
//                    public void msgReceived(String topic, byte[] msg) {
//                        UITools.writeLog(MainActivity.this, infoText, topic + ": " + new String(msg));
//                    }
//                });
                new ExClient();
            }
        });
    }

    public class ExClient extends Client {

        public ExClient() {
            super(UITools.GO_IP);
        }

        @Override
        public void clientStarted(String clientId) {
            // TODO: 10/27/2016
        }

        @Override
        public void send() {
            sendMessage("HELLO THERE!");
        }

        @Override
        public void resolveResult(byte[] result) {
            UITools.writeLog(MainActivity.this, infoText, "From worker: " + new String(result));
        }
    }

    public class ExWorker extends Worker {

        public ExWorker() {
            super(MainActivity.this, UITools.GO_IP);
        }

        @Override
        public void workerStarted(String workerId) {
            // TODO: 10/27/2016  
        }

        @Override
        public void receivedTask(String clientId, int dataSize) {
            // TODO: 10/27/2016
        }

        @Override
        public void workerFinished(String workerId, TaskDone taskDone) {
            // TODO:
        }

        @Override
        public byte[] resolveRequest(byte[] request) {
            return "FROM WORKER ".getBytes();
        }
    }

//    public class ExPublisher extends Publisher {
//
//        public ExPublisher(String _groupIp) {
//            super(_groupIp, 2000);
//        }
//
//        @Override
//        public void send() {
//            String newDate = new Date().toString();
//            sendFrame("ADFB", newDate.getBytes());
//            sendFrame("CDEF", newDate.getBytes());
//        };
//    }

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
