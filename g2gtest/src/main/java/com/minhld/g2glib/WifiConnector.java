package com.minhld.g2glib;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.text.format.Formatter;
import android.widget.TextView;

import com.minhld.supports.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

/**
 * Created by minhld on 7/26/2016.
 */

public class WifiConnector {
    public static final String SERVER_IP = "144.39.162.153"; //"144.39.248.223"; //"144.39.215.135"; //"144.39.254.119";// "192.168.49.1";
    public static final String PASSWORD = "DIWNZzzv"; // "qVK5TkO9"; // "NbNCLPiX"; // "EWat5Nhr";

    WifiManager mWifiManager;
    WiFiScanListener scanListener;

    SocketHandler mSocketHandler;
    Handler mSocketUIListener;

    String deviceName;
    TextView logText;

    public void setSocketHandler(Handler skHandler) {
        this.mSocketUIListener = skHandler;
    }

    public void setmWifiScanListener(WiFiScanListener scanListener) {
        this.scanListener = scanListener;
    }

    public WifiConnector(Activity c, TextView logText) {
        this.logText = logText;
        mWifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        IntentFilter filters = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filters.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        c.registerReceiver(mWifiScanReceiver, filters);
    }

    /**
     * receive a wifi network list
     */
    private BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> mScanResults = mWifiManager.getScanResults();
                if (scanListener != null) {
                    // only keep wifi direct network
                    for (int i = mScanResults.size() - 1; i >= 0; i--) {
                        if (!mScanResults.get(i).SSID.toLowerCase().contains("direct")) {
                            mScanResults.remove(i);
                        }
                    }
                    scanListener.listReceived(mScanResults);
                }
            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                if (SupplicantState.isValidState(state) && state == SupplicantState.COMPLETED) {
                    // when a wifi connection is established
                    boolean connected = checkConnectedToDesiredWifi(c);
                }
            }
        }
    };

    public void requestPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                0);
    }

    public void getWifiConnections() {
//        ActivityCompat.requestPermissions(activity,
//                new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
//                0);
        mWifiManager.startScan();
    }

    public boolean connectWifiNetwork(String SSID) {
//        writeLog("attempt connecting to " + wifiNetwork.SSID);
//        WifiConfiguration wifiConfiguration = new WifiConfiguration();
//        wifiConfiguration.SSID = "\"" + wifiNetwork.SSID + "\"";
//        wifiConfiguration.preSharedKey = "\"" + password + "\"";
////        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//        int netId = mWifiManager.addNetwork(wifiConfiguration);
//
//        List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
//        for( WifiConfiguration i : list ) {
//            if(i.SSID != null && i.SSID.equals("\"" + wifiNetwork.SSID + "\"") && i.networkId == netId) {
//                mWifiManager.disconnect();
//                mWifiManager.reconnect();
////                mWifiManager.saveConfiguration();
//                mWifiManager.enableNetwork(i.networkId, true);
//                break;
//            }
//        }
//
//        writeLog("wait for establishment...");

        WifiConfiguration configuration = createOpenWifiConfiguration(SSID);
        int networkId = mWifiManager.addNetwork(configuration);
        writeLog("networkId assigned while adding network is " + networkId);
        return enableNetwork(SSID, networkId);
    }

    private WifiConfiguration createOpenWifiConfiguration(String SSID) {
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = String.format("\"%s\"", SSID);
        configuration.preSharedKey = "\"" + PASSWORD + "\"";
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        assignHighestPriority(configuration);
        return configuration;
    }

    //To tell OS to give preference to this network
    private void assignHighestPriority(WifiConfiguration config) {
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (config.priority <= existingConfig.priority) {
                    config.priority = existingConfig.priority + 1;
                }
            }
        }
    }

    private boolean enableNetwork(String SSID, int networkId) {
        if (networkId == -1) {
            networkId = getExistingNetworkId(SSID);

            if (networkId == -1) {
                writeLog("Couldn't add network with SSID: " + SSID);
                return false;
            }
        }
        return mWifiManager.enableNetwork(networkId, true);
    }

    private int getExistingNetworkId(String SSID) {
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (SSID != null && SSID.equals("\"" + existingConfig.SSID + "\"")) {
                    return existingConfig.networkId;
                }
            }
        }
        return -1;
    }


    /** Detect you are connected to a specific network. */
    private boolean checkConnectedToDesiredWifi(Context c) {
        boolean connected = false;

        WifiManager wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);

        WifiInfo wifi = wifiManager.getConnectionInfo();
        if (wifi != null) {
            deviceName = wifi.getSSID();
            writeLog("connected to: " + deviceName + "; " +
                    "bssid: " + wifi.getBSSID() + "; " +
                    "IP: " + Formatter.formatIpAddress(wifi.getIpAddress()) + "; " +
                    "freq: " + wifi.getFrequency() + "MHz; " +
                    "speed: " + wifi.getLinkSpeed() + "Mbps; ");
        }

        // create client socket
        try {
            mSocketHandler = new ClientSocketHandler(mSocketUIListener, InetAddress.getByName(SERVER_IP));
            mSocketHandler.start();
        } catch (UnknownHostException uhEx) {
            writeLog("exception: " + uhEx.getMessage());
            uhEx.printStackTrace();
        }

        return connected;
    }

    public interface WiFiScanListener {
        public void listReceived(List<ScanResult> mScanResults);
    }

    public void writeLog(final String msg){
        String outMsg = Utils.SDF.format(new Date()) + ": " + msg + "\n";
        logText.append(outMsg);
    }

    public void writeString(String msg) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = ("[" + deviceName + "] " + msg).getBytes();
        byte[] lengthBytes = Utils.intToBytes(data.length);
        bos.write(lengthBytes, 0, lengthBytes.length);
        bos.write(data, 0, data.length);
        sendObject(bos.toByteArray());
    }

    /**
     * this function will send an object through socket to the server
     *
     * @param st should be a serializable object
     */
    public void sendObject(Object st) {
        if (st instanceof byte[]) {
            mSocketHandler.write((byte[])st);
        }else {
            try {
                // we need to serialize it to binary array before dispatching it
                mSocketHandler.write(Utils.serialize(st));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
