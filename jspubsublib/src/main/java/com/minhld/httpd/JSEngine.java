package com.minhld.httpd;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.minhld.utils.Utils;


/**
 * Created by minhld on 3/28/2016.
 * This class takes control of the Local web server, and local files
 */
public class JSEngine {
    static final String TAG = "JSEngine";
    public static Object data;

    static WebView hiddenWeb;
    static JavaScriptInterface.JobDoneListener listener;

    public static void setJobDoneListener(JavaScriptInterface.JobDoneListener listener) {
        JSEngine.listener = listener;
    }

    /**
     * this function copies all necessary files including HTML & JS
     * to a place in local storage for the web-view usage
     */
    public static void initJs(Activity activity) {
        try {
            // ------ write the jquery file to the local storage ------
            String containerPath = Utils.getDownloadPath();
            Utils.copyAssets2Storage(activity, "jquery.js", containerPath);

            // ------ write the jquery file to the local storage ------
            Utils.copyAssets2Storage(activity, "test.html", containerPath);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // ------ initiate javascript interface of the web-view ------
        JavaScriptInterface jsInterface = new JavaScriptInterface(hiddenWeb);
        jsInterface.setJobDoneListener(new JavaScriptInterface.JobDoneListener() {
            @Override
            public void jobDone(Object data) {
                JSEngine.listener.jobDone(data);
            }
        });

        // ------ temporary web-view ------
        hiddenWeb = new WebView(activity);
        hiddenWeb.addJavascriptInterface(jsInterface, JavaScriptInterface.JAVASCRIPT_MODULE);
        hiddenWeb.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.v(TAG, consoleMessage.lineNumber() + ": " + consoleMessage.message());
                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                // silently go
            }
        });
        hiddenWeb.getSettings().setJavaScriptEnabled(true);
        hiddenWeb.getSettings().setDomStorageEnabled(true);

        if (Build.VERSION.SDK_INT >= 19) {
            hiddenWeb.setWebContentsDebuggingEnabled(true);
        }
    }

    /**
     * initiate the local web server
     * the port will be defined in AsslHTTPD.SERVER_DEF_PORT (default at 3883)
     *
     * @param context
     */
    public static void startServer(Context context) {
        try {
            AsslWebServer.startServer(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this function will load the html to load the javascript code
     */
    public static void execJob() {
        // using async task to load the web-view on main UI thread
        (new AsyncTask() {
            String testUrl = "";
            @Override
            protected Object doInBackground(Object[] params) {
                // ------ load the html file to run the javascript file ------
                String testFilePath = Utils.getDownloadPath() + "/test.html";
                testUrl = AsslWebServer.getFile(testFilePath);
                publishProgress();

                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                hiddenWeb.loadUrl(testUrl);
            }
        }).execute();

    }

    /**
     * stop the local web server
     */
    public static void stopServer() {
        AsslWebServer.stopServer();
    }
}
