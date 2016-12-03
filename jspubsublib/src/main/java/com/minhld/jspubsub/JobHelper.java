package com.minhld.jspubsub;

import android.content.Context;

import com.minhld.jobex.Job;
import com.minhld.jobex.JobDataParser;
import com.minhld.utils.Utils;

import org.apache.commons.io.FileUtils;

import java.io.File;

import dalvik.system.DexClassLoader;

/**
 * job helper helps loading job and data parser classes, as well as support invocation
 * of these classes' functions.
 *
 * Created by minhld on 10/19/2016.
 */

public class JobHelper {

    static Job job = null;
    static JobDataParser dataParser = null;

    /**
     * get job class by loading job.jar package (which is prematurely stored in local storage)
     *
     * @param c
     * @param clientId
     * @param jobBytes
     * @return
     */
    public static Job getJob(Context c, String clientId, byte[] jobBytes) {
        if (JobHelper.job != null) {
            return JobHelper.job;
        }

        // otherwise we need to store the job into
        try {
            Class dataParserClass = preliminaryWork(c, clientId, jobBytes, Utils.JOB_CLASS_NAME);
            JobHelper.job = (Job) dataParserClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return JobHelper.job;
    }

    /**
     * get data parser: get data parser from the job.jar file. \n
     * this function may return the data parser immediately since it's already loaded from the
     * last calls. this function may also return null if the intent class could not be found
     * in the jar.
     *
     * @param c
     * @param clientId
     * @param jobBytes
     * @return
     */
    public static JobDataParser getDataParser(Context c, String clientId, byte[] jobBytes) {
        // if the data parser is already available, just return it
        if (JobHelper.dataParser != null) {
            return JobHelper.dataParser;
        }

        //
        try {
            Class dataParserClass = preliminaryWork(c, clientId, jobBytes, Utils.PARSER_CLASS_NAME);
            JobHelper.dataParser = (JobDataParser) dataParserClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return JobHelper.dataParser;
    }

    /**
     * this function will do these following works
     *  - check if the job file has been downloaded to the local storage
     *  - if not then download, otherwise load the job file using dex classloader
     *  - load the class
     *
     * @param c
     * @param clientId
     * @param jobBytes
     * @param className
     * @return
     * @throws Exception
     */
    private static Class preliminaryWork(Context c, String clientId, byte[] jobBytes, String className) throws Exception {
        // save the job (jar package) to a file stored in local storage
        String dataPath = Utils.getDownloadPath() + "/" + clientId + "_" + Utils.JOB_FILE_NAME;
        File jobFile = new File(dataPath);
        if (!jobFile.exists()) {
            FileUtils.writeByteArrayToFile(jobFile, jobBytes);
        }

        // load class from local jar file
        String dexDir = c.getDir("dex", 0).getAbsolutePath();
        ClassLoader parent  = c.getClass().getClassLoader();
        DexClassLoader loader = new DexClassLoader(dataPath, dexDir, null, parent);
        return loader.loadClass(className);
    }

    /**
     * remove the job file out of the download folder after job has finished
     *
     * @param clientId
     */
    public static void removeJobFile(String clientId) {
        // search for the path of [clientId]_job.jar
        String dataPath = Utils.getDownloadPath() + "/" + clientId + "_" + Utils.JOB_FILE_NAME;
        File jobFile = new File(dataPath);
        try {
            if (!jobFile.exists()) {
                FileUtils.forceDelete(jobFile);
            }
        } catch (Exception e) {
            // this shouldn't be a case
            e.printStackTrace();
        }
    }
}
