package com.minhld.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by minhld on 12/7/2015.
 */
public class PeerSpecs {
    public float cpuSpeed;
    public int cpuCoreNum;
    public float cpuUsage;

    public int memTotal;
    public float memUsage;

    public float batTotal;
    public float batUsage;

    public String deviceName;
    public String availability;
    public float RL;
    public String network;
    public String gps;

    public static PeerSpecs getPeerSpecsFromJSON(String jsonData) {
        PeerSpecs ps = new PeerSpecs();

        try {
            JsonObject jsonObj = new JsonParser().parse(jsonData).getAsJsonObject();
            // basic information
            ps.deviceName = jsonObj.get("device").getAsString();
            ps.availability = jsonObj.get("availability").getAsString();
            ps.RL = jsonObj.get("RL").getAsFloat();
            ps.network = jsonObj.get("network").getAsString();
            ps.gps = jsonObj.get("gps").getAsString();

            // cpu
            JsonObject cpuAll = jsonObj.getAsJsonObject("cpu");
            ps.cpuCoreNum = cpuAll.get("cores").getAsInt();
            ps.cpuSpeed = cpuAll.get("speed").getAsFloat();
            ps.cpuUsage = cpuAll.get("usage").getAsFloat();

            // memory
            JsonObject memAll = jsonObj.getAsJsonObject("memory");
            ps.memTotal = memAll.get("total").getAsInt();
            ps.memUsage = memAll.get("usage").getAsFloat();

            // battery
            JsonObject battAll = jsonObj.getAsJsonObject("battery");
            ps.batTotal = battAll.get("total").getAsFloat();
            ps.batUsage = battAll.get("usage").getAsFloat();

        } catch (Exception e) {

        }

        return ps;
    }

    public static String getMyJSONSpecs(Context c, String deviceName) {
        PeerSpecs ps = getMySpecs(c);
        JsonObject jsonSpecs = new JsonObject();
//        JSONObject jsonSpecs = new JSONObject();
        try {
            // general information
            if (deviceName.equals("")) {
                deviceName = Build.MODEL;
            }
            jsonSpecs.addProperty("device", deviceName);
            jsonSpecs.addProperty("RL", ps.RL);
            jsonSpecs.addProperty("availability", ps.availability);
            jsonSpecs.addProperty("network", "on");
            jsonSpecs.addProperty("gps", "off");

            // specific data
            JsonObject jsonCPU = new JsonObject();
            jsonCPU.addProperty("usage", ps.cpuUsage);
            jsonCPU.addProperty("speed", ps.cpuSpeed);
            jsonCPU.addProperty("cores", ps.cpuCoreNum);
            jsonSpecs.add("cpu", jsonCPU);

            JsonObject jsonMem = new JsonObject();
            jsonMem.addProperty("usage", ps.memUsage);
            jsonMem.addProperty("total", ps.memTotal);
            jsonSpecs.add("memory", jsonMem);

            JsonObject jsonBattery = new JsonObject();
            jsonBattery.addProperty("usage", ps.batUsage);
            jsonBattery.addProperty("total", ps.batTotal);
            jsonSpecs.add("battery", jsonBattery);

            return jsonSpecs.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * get current specs of the device
     * @return
     */
    public static PeerSpecs getMySpecs(Context c) {
        PeerSpecs ps = new PeerSpecs();

        // cpu
        ps.cpuSpeed = getCpuTotal();
        ps.cpuCoreNum = 1;
        ps.cpuUsage = readUsage();

        // memory
        float[] mems = readMem2(c);
        ps.memTotal = (int) mems[0];
        ps.memUsage = mems[1];

        // battery
        ps.batTotal = getBatteryCapacity(c);
        ps.batUsage = getBatteryUsage(c);

        // get available threshold
        String availThresStr = Utils.getConfig("availability-threshold");
        float availThres = Float.parseFloat(availThresStr);

        ps.availability = ps.batUsage > availThres ? "on" : "off";
        ps.RL = (ps.cpuSpeed * ps.cpuCoreNum / ps.cpuUsage) +
                (ps.memTotal / ps.memUsage) +
                (ps.batTotal / (ps.batUsage * 1000));

        return ps;
    }

    public static float getBatteryCapacity(Context c) {
        Object mPowerProfile_ = null;

        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";

        try {
            mPowerProfile_ = Class.forName(POWER_PROFILE_CLASS).
                            getConstructor(Context.class).newInstance(c);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            double batteryCapacity = (Double) Class
                    .forName(POWER_PROFILE_CLASS)
                    .getMethod("getAveragePower", java.lang.String.class)
                    .invoke(mPowerProfile_, "battery.capacity");
            return (float) batteryCapacity;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static float getBatteryUsage(Context c) {
        Intent batteryIntent = c.registerReceiver(null,
                            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }

    public static float[] readMem2(Context c) {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memInfo);

        long availableMegs = memInfo.availMem / 1048576L;
        float memTotal = (float) memInfo.totalMem / 1048576f;
        float memUsage = (memTotal - availableMegs) / memTotal;
        return new float[] { memTotal, memUsage };
    }

    public static float[] readMem() {
        RandomAccessFile reader;
        String line = "";
        float memTotal = 0, memFree = 0, memUsage = 0;
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            while ((line = reader.readLine()) != null) {
                line = line.toLowerCase();
                if (line.contains("memtotal:")) {
                    line = line.replaceAll("memtotal:", "").
                            replaceAll("kb","").trim();
                    memTotal = Float.parseFloat(line) / 1048576L;
                }
                if (line.contains("memfree:")) {
                    line = line.replaceAll("memfree:", "").
                            replaceAll("kb", "").trim();
                    memFree = Float.parseFloat(line) / 1048576L;
                    memUsage = (memTotal - memFree) / memTotal;
                }
            }
            reader.close();
            return new float[] { memTotal, memUsage };
        } catch (IOException e) {
            return new float[2];
        }

    }

    public static float getCpuTotal() {
        RandomAccessFile reader;
        String line = "";
        StringBuffer buffer = new StringBuffer();
        float cpuTotal = 0;
        try {
            reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r");
            while ((line = reader.readLine()) != null) {
                buffer.append(line.toLowerCase());
            }
            reader.close();
            return Float.parseFloat(buffer.toString()) / 1048576L;
        } catch (IOException e) {
            return 0;
        }
    }

    private static float readUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();

            String[] toks = load.split(" ");

            long idle1 = Long.parseLong(toks[5]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            try {
                Thread.sleep(360);
            } catch (Exception e) {}

            reader.seek(0);
            load = reader.readLine();
            reader.close();

            toks = load.split(" ");

            long idle2 = Long.parseLong(toks[5]);
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return 0;
    }

}
