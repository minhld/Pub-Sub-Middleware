package com.minhld.wordcounttest;

import android.content.Context;
import android.graphics.Bitmap;

import com.minhld.jobex.JobDataParser;

import dalvik.system.DexClassLoader;

/**
 * Created by minhld on 10/17/2016.
 */

public class JobSupporter {
    static JobDataParser dataParser;

    public static void initDataParser(Context c, String jarPath) throws Exception {
        String dexDir = c.getDir("dex", 0).getAbsolutePath();
        ClassLoader parent  = c.getClass().getClassLoader();
        DexClassLoader loader = new DexClassLoader(jarPath, dexDir, null, parent);
        Class dataParserClass = loader.loadClass("com.minhld.jobex.JobDataParserImpl");
        dataParser = (JobDataParser) dataParserClass.newInstance();
    }

    public static byte[] getData(String filePath) throws Exception {
        Bitmap bmp = (Bitmap) dataParser.loadObject(filePath);
        return dataParser.parseObjectToBytes(bmp);
    }
}
