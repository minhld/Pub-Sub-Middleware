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

import com.minhld.pubsublib.MidPublisher;
import com.minhld.pubsublib.MidSubscriber;
import com.minhld.wfd.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
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
                    UITools.writeLog(MainActivity.this, infoText, strMsg);
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
                new MidPublisher(UITools.GO_IP, mainUiHandler);
            }
        });

        subBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MidSubscriber(UITools.GO_IP, mainUiHandler);
            }
        });
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
