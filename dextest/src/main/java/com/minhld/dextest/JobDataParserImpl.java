package com.minhld.dextest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import com.minhld.jobex.JobDataParser;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 *
 * Created by minhld on 01/28/2016.
 */
public class JobDataParserImpl implements JobDataParser {

    @Override
    public Class getDataClass() {
        return Bitmap.class;
    }

    @Override
    public Object loadObject(String path) throws Exception {
        return BitmapFactory.decodeFile(path);
    }

    @Override
    public Object parseBytesToObject(byte[] byteData) throws Exception {
        return BitmapFactory.decodeByteArray(byteData, 0, byteData.length);
    }

    @Override
    public byte[] parseObjectToBytes(Object objData) throws Exception {
        Bitmap bmpData = (Bitmap) objData;

        // assign the binary data
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmpData.compress(Bitmap.CompressFormat.JPEG, 50, bos);
        byte[] byteData = bos.toByteArray();
        bos.close();

        return byteData;
    }

    @Override
    public byte[] getPartFromObject(Object data, int firstOffset, int lastOffset) {
        Bitmap bmpData = (Bitmap) data;
        int firstIndex = bmpData.getWidth() * firstOffset;
        int pieceWidth = bmpData.getWidth() * (lastOffset - firstOffset);
        Bitmap bmpPart = Bitmap.createBitmap(bmpData, (pieceWidth * firstIndex), 0, pieceWidth, bmpData.getHeight());
        try {
            return parseObjectToBytes(bmpPart);
        } catch (Exception e) {
            // no part return
            return null;
        }
    }

    @Override
    public String getJsonMetadata(Object objData) {
        Bitmap bmp = (Bitmap) objData;
        return "{ 'width': " + bmp.getWidth() + ", 'height': " + bmp.getHeight() + " }";
    }

    @Override
    public Object createObjectHolder(String jsonMetadata) {
        try {
            JSONObject resultObj = new JSONObject(jsonMetadata);
            int width = resultObj.getInt("width"), height = resultObj.getInt("height");
            Bitmap finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            return finalBitmap;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object copyPartToHolder(Object placeholderObj, byte[] partObj, int index) {
        // get bitmap from original data
        Bitmap partBmp = BitmapFactory.decodeByteArray(partObj, 0, partObj.length);

        int pieceWidth = partBmp.getWidth();
        Canvas canvas = new Canvas((Bitmap) placeholderObj);
        canvas.drawBitmap(partBmp, index * pieceWidth, 0, null);
        return null;
    }

    @Override
    public void destroyObject(Object data) {
        ((Bitmap) data).recycle();
    }

    @Override
    public boolean isObjectDestroyed(Object data) {
        return ((Bitmap) data).isRecycled();
    }
}
