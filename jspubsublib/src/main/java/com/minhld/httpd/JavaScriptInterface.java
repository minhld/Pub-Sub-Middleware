package com.minhld.httpd;

import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.minhld.job2p.supports.Utils;


/**
 * Created by minhld on 2/22/2016.
 */
public class JavaScriptInterface {
    public static final String JAVASCRIPT_MODULE = "jsInterface";
    public static final String DTYPE_BASE64 = "base64";

    JobDoneListener jobDoneListener = null;
    WebView webView;

    public void setJobDoneListener(JobDoneListener jobDoneListener) {
        this.jobDoneListener = jobDoneListener;
    }

    public JavaScriptInterface(WebView webView) {
        this.webView = webView;
    }

    @JavascriptInterface
    public String getFile(String type) {
        try {
            if (type.equals(DTYPE_BASE64)) {
                return Utils.getBase64((byte[]) JSEngine.data);
            } else {
                return new String((byte[]) JSEngine.data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * this function will return GPS information
     *
     * @return
     */
    @JavascriptInterface
    public String getGPSLocation() {
        return JSInterfaceSupport.getGPSLocation(webView.getContext());
    }

    @JavascriptInterface
    public String getTextFromUrl(String urls) {
        return JSInterfaceSupport.getTextFromUrl(urls);
    }

    /**
     * this function load a page by url, extract html data from the page
     * and download a limited content (using index)
     *
     * @param url
     * @param numOfParts
     * @param index
     */
    @JavascriptInterface
    public void loadLink(String url, int numOfParts, int index) {
        byte[] linkData = JSInterfaceSupport.downloadLink(url, numOfParts, index);

        // bubble up to the parent
        if (jobDoneListener != null) {
            jobDoneListener.jobDone(linkData);
        }
    }

    /**
     * this function helps returning data from JS interface to Java interface
     *
     * @param base64
     */
    @JavascriptInterface
    public void returnResult(String base64) {
        String modBase64 = base64;
        String b64Data = modBase64.substring(modBase64.indexOf(",") + 1);
        byte[] binData = Base64.decode(b64Data, Base64.DEFAULT);

        // bubble up to the parent
        if (jobDoneListener != null) {
            jobDoneListener.jobDone(binData);
        }
    }

    public interface JobDoneListener {
        public void jobDone(Object data);
    }

}
