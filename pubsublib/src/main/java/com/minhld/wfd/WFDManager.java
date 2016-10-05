package com.minhld.wfd;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;

import com.minhld.utils.Utils;

import java.util.Collection;

/**
 * handles all WFD connection matters
 *
 * Created by minhld on 7/25/2016.
 */

public class WFDManager extends BroadcastReceiver {
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    IntentFilter mIntentFilter;

    Handler mHandler;
    BroadCastListener broadCastListener;

    // TEST:
    String connectedDeviceName = "";

    public void setWFDListener(Handler skHandler) {
        this.mHandler = skHandler;
    }

    public WFDManager(Activity c){
        this.mManager = (WifiP2pManager)c.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(c, c.getMainLooper(), null);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                writeLog("wifi p2p is enabled");
            } else {
                // Wi-Fi P2P is not enabled
                writeLog("wifi p2p is disabled");
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    Collection<WifiP2pDevice> deviceList = peers.getDeviceList();
                    reloadDeviceList(deviceList);

                    if (broadCastListener != null){
                        broadCastListener.peerDeviceListUpdated(deviceList);
                    }

                    writeLog(deviceList.size() + " devices was found");
                }
            });
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo info) {
                    if (broadCastListener != null) {
                        broadCastListener.wfdEstablished(info);
                    }
//                    if (info.groupFormed && info.isGroupOwner) {
//                        // if it is a group owner, it will become a broker
//                        String brokerIp = info.groupOwnerAddress.getHostAddress();
//                        new Broker(brokerIp, mWFDListener).execute();
//                        writeLog("broker started");
//                    } else if (info.groupFormed) {
//                        String brokerIp = info.groupOwnerAddress.getHostAddress();
//                        new Publisher(brokerIp, mWFDListener).execute();
//                    } else {
//
//                    }
                }
            });
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            writeLog("device's wifi state changed");
        }
    }

    /**
     * this will create a group and itself be a Group Owner
     */
    public void createGroup() {
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                writeLog("group created successfully");
            }

            @Override
            public void onFailure(int reason) {
                switch (reason){
                    case 0: {
                        writeLog("group create failed: operation failed due to an internal error");
                        break;
                    }
                    case 1: {
                        writeLog("group create failed: p2p is unsupported on the device");
                        break;
                    }
                    case 2: {
                        writeLog("group create failed: framework is busy and unable to service the request");
                        break;
                    }
                }
            }
        });
    }

    /**
     * get information of the group
     */
    public void requestGroupInfo() {
        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                writeLog("[g-info]: " + (group.isGroupOwner() ? "owner" : "p2p-clt") + "; " +
                        "name: " + group.getNetworkName() + "; " +
                        "pwd: " + group.getPassphrase());
            }
        });
    }

    /**
     * send the request
     */
    public void discoverPeers(){
        // TEST: start the clock
        Utils.appendTestInfo("discover", "discovery starts");

        this.mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // TEST: end the clock
                Utils.appendTestInfo("discover", "discovery ends");

                writeLog("discovery called successfully");
            }

            @Override
            public void onFailure(int reason) {
                switch (reason){
                    case 0: {
                        writeLog("discovery failed: operation failed due to an internal error");
                        break;
                    }
                    case 1: {
                        writeLog("discovery failed: p2p is unsupported on the device");
                        break;
                    }
                    case 2: {
                        writeLog("discovery failed: framework is busy and unable to service the request");
                        break;
                    }
                }
            }
        });
    }

    public void connectToADevice(final WifiP2pDevice device, final WifiP2pConnectionListener listener) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        // TEST: start connect clock
        Utils.appendTestInfo("connect", "connect " + device.deviceName + " starts");
        connectedDeviceName = device.deviceName;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                writeLog("connection established with " + device.deviceName + " successfully");

                if (listener != null) {
                    listener.connectInfoReturned(0);
                }
            }

            @Override
            public void onFailure(int reason) {
                writeLog("connection with " + device.deviceName + " failed");
                if (listener != null) {
                    listener.connectInfoReturned(reason);
                }
            }
        });
    }

    /**
     * disconnect with a peer
     *
     * @param deviceName
     * @param listener
     */
    public void disconnect(final String deviceName, final WifiP2pConnectionListener listener){
//        // close the current socket
//        mSocketHandler.dispose();

        // dispose the group it connected to
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                writeLog("disconnected with " + deviceName + " successfully");
                if (listener != null) {
                    listener.connectInfoReturned(0);
                }
            }

            @Override
            public void onFailure(int reason) {
                writeLog("disconnected from " + deviceName + " failed");
            }
        });
    }

    /**
     * connect to an available group owner
     *
     * @param ip
     * @param name
     */
    public void connect(final String ip, final String name) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = ip;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                writeLog("connection established with " + name + " successfully");
            }

            @Override
            public void onFailure(int reason) {
                writeLog("connection with " + name + " failed");
            }
        });
    }

    public IntentFilter getSingleIntentFilter() {
        if (mIntentFilter == null) {
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        }

        return mIntentFilter;
    }

    /**
     * re-add the whole list of devices
     *
     * @param devices
     */
    private void reloadDeviceList(Collection<WifiP2pDevice> devices) {
        Utils.connectedDevices.clear();

        Utils.XDevice xDev = null;
        for (WifiP2pDevice device : devices) {
            // only try the CONNECTED devices
            if (device.status == WifiP2pDevice.CONNECTED) {
                xDev = new Utils.XDevice(device.deviceAddress, device.deviceName);
                Utils.connectedDevices.put(device.deviceName, xDev);

                if (device.deviceName.equals(connectedDeviceName)) {
                    Utils.appendTestInfo("connect", "connect " + connectedDeviceName + " ends");
                }
            }
        }

    }

    public void setBroadCastListener(BroadCastListener pdlcListener){
        this.broadCastListener = pdlcListener;
    }

    /**
     * this class is to throw events from WFD Manager back to the UI thread
     */
    public interface BroadCastListener {
        /**
         * this is called when the device list changes
         * @param deviceList
         */
        public void peerDeviceListUpdated(Collection<WifiP2pDevice> deviceList);

        /**
         * called when wifi direct connection is established
         * @param p2pInfo
         */
        public void wfdEstablished(WifiP2pInfo p2pInfo);
    }

//    public void writeString(String msg) {
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        byte[] data = ("[" + deviceName + "] " + msg).getBytes();
//        byte[] lengthBytes = Utils.intToBytes(data.length);
//        bos.write(lengthBytes, 0, lengthBytes.length);
//        bos.write(data, 0, data.length);
//        sendObject(bos.toByteArray());
//    }

//    /**
//     * this function will send an object through socket to the server
//     *
//     * @param st should be a serializable object
//     */
//    public void sendObject(Object st) {
//        if (st instanceof byte[]) {
//            mSocketHandler.write((byte[])st);
//        }else {
//            try {
//                // we need to serialize it to binary array before dispatching it
//                mSocketHandler.write(Utils.serialize(st));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public void writeLog(final String msg){
        mHandler.obtainMessage(Utils.MESSAGE_INFO, msg).sendToTarget();
    }
}
