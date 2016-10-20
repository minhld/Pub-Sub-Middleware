package com.minhld.pubsublib;

import android.content.Context;

import com.minhld.jobex.JobDataParser;
import com.minhld.jobex.JobPackage;
import com.minhld.utils.Utils;

import org.apache.commons.io.FileUtils;

import java.io.File;

import dalvik.system.DexClassLoader;

/**
 * job helper will
 * Created by minhld on 10/19/2016.
 */

public class JobHelper {

    static JobPackage jobPackage = null;
    static JobDataParser dataParser = null;

    public static JobPackage getJobPackage(Context c, String clientId, byte[] jobBytes) {
        if (JobHelper.jobPackage != null) {
            return JobHelper.jobPackage;
        }



        return JobHelper.jobPackage;
    }

    public static JobDataParser getDataParser(Context c, String clientId, byte[] jobBytes) {
        // if the data parser is already available, just return it
        if (JobHelper.dataParser != null) {
            return JobHelper.dataParser;
        }

        // otherwise we need to store the job into
        //preliminaryWork(c, clientId, jobBytes, Utils.PARSER_CLASS_NAME);

        return JobHelper.dataParser;
    }

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
}
