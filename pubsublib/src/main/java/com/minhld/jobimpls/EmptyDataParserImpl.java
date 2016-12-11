package com.minhld.jobimpls;

import com.minhld.jobex.JobDataParser;
import com.minhld.utils.Utils;

/**
 *
 * Created by minhld on 01/28/2016.
 */
public class EmptyDataParserImpl implements JobDataParser {

    @Override
    public Class getDataClass() {
        return String.class;
    }

    @Override
    public Object loadObject(String path) throws Exception {
        return "please wait!";
    }

    @Override
    public Object parseBytesToObject(byte[] byteData) throws Exception {
        return Utils.deserialize(byteData);
    }

    @Override
    public byte[] parseObjectToBytes(Object objData) throws Exception {
        if (objData instanceof byte[]) {
            return (byte[]) objData;
        } else {
            return Utils.serialize(objData);
        }
    }

    @Override
    public byte[] getPartFromObject(Object data, int firstOffset, int lastOffset) {
        try {
            return parseObjectToBytes(data);
        } catch(Exception e) {
            return new byte[0];
        }
    }

    @Override
    public Object createPlaceHolder(Object dataObject) {
        return "placeholder:";
    }

    @Override
    public Object copyPartToHolder(Object placeholderObj, byte[] partObj, int firstOffset, int lastOffset) {
        String placeholderPath = (String) placeholderObj;
        placeholderPath = placeholderPath + partObj;
        return placeholderPath;
    }

    @Override
    public void destroyObject(Object data) {}

    @Override
    public boolean isObjectDestroyed(Object data) {
        return true;
    }

}
