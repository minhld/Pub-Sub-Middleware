package com.minhld.edgehandlingtest;

import com.minhld.jobex.Job;
import com.minhld.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by minhld on 12/10/2016.
 */
public class EmptyJobImpl implements Job {
    public static final int WAITING_TIME = 100000;

	public Object exec(Object input) {
        // this will do nothing but waiting for very long - WAITING_TIME
        try {
            Thread.sleep(WAITING_TIME);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "finished waiting!";
    }
}
