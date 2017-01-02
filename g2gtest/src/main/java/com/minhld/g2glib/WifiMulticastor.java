package com.minhld.g2glib;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by minhld on 8/3/2016.
 *
 * This class is to test the multicast feature
 * This will periodically send the broadcast message every 3 seconds
 */

public class WifiMulticastor extends AsyncTask {
    public static int MULTICAST_SOCKET_PORT = 8383;
    public static enum SocketInfo {
        SOCKET_ERROR,

    }

    private boolean enableMulticast = true;
    private int broadcastPeriod = 3000;

    public WifiMulticastor(boolean isMulticastEnabled, int... broadcastPeriods) {
        this.enableMulticast = isMulticastEnabled;
        if (broadcastPeriods.length > 0) {
            this.broadcastPeriod = broadcastPeriods[0];
        }
    }

    @Override
    protected Object doInBackground(Object[] params) {
        DatagramSocket socket = null;
        InetAddress group = null;

        // initiate the sender socket
        try {
            socket = new DatagramSocket(MULTICAST_SOCKET_PORT);
        } catch (SocketException se) {
            publishProgress(SocketInfo.SOCKET_ERROR);
        }

        // initiate the group
        try {
            group = InetAddress.getByName("225.0.0.1");
        } catch (UnknownHostException e) {
            socket.close();
            e.printStackTrace();
            publishProgress(SocketInfo.SOCKET_ERROR);
        }

        while (enableMulticast) {
            //Sending to Multicast Group
            String message = "";
            byte[] buf = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MULTICAST_SOCKET_PORT);
            try {
                socket.send(packet);
            } catch (IOException e) {
                socket.close();
                e.printStackTrace();
                publishProgress(SocketInfo.SOCKET_ERROR);
            }

            socket.close();

            // wait for 3 seconds before a new broadcast
            try {
                Thread.sleep(this.broadcastPeriod);
            } catch (Exception e) { }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        if (values.length > 0) {
            SocketInfo retVal = (SocketInfo)values[0];
            switch (retVal) {
                case SOCKET_ERROR: {
                    break;
                }
            }
        }
    }
}
