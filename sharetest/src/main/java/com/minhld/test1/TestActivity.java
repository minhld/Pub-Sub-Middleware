package com.minhld.test1;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.minhld.pbsbmid.MidSupporter;
import com.minhld.pbsbmid.R;
import com.minhld.pbsbmid.UITools;
import com.minhld.pubsublib.Publisher;
import com.minhld.pubsublib.Subscriber;
import com.minhld.wfd.Utils;

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
        setContentView(R.layout.activity_main);

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
                    public void msgReceived(String topic, byte[] msg) {
                        UITools.writeLog(TestActivity.this, infoText, topic + ": " + new String(msg));
                    }
                });

            }
        });
    }

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
            String newDate = new Date().toString();
            sendFrame("ADFB", newDate.getBytes());
            sendFrame("CDEF", newDate.getBytes());
        };
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
