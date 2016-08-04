package com.minhld.pbsbmid;

import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.minhld.pbsbmid.lib.Utils;
import com.minhld.pbsbmid.lib.WifiBroader;
import com.minhld.pbsbmid.lib.WifiPeerListAdapter;

import java.util.Collection;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.createGroupBtn)
    Button createGroupBtn;

    @BindView(R.id.discoverBtn)
    Button discoverBtn;

    @BindView(R.id.sendDataBtn)
    Button sendDataBtn;

    @BindView(R.id.deviceList)
    ListView deviceList;

    @BindView(R.id.infoText)
    TextView infoText;

    WifiBroader wifiBroader;
    IntentFilter mIntentFilter;
    WifiPeerListAdapter deviceListAdapter;

    Handler mainUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Utils.MESSAGE_READ_SERVER: {
                    String strMsg = msg.obj.toString();
                    UITools.writeLog(MainActivity.this, infoText, strMsg);
                    break;
                }
                case Utils.MESSAGE_READ_CLIENT: {
                    String strMsg = msg.obj.toString();
                    UITools.writeLog(MainActivity.this, infoText, strMsg);
                    break;
                }
                case Utils.MAIN_JOB_DONE: {

                    break;
                }
                case Utils.MAIN_INFO: {
                    String strMsg = (String) msg.obj;
                    UITools.writeLog(MainActivity.this, infoText, strMsg);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        infoText.setMovementMethod(new ScrollingMovementMethod());

        wifiBroader = new WifiBroader(this, infoText);
        wifiBroader.setSocketHandler(mainUiHandler);
        wifiBroader.setBroadCastListener(new WifiBroader.BroadCastListener() {
            @Override
            public void peerDeviceListUpdated(Collection<WifiP2pDevice> deviceList) {
                deviceListAdapter.clear();
                deviceListAdapter.addAll(deviceList);
                deviceListAdapter.notifyDataSetChanged();
            }

            @Override
            public void socketUpdated(Utils.SocketType socketType, boolean connected) {

            }
        });
        mIntentFilter = wifiBroader.getSingleIntentFilter();

        // device list
        deviceListAdapter = new WifiPeerListAdapter(this, R.layout.row_devices, wifiBroader);
        deviceList.setAdapter(deviceListAdapter);


        createGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiBroader.createGroup();
            }
        });

        discoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiBroader.discoverPeers();
            }
        });

        sendDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(wifiBroader);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.registerReceiver(wifiBroader, mIntentFilter);
    }
}
