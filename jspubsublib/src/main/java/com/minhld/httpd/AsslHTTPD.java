package com.minhld.httpd;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by minhld on 2/20/2016.
 */
public class AsslHTTPD extends NanoHTTPD {
    public static final String SERVER_DEF_HOST = "localhost";
    public static final int SERVER_DEF_PORT = 3883;

    final String TAG = "AsslHTTPD";

    static final String MIME_DEFAULT_BINARY = "application/octet-stream";
    static final Map<String,String> mimeTypes = new HashMap<String, String>() {{
        put("css", "text/css");
        put("htm", "text/html");
        put("html", "text/html");
        put("xhtml", "application/xhtml+xml");
        put("xml", "text/xml");
        put("json", "application/json");
        put("java", "text/x-java-source, text/java");
        put("md", "text/plain");
        put("txt", "text/plain");
        put("asc", "text/plain");
        put("gif", "image/gif");
        put("jpg", "image/jpeg");
        put("jpeg", "image/jpeg");
        put("png", "image/png");
        put("mp3", "audio/mpeg");
        put("m3u", "audio/mpeg-url");
        put("mp4", "video/mp4");
        put("ogv", "video/ogg");
        put("flv", "video/x-flv");
        put("mov", "video/quicktime");
        put("swf", "application/x-shockwave-flash");
        put("js", "application/javascript");
        put("pdf", "application/pdf");
        put("doc", "application/msword");
        put("ogg", "application/x-ogg");
        put("zip", "application/octet-stream");
        put("exe", "application/octet-stream");
        put("class", "application/octet-stream");
    }};

    AsslHTTPD asslServer;
    Context context;

    public AsslHTTPD(Context context) {
        super(AsslHTTPD.SERVER_DEF_HOST, AsslHTTPD.SERVER_DEF_PORT);
        this.context = context;
    }

    public AsslHTTPD(Context context, String hostName, int port){
        super(hostName, port);
        this.context = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response res = null;
        String uri = session.getUri();

        String downloadAbsPath = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

        if (uri.contains("favicon")){
            uri = "";
        }

        // Get MIME type from file name extension, if possible
        String mime = MIME_DEFAULT_BINARY;
        int dot = uri.lastIndexOf('.');
        if (dot >= 0) {
            mime = mimeTypes.get(uri.substring(dot + 1).toLowerCase());
        }

        String eTag = "";

        if (uri.contains(downloadAbsPath)){
            try{
                InputStream is = new FileInputStream(uri);
                res = new Response(Response.Status.OK, mime, is, is.available());
                if (res != null){
                    eTag = Integer.toHexString(new Random().nextInt());
                    res.addHeader("ETag", eTag);
                    res.addHeader("Connection", "Keep-alive");
                    return res;
                }
            }catch(IOException e){
                Log.v(TAG, e.getMessage());
            }
        } else {
            return null;
        }

        return null;
    }

}
