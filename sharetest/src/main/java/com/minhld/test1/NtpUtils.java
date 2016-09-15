package com.minhld.test1;

import android.os.AsyncTask;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by minhld on 9/14/2016.
 * Refered to the code of MyShare project from Dr Y.K
 */
public class NtpUtils {

    /**
     * Gets time using NTPSync
     */
    public static long getTime() {
        AsyncTask<Void, Void, Long> getTimeTask = new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... unused) {
                long newTime = 0;

                try {
                    long sTime = System.currentTimeMillis();
                    long offset = getOffset();

                    long deviceTime = System.currentTimeMillis();
                    newTime = deviceTime + offset;

                    return newTime;

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(Long result) {
                super.onPostExecute(result);
            }
        };

        //application will stop
        try {
            synchronized (getTimeTask) {
                long off = getTimeTask.execute().get(700, TimeUnit.MILLISECONDS);
                getTimeTask = null;
                return off;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static long getOffset() {
        String ntpHostname = "0.pool.ntp.org";
        return contactNtpServer(ntpHostname);
    }

    public static long contactNtpServer(String ntpHost) {
        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(700);

        TimeInfo info = null;
        try {
            client.open();

            InetAddress hostAddr = InetAddress.getByName(ntpHost);
            info = client.getTime(hostAddr);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.close();
        }

        if (info != null) {
            // compute offset/delay if not already done
            info.computeDetails();
            return info.getOffset();
        } else {
            return 0;
        }
    }
}
