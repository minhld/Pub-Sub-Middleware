package com.minhld.wordcounttest;

import com.minhld.jobex.JobDataParser;
import com.minhld.utils.Utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * Created by minhld on 01/28/2016.
 */
public class WordDataParserImpl implements JobDataParser {

    @Override
    public Class getDataClass() {
        return String.class;
    }

    @Override
    public Object loadObject(String path) throws Exception {
        String url = "http://129.123.7.172:3883/sm/html/b19.html";
        return getTextFromHttps(url);
    }

    @Override
    public Object parseBytesToObject(byte[] byteData) throws Exception {
        return new String(byteData);
    }

    @Override
    public byte[] parseObjectToBytes(Object objData) throws Exception {
        return ((String) objData).getBytes();
    }

    @Override
    public byte[] getPartFromObject(Object data, int firstOffset, int lastOffset) {
        String dataStr = (String) data;
        String subData = dataStr.substring(firstOffset, lastOffset);
        return subData.getBytes();

    }

    @Override
    public Object createPlaceHolder(Object dataObject) {
        return new TopWords();
    }

    @Override
    public Object copyPartToHolder(Object placeholderObj, byte[] partObj, int firstOffset, int lastOffset) {
        String listWords = new String(partObj);
        TopWords topWords = (TopWords) placeholderObj;
        try {
            JSONObject jsonWords = new JSONObject(listWords);
            Iterator<String> wordKeys = jsonWords.keys();
            String key = "";
            int count = 0;
            while (wordKeys.hasNext()) {
                key = wordKeys.next();
                if (topWords.words.containsKey(key)) {
                    count = topWords.words.get(key) + jsonWords.getInt(key);
                } else {
                    count = jsonWords.getInt(key);
                }
                topWords.words.put(key, count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // return
        return topWords;
    }

    @Override
    public void destroyObject(Object data) {
        data = null;
    }

    @Override
    public boolean isObjectDestroyed(Object data) {
        return true;
    }

    public class TopWords {
        public HashMap<String, Integer> words = new HashMap<>();
    }

    private String getTextFromHttps(String url) {
        try {
            // open http connection
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

            // get the page's data
            StringBuffer buffer = new StringBuffer();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            while ((line = br.readLine()) != null){
                buffer.append(line);
            }
            br.close();

            return buffer.toString();
            //
            // return Jsoup.parse(buffer.toString()).text();
            // return android.text.Html.fromHtml(buffer.toString()).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
}
