package com.minhld.httpd;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import com.minhld.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;
import java.util.zip.ZipOutputStream;


/**
 * Created by minhld on 4/10/2016.
 */
public class JSInterfaceSupport {
    //region Variables

    static String result = "unknown";

    //endregion

    //region Internet Remote Functions

    /**
     * download the whole document of URL links
     * the input parameter can contain a number of URLs, each will be split
     * with others by semicolons
     *
     * @param urls
     * @return
     */
    public static String getTextFromUrl(String urls) {
        StringBuffer allText = new StringBuffer();
        String[] urlList = urls.split(";");
        try {
//            Document htmlDoc = null;
//            for (int i = 0; i < urlList.length; i++) {
//                 htmlDoc = Jsoup.connect(urlList[i]).get();
//                 allText.append(htmlDoc.text() + "\n");
//            }

            String htmlDoc = "";
            for (int i = 0; i < urlList.length; i++) {
                htmlDoc = getTextFromHttps(urlList[i]);
                allText.append(htmlDoc + "\n");
            }

            return allText.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * get data from HTTPS
     *
     * @param url
     * @return
     */
    private static String getTextFromHttps(String url) {
        try {
            // open http connection
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            //conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            //conn.setRequestProperty("Accept","*/*");
            //conn.setConnectTimeout(10000);
            //conn.connect();

            // get the page's data
            StringBuffer buffer = new StringBuffer();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            while ((line = br.readLine()) != null){
                buffer.append(line);
            }
            br.close();

            //
            //return Jsoup.parse(buffer.toString()).text();
            return android.text.Html.fromHtml(buffer.toString()).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * download a part of the whole document of an URL link
     * the part is defined by <b>numOfParts</b> and <b>index</b>
     *
     * @param url
     * @param numOfParts
     * @param index
     * @return
     */
    public static byte[] downloadLink(String url, int numOfParts, int index) {
        byte[] result = new byte[0];

        // create a zip output
        String anyFileName = UUID.randomUUID().toString() + ".zip";
        String anyPath = Utils.getDownloadPath() + "/" + anyFileName;
        ZipOutputStream zipOut = null;

        try {
            zipOut = new ZipOutputStream(new FileOutputStream(anyPath));

            HashMap<String, String> links = updateLinks(url);

            if (index == 0) {
                Utils.writeZip(zipOut, "index.html", links.get("html"));
            }
            links.remove("html");

            int linksSize = links.size();
            int step = numOfParts == 1 ? linksSize : linksSize / numOfParts + 1;
            int offs = step * index;

            // write a small piece of metadata
            Utils.writeZip(zipOut, linksSize + "_" + step + "_" + offs, "link: " + url + "\nstep: " + step + ", offset: " + offs);

            int cnt = 0;
            for (String lnkKey : links.keySet()) {
                if (cnt < offs) {
                    cnt++;
                    continue;
                }

                if (cnt < offs + step) {
                    // download the link
                    try {
                        Utils.writeZip(zipOut, lnkKey, new URL(links.get(lnkKey)).openStream());
                    } catch(Exception e) {
                        // skip one resource, no problem
                        e.printStackTrace();
                    }
                    cnt++;
                }else {
                    break;
                }
            }

            zipOut.flush();
            zipOut.close();

            result = Utils.readFile(new File(anyPath));

        } catch (IOException e) {
            e.printStackTrace();

        }

        return result;
    }

    /**
     * this function loads a web page and return data & all the related links
     * using within that page including css, js, images & other multimedia
     * resources
     *
     * @param url
     * @return
     * @throws IOException
     */
    private static HashMap<String, String> updateLinks(String url) throws IOException {
        HashMap<String, String> links = new HashMap<>();

        Document htmlDoc = Jsoup.connect(url).get();
        Elements elements = htmlDoc.getAllElements();

        String orgSrcUrl = "", srcName = "";
        for (Element e : elements) {
            //srcUrl = getSrc(e);
            if (e.attr("href") != null && !e.attr("href").isEmpty() && !e.tagName().equalsIgnoreCase("a")) {
                orgSrcUrl = e.attr("href");
                srcName = getFilename(orgSrcUrl);
                links.put(srcName, orgSrcUrl);
                htmlDoc.getElementsByAttributeValue("href", orgSrcUrl).attr("href", srcName);
            }

            if (e.attr("src") != null && !e.attr("src").isEmpty()) {
                orgSrcUrl = e.attr("src");
                srcName = getFilename(orgSrcUrl);
                links.put(srcName, orgSrcUrl);
                htmlDoc.getElementsByAttributeValue("src", orgSrcUrl).attr("src", srcName);
            }

        }

        String htmlDocStr = htmlDoc.html();
        links.put("html", htmlDocStr);

        return links;
    }

    /**
     * only get the file name of an URL
     *
     * @param src
     * @return
     */
    private static String getFilename(String src) {
        return new File(src).getName();
    }

    //endregion

    //region GPS Remote Functions

//    /**
//     * get single GPS location
//     *
//     * @param c context
//     * @return
//     */
//    public static String getGPSLocation(Context c) {
//        LocationManager locationManager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
//
//        // if GPS is disable, suggest to open the GPS dialog to enable it
//        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
//            return "GPS disabled";
//        }
//
//        LocationListener locationListener = new LocationListener() {
//            public void onLocationChanged(Location location) {
//                // when location data is available
//                result = location.getLatitude() + ", " + location.getLongitude();
//            }
//
//            public void onStatusChanged(String provider, int status, Bundle extras) {}
//
//            public void onProviderEnabled(String provider) {}
//
//            public void onProviderDisabled(String provider) {}
//        };
//
//        try {
//            // request permission for accessing GPS
//            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, Looper.getMainLooper());
//        } catch (SecurityException e) {
//            return "exception: " + e.getMessage();
//        }
//
//        // check if value is available or not
//        while (true) {
//            if (!result.equals("")) {
//                return result;
//            }
//
//            // wait for a few milliseconds
//            try {
//                Thread.sleep(100);
//            } catch(Exception e) { }
//        }
//    }

    //endregion
}
