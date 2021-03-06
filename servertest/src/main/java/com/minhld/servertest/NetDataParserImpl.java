package com.minhld.servertest;

import com.minhld.jobex.JobDataParser;
import com.minhld.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 *
 * Created by minhld on 01/28/2016.
 */
public class NetDataParserImpl implements JobDataParser {

    @Override
    public Class getDataClass() {
        return String.class;
    }

    @Override
    public Object loadObject(String path) throws Exception {
        com.minhld.jobimpls.NetJobImpl.WebPart webPart = new com.minhld.jobimpls.NetJobImpl.WebPart();
        webPart.url = "http://giaitri.vnexpress.net/photo/trong-nuoc/buoi-toi-dam-am-cua-gia-dinh-my-linh-3339639.html";
        return webPart;
    }

    @Override
    public Object parseBytesToObject(byte[] byteData) throws Exception {
        NetJobImpl.WebPart webPart = (NetJobImpl.WebPart) Utils.deserialize(byteData);
        return webPart;
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
        NetJobImpl.WebPart webPart = new NetJobImpl.WebPart();
        webPart.url = (String) data;
        webPart.firstOffset = firstOffset;
        webPart.lastOffset = lastOffset;
        try {
            return parseObjectToBytes(webPart);
        } catch(Exception e) {
            return new byte[0];
        }
    }

    @Override
    public Object createPlaceHolder(Object dataObject) {
        // the place holder will be an empty folder
        // delete the placeholder if it is already there
        String placeholderPath = Utils.getDownloadPath() + "/web";
        File f = new File(placeholderPath);
        if (f.exists()) {
            try {
                Utils.delete(f);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        // create the placeholder
        f.mkdir();

        return placeholderPath;
    }

    @Override
    public Object copyPartToHolder(Object placeholderObj, byte[] partObj, int firstOffset, int lastOffset) {
        String placeholderPath = (String) placeholderObj;

        // save to a file
        String uuid = UUID.randomUUID().toString();
        String tempZipPath = Utils.getDownloadPath() + "/" + uuid + ".zip";

        try {
            FileOutputStream fos = new FileOutputStream(tempZipPath);
            fos.write(partObj, 0, partObj.length);
            fos.flush();
            fos.close();

            Utils.unzipFile(tempZipPath, placeholderPath, false);

            // delete the tempo ZIP file
            Utils.delete(new File(tempZipPath));
        } catch(Exception e) {

        }

        // return
        return placeholderPath;
    }

    @Override
    public void destroyObject(Object data) {
        data = null;
    }

    @Override
    public boolean isObjectDestroyed(Object data) {
        return true;
    }

}
