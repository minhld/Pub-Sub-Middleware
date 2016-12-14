package com.minhld.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import org.apache.commons.io.FileUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

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

    public static final int STATUS_LIST_PORT = 5558;
    public static final int STATUS_RESP_PORT = 5559;
    public static final int WAITING_TIME = 15000;
    public static final int CLIENT_WAIT_TIME = 3000;
    public static final int CLIENT_WAIT_REESTABLISH = 3000;
    public static final int BROKER_TIMEOUT = 2000;

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
    public static final String JOB_FILE_NAME = "job.jar";
    public static final String JOB_CLASS_NAME = "com.minhld.jobex.JobImpl";
    public static final String PARSER_CLASS_NAME = "com.minhld.jobex.JobDataParserImpl";
    public static final String JOB_EXEC_METHOD = "exec";
    public static final String ID_DELIMITER = "@@@";

    public static final String MSG_ACK = "ACK";
    public static final int MAX_ACK_SIZE = 1024;

    public static final String BROKER_GENERAL_IP = "*";
    public static final String BROKER_SPECIFIC_IP = "192.168.49.1";
    public static final String BROKER_DELIMITER = "";
    public static final String WORKER_READY = "READY";
    public static final String CLIENT_REQ_RES = "CLIENT_REQ";
    public static final String WORKER_REP_RES = "WORKER_REP";
    public static final int MAX_WORKERS_PER_JOB = 5;

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

//    /**
//     *
//     * @param c
//     * @param jobBytes
//     * @return
//     * @throws Exception
//     */
//    public static Class getObject(Context c, byte[] jobBytes) throws Exception {
//        // save the job data to file
//        String objectPath = Utils.getDownloadPath() + "/" + JOB_FILE_NAME_DL;
//        FileUtils.writeByteArrayToFile(new File(objectPath), jobBytes);
//
//        // load class from local jar file
//        String dexDir = c.getDir("dex", 0).getAbsolutePath();
//        ClassLoader parent  = c.getClass().getClassLoader();
//        DexClassLoader loader = new DexClassLoader(objectPath, dexDir, null, parent);
//        return loader.loadClass(JOB_CLASS_NAME);
//    }

    /**
     * this function will execute a class that is stored in Download folder
     *
     * @param c
     * @return
     * @throws Exception
     */
    public static Object runRemote(Context c, String jobPath, Object srcObject, Class type) throws Exception {
        // check if the files are valid or not
        if (!new File(jobPath).exists()) {
            throw new Exception("job or data file does not exist");
        }

        // address the class object and its executable method
        String dexDir = c.getDir("dex", 0).getAbsolutePath();
        ClassLoader parent  = c.getClass().getClassLoader();
        DexClassLoader loader = new DexClassLoader(jobPath, dexDir, null, parent);
        Class jobClass = loader.loadClass(JOB_CLASS_NAME);
        Object o = jobClass.newInstance();
        Method m = jobClass.getMethod(JOB_EXEC_METHOD, type);

        // address the resource
        return m.invoke(o, srcObject);
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

    /**
     * appends a test value (key + value) to a text file in the Download folder
     *
     * @param fileName
     * @param test
     * @param values
     */
    public static void appendTestInfo(String fileName, String test, long... values) {
        try {
            File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String testResultPath = downloadFolder.getAbsolutePath() + "/" + fileName + ".txt";
            FileWriter writer = new FileWriter(testResultPath, true);
            if (values.length == 0) {
                writer.write(test + " " + new Date().getTime() + "\n");
            } else {
                writer.write(test);
                for (int i = 0; i < values.length; i++) {
                    writer.write(" " + values[i]);
                }
                writer.write("\n");
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String genDRL(String workerId, float cpu, float cpuUsed,
                                float mem, float memUsed, float bat, float batUsed) {
        String retStr = "Id=" + workerId + ";DRL=";
        // this is out of service
        if (batUsed >= 0.8) return retStr + "0";

        float DRL = (cpu * 2f - cpuUsed * 0.5f) + (mem * 1.5f - memUsed * 0.5f);
        return retStr + DRL;
    }

    /**
     *
     * @param resp
     * @return
     */
    public static Object getResponse(String resp, String infoType) {
        String[] elements = resp.split(";");

        if (infoType.equals("drl")) {
            return Float.parseFloat(elements[1].split("=")[1]);
        } else if (infoType.equals("id")){
            return elements[0].split("=")[1];
        }

        return null;
    }

    /**
     * delete file/folder and the whole inner files
     *
     * @param file
     * @throws IOException
     */
    public static void delete(File file)
            throws IOException{
        if(file.isDirectory()){
            if(file.list().length==0){
                file.delete();
            }else{
                String files[]=file.list();
                for (String temp:files){
                    File fileDelete=new File(file,temp);
                    delete(fileDelete);
                }

                if(file.list().length==0){
                    file.delete();
                }
            }
        }else{
            file.delete();
        }
    }

    public static String unzipFile(String zipFilePath, String outputFolder, boolean overwrite) throws Exception{
        byte[] buffer = new byte[1024];

        try{
            if (overwrite) {
                File folder = new File(outputFolder);
                if (folder.exists()) {
                    delete(folder);
                }
                folder.mkdir();
            }

            File zipFile = new File(zipFilePath);
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();
            String outputFile = "";
            while(ze != null) {
                // only process file - skip folder
                if (ze.isDirectory()) {
                    ze = zis.getNextEntry();
                    continue;
                }
                String fileName = ze.getName();
                outputFile = outputFolder + File.separator + fileName;
                File newFile = new File(outputFile);
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0){
                    fos.write(buffer, 0, len);
                }
                fos.close();
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();

            return outputFolder;
        }catch(IOException e){
            throw e;
        }
    }
}
