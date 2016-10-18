package com.minhld.jobex;

import com.minhld.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 *
 * Created by minhld on 10/11/2016.
 */

public class JobPackage implements Serializable {
    public int index;
    public byte[] dataBytes;
    public byte[] jobBytes;

    public JobPackage(int index, byte[] data, byte[] job) {
        this.index = index;
        this.dataBytes = data;
        this.jobBytes = job;
    }

    public JobPackage(int index, byte[] data, String jobPath) {
        this.index = index;
        this.dataBytes = data;
        try {
            this.jobBytes = Utils.readFile(new File(jobPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * return a serialized byte array of the current job object
     *
     * @return
     */
    public byte[] toByteArray() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] packageBytes = Utils.serialize(this);
            byte[] packageLength = Utils.intToBytes(packageBytes.length);
            bos.write(packageLength, 0, packageLength.length);
            bos.write(packageBytes, 0, packageBytes.length);
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
