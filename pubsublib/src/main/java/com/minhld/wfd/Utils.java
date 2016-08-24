package com.minhld.wfd;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by minhld on 9/22/2015.
 */
public class Utils {
    public static final int SERVER_PORT = 8881;
    public static final int BROKER_XPUB_PORT = 8383;
    public static final int BROKER_XSUB_PORT = 8388;
    public static final int SERVER_TIMEOUT = 5000;
    public static final int MESSAGE_READ_CLIENT = 0x500 + 1;
    public static final int MESSAGE_READ_SERVER = 0x500 + 2;
    public static final int MESSAGE_READ_JOB_SENT = 0x500 + 3;
    public static final int MESSAGE_READ_NO_FILE = 0x500 + 5;
    public static final int MESSAGE_INFO = 0x500 + 6;

    public static enum PubSubType {
        PubSub,
        Router
    }

    public static enum BrokerType {
        Broker,
        Brokerless
    }

    // same value as MESSAGE_READ_SERVER, because it will be used for replacing
    // each other sometimes.
    public static final int JOB_OK = 0x500 + 2;
    public static final int JOB_FAILED = -1;

    public static final byte JOB_TYPE_ACK = 50;
    public static final byte JOB_TYPE_ORG = 1;

    public static class WiFiDirectStatus {
        public static final int AVAILABLE = 3;
        public static final int CONNECTED = 0;
        public static final int FAILED = 2;
        public static final int INVITED = 1;
        public static final int UNAVAILABLE = 4;
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static final int ACK_WAIT = 20000;

    public static final int MAIN_JOB_DONE = 1;
    public static final int MAIN_INFO = -1;

    public static final SimpleDateFormat SDF = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    public static final String JOB_FILE_NAME = "Job.jar";
    public static final String JOB_CLASS_NAME = "com.minhld.jobs.Job";
    public static final String JOB_EXEC_METHOD = "exec";

    public static final String MSG_ACK = "ACK";
    public static final int MAX_ACK_SIZE = 1024;

    public static final String BROKER_GENERAL_IP = "*";
    public static final String BROKER_SPECIFIC_IP = "192.168.49.1";
    public static final String BROKER_DELIMITER = "";
    public static final String WORKER_READY = "READY";
    public static final String CLIENT_REQ_RES = "CLIENT_REQ";
    public static final String WORKER_REP_RES = "WORKER_REP";

    public enum SocketType {
        SERVER,
        CLIENT
    }

    public static class XDevice {
        public String address;
        public String name;

        public XDevice () {}

        public XDevice (String address, String name) {
            this.address = address;
            this.name = name;
        }
    }

    /**
     * list of connected client devices that currently connect to current server<br>
     * this list will be used as iterating devices for sending, checking, etc...
     */
    public static Map<String, XDevice> connectedDevices = new HashMap<>();

    /**
     * list of configuration options
     */
    public static HashMap<String, String> configs = new HashMap<>();

    /**
     * Serialize an object to binary array
     *
     * @param obj
     * @return
     * @throws IOException
     */
    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
        return b.toByteArray();
    }

    /**
     * Deserialize an object from a binary array
     *
     * @param bytes
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        return o.readObject();
    }

    /**
     * this function will execute a class that is stored in Download folder
     *
     * @param c
     * @return
     * @throws Exception
     */
    public static Object runRemote(Context c, String jobPath, Object srcObject, Class type) throws Exception {
//        // check if the files are valid or not
//        if (!new File(jobPath).exists()) {
//            throw new Exception("job or data file does not exist");
//        }
//
//        // address the class object and its executable method
//        String dex_dir = c.getDir("dex", 0).getAbsolutePath();
//        ClassLoader parent  = c.getClass().getClassLoader();
//        DexClassLoader loader = new DexClassLoader(jobPath, dex_dir, null, parent);
//        Class jobClass = loader.loadClass(JOB_CLASS_NAME);
//        Object o = jobClass.newInstance();
//        Method m = jobClass.getMethod(JOB_EXEC_METHOD, type);
//
//        // address the resource
//        return m.invoke(o, srcObject);
        // this is urgent case so that we wont use DexClassLoader

        // we will use Job object directly
        return null;
    }

    /**
     * write data from a byte array to file
     *
     * @param outputFilePath
     * @param data
     * @throws IOException
     */
    public static void writeFile(String outputFilePath, byte[] data) throws IOException {

        FileOutputStream fos = new FileOutputStream(outputFilePath);
        fos.write(data, 0, data.length);
        fos.flush();
        fos.close();
    }

    public static void grandWritePermission(Activity activity) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            int permission = ActivityCompat.checkSelfPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        }
    }

    /**
     * read file and return binary array
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static byte[] readFile(File file) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        int read = 0;
        byte[] buff = new byte[1024];
        while ((read = fis.read(buff)) != -1) {
            bos.write(buff, 0, read);
        }
        return bos.toByteArray();
    }

    public static String getConfig(String keyConfig) {
        return Utils.configs.get(keyConfig);
    }

    /**
     * get predefined app configuration when it is loading
     * @return
     */
    public static HashMap<String, String> readConfigs(Context c) {
        try {
            XmlPullParser xmlParser = XmlPullParserFactory.newInstance().newPullParser();
            xmlParser.setInput(c.getAssets().open("config.xml"), "utf-8");

            int eventType = xmlParser.getEventType();
            String tagName = "", tagText = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                tagName = xmlParser.getName();
                switch (eventType) {
                    case XmlPullParser.TEXT:
                        tagText = xmlParser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("role")) {
                            configs.put(tagName, tagText);
                        } else if (tagName.equalsIgnoreCase("availability-threshold")) {
                            configs.put(tagName, tagText);
                        }
                        break;

                    default:
                        break;
                }
                eventType = xmlParser.next();
            }
        } catch (Exception e) {
            // leave it here
            e.printStackTrace();
        }
        return configs;
    }

    public static byte[] intToBytes(int val) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(val).array();
    }

    public static int bytesToInt(byte[] arr) {
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * get the absolute path of the default Download folder
     *
     * @return
     */
    public static String getDownloadPath() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    }

}
