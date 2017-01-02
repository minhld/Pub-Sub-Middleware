package com.minhld.g2glib;

/**
 * Created by minhld on 01/28/2016
 */

import android.content.Context;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.minhld.g2gtest.R;
import com.minhld.g2gtest.UITools;

import java.util.List;

/**
 * Array adapter for ListFragment that maintains WifiP2pDevice list.
 */
public class WifiNetworkListAdapter extends ArrayAdapter<ScanResult> {
    private Context context;
    private List<ScanResult> items;
    private WifiConnector wifiWiFiBroader;

    /**
     * @param context
     * @param textViewResourceId
     */
    public WifiNetworkListAdapter(Context context, int textViewResourceId, WifiConnector wifiBroader) {
        super(context, textViewResourceId/*, objects*/);

        this.context = context;
        //this.items = objects;
        this.wifiWiFiBroader = wifiBroader;
    }

//    @Override
//    public WifiP2pDevice getItem(int position) {
//        return super.getItem(position);
//    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.row_wifi, null);
        }
        ScanResult result = this.getItem(position);
        if (result != null) {
            TextView top = (TextView) v.findViewById(R.id.device_name);
            TextView bottom = (TextView) v.findViewById(R.id.device_details);
            if (top != null) {
                top.setText(result.SSID);
            }
            if (bottom != null) {
                bottom.setText(result.capabilities);
            }
        }
        v.setOnClickListener(new DeviceClickListener(result));
        return v;
    }

    /**
     * this class hold one Device object to establish connection
     * to the device described by this object
     */
    private class DeviceClickListener implements View.OnClickListener {
        ScanResult result;

        public DeviceClickListener(ScanResult result) {
            this.result = result;
        }

        @Override
        public void onClick(View v) {
            // connect
            UITools.showInputDialog(WifiNetworkListAdapter.this.context, new UITools.InputDialogListener() {
                @Override
                public void inputDone(String resultStr) {
                    wifiWiFiBroader.connectWifiNetwork(DeviceClickListener.this.result, resultStr);
                }
            }, WifiConnector.PASSWORD);

        }
    }

}