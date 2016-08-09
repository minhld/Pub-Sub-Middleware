package com.minhld.pbsbmid;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.minhld.pubsublib.MidBroker;
import com.minhld.pubsublib.MidPublisher;
import com.minhld.wfd.Utils;
import com.minhld.wfd.WFDManager;

import java.util.Collection;

/**
 * Created by minhld on 8/6/2016.
 *
 * This class utilizes the pubsub library to provide full functionality of
 * publish-subscribe to the client application
 */
public class MidSupporter {
    Activity context;
    WFDManager wfdManager;
    IntentFilter mIntentFilter;
    WifiPeerListAdapter deviceListAdapter;
    MidBroker mBroker;


//    Handler mainUiHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case Utils.MESSAGE_READ_SERVER: {
//                    String strMsg = msg.obj.toString();
//                    UITools.writeLog(MainActivity.this, infoText, strMsg);
//                    break;
//                }
//                case Utils.MESSAGE_READ_CLIENT: {
//                    String strMsg = msg.obj.toString();
//                    UITools.writeLog(MainActivity.this, infoText, strMsg);
//                    break;
//                }
//                case Utils.MAIN_JOB_DONE: {
//
//                    break;
//                }
//                case Utils.MAIN_INFO: {
//                    String strMsg = (String) msg.obj;
//                    UITools.writeLog(MainActivity.this, infoText, strMsg);
//                    break;
//                }
//            }
//        }
//    };

    public MidSupporter(Activity context, final Handler mainHandler) {
        this.context = context;

        wfdManager = new WFDManager(this.context);
        wfdManager.setWFDListener(mainHandler);
        wfdManager.setBroadCastListener(new WFDManager.BroadCastListener() {
            @Override
            public void peerDeviceListUpdated(Collection<WifiP2pDevice> deviceList) {
                deviceListAdapter.clear();
                deviceListAdapter.addAll(deviceList);
                deviceListAdapter.notifyDataSetChanged();
            }

            @Override
            public void wfdEstablished(WifiP2pInfo p2pInfo) {
                if (p2pInfo.groupOwnerAddress == null) {
                    return;
                }

                String brokerIp = p2pInfo.groupOwnerAddress.getHostAddress();
                if (p2pInfo.groupFormed && p2pInfo.isGroupOwner) {
                    if (mBroker != null) {
                        mainHandler.obtainMessage(Utils.MESSAGE_INFO, "broker reused").sendToTarget();
                        return;
                    }

                    // the group owner will also become a broker
                    mBroker = new MidBroker(brokerIp, mainHandler);
                } else if (p2pInfo.groupFormed) {
                    // let user select to be either publisher or subscriber
//                    new MidPublisher(brokerIp, mainHandler).start();
                }
            }
        });
        mIntentFilter = wfdManager.getSingleIntentFilter();
        deviceListAdapter = new WifiPeerListAdapter(this.context, R.layout.row_devices, wfdManager);
    }

    /**
     * discover the peers in the WiFi peer-to-peer mobile network
     */
    public void discoverPeers() {
        // start discovering
        wfdManager.discoverPeers();
        mIntentFilter = wfdManager.getSingleIntentFilter();
    }

    /**
     * create itself to be a group owner to handle a private group
     */
    public void createGroup() {
        wfdManager.createGroup();
    }

    /**
     * return the peer list adapter (for UI usage)
     * @return
     */
    public WifiPeerListAdapter getDeviceListAdapter() {
        return deviceListAdapter;
    }

    /**
     * this should be added at the end of onPause on main activity
     */
    public void actOnPause() {
        if (wfdManager != null && mIntentFilter != null) {
            this.context.unregisterReceiver(wfdManager);
        }
    }

    /**
     * this should be added at the end of onResume on main activity
     */
    public void actOnResume() {
        if (wfdManager != null && mIntentFilter != null) {
            this.context.registerReceiver(wfdManager, mIntentFilter);
        }
    }
}
