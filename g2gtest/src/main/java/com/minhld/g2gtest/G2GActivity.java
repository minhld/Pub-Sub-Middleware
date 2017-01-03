package com.minhld.g2gtest;

import android.Manifest;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.minhld.g2glib.WifiBroader;
import com.minhld.g2glib.WifiConnector;
import com.minhld.g2glib.WifiNetworkListAdapter;
import com.minhld.g2glib.WifiPeerListAdapter;
import com.minhld.supports.Utils;

import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class G2GActivity extends AppCompatActivity {
    @BindView(R.id.createGroupBtn)
    Button createGroupBtn;

    @BindView(R.id.discoverBtn)
    Button discoverBtn;

    @BindView(R.id.getDirectInfoBtn)
    Button getDirectInfoBtn;

    @BindView(R.id.sendWifiDirectBtn)
    Button sendWifiDirectBtn;

    @BindView(R.id.searchWiFiBtn)
    Button searchWiFiBtn;

    @BindView(R.id.getWiFiInfoBtn)
    Button getWiFiInfoBtn;

    @BindView(R.id.sendWifiDataBtn)
    Button sendWiFiDataBtn;

    @BindView(R.id.deviceList)
    ListView deviceList;

    @BindView(R.id.wifiList)
    ListView wifiList;

    @BindView(R.id.infoText)
    TextView infoText;

    WifiBroader wifiBroader;
    WifiConnector orgWifiBroader;
    IntentFilter mIntentFilter;

    WifiPeerListAdapter deviceListAdapter;
    WifiNetworkListAdapter networkListAdapter;

    Handler mainUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Utils.MESSAGE_READ_SERVER: {
                    String strMsg = msg.obj.toString();
                    UITools.writeLog(G2GActivity.this, infoText, strMsg);
                    break;
                }
                case Utils.MESSAGE_READ_CLIENT: {
                    String strMsg = msg.obj.toString();
                    UITools.writeLog(G2GActivity.this, infoText, strMsg);
                    break;
                }
                case Utils.MAIN_JOB_DONE: {

                    break;
                }
                case Utils.MAIN_INFO: {
                    String strMsg = (String) msg.obj;
                    UITools.writeLog(G2GActivity.this, infoText, strMsg);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_g2_g);

        ButterKnife.bind(this);
        infoText.setMovementMethod(new ScrollingMovementMethod());

        // ------ Prepare for WiFi Direct ------
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

        // ------ Prepared for Original WiFi ------
        orgWifiBroader = new WifiConnector(this, infoText);
        orgWifiBroader.setSocketHandler(mainUiHandler);
        orgWifiBroader.setmWifiScanListener(new WifiConnector.WiFiScanListener() {
            @Override
            public void listReceived(List<ScanResult> mScanResults) {
                networkListAdapter.clear();
                networkListAdapter.addAll(mScanResults);
                networkListAdapter.notifyDataSetChanged();
            }
        });

        // WiFi network list
        networkListAdapter = new WifiNetworkListAdapter(this, R.layout.row_wifi, orgWifiBroader);
        wifiList.setAdapter(networkListAdapter);

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

        getDirectInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiBroader.requestGroupInfo();
            }
        });

        sendWifiDirectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiBroader.writeString("sent a ACK :)");
            }
        });

        searchWiFiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    orgWifiBroader.requestPermission(G2GActivity.this);
                } else {
                    // search for Wifi network list
                    orgWifiBroader.getWifiConnections();
                }
            }
        });

        getWiFiInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        sendWiFiDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // connect to one of the Wifi networks
                // wifiBroader.requestGroupInfo();
                orgWifiBroader.writeString("sent a WiFi ACK :)");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        orgWifiBroader.getWifiConnections();
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
