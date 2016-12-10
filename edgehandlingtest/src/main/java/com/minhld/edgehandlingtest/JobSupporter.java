package com.minhld.edgehandlingtest;

import android.content.Context;

import com.minhld.jobex.JobDataParser;
import com.minhld.jobimpls.*;
import com.minhld.jobimpls.NetDataParserImpl;

/**
 * Created by minhld on 10/17/2016.
 */

public class JobSupporter {
    static JobDataParser dataParser;

    public static void initDataParser(Context c, String jarPath) throws Exception {
        dataParser = new NetDataParserImpl();
    }

    public static byte[] getData(String filePath) throws Exception {
        Object dataObj = dataParser.loadObject(filePath);
        return dataParser.parseObjectToBytes(dataObj);
    }
}
