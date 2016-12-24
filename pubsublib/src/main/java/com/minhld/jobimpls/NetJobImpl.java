package com.minhld.jobimpls;

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
 * Created by minhld on 11/2/2015.
 */
public class NetJobImpl implements Job {

	public Object exec(Object input) {
        WebPart webPart = (WebPart) input;
        byte[] result = new byte[0];

        // create a zip output
        String anyFileName = UUID.randomUUID().toString() + ".zip";
        String anyPath = Utils.getDownloadPath() + "/" + anyFileName;
        ZipOutputStream zipOut = null;

        try {
            zipOut = new ZipOutputStream(new FileOutputStream(anyPath));

            HashMap<String, String> links = updateLinks(webPart.url);

            if (webPart.firstOffset == 0) {
                writeZip(zipOut, "index.html", links.get("html"));
            }
            links.remove("html");

            int linksSize = links.size();
            // int step = webPart.numOfParts == 1 ? linksSize : linksSize / webPart.numOfParts + 1;
            int step = (int) ((float) linksSize * ((float) (webPart.lastOffset - webPart.firstOffset) / 100));
            int offs = (int) ((float) linksSize * ((float) webPart.firstOffset / 100));

            // write a small piece of metadata
            writeZip(zipOut, linksSize + "_" + step + "_" + offs, "link: " + webPart.url + "\nstep: " + step + ", offset: " + offs);

            int cnt = 0;
            for (String lnkKey : links.keySet()) {
                if (cnt < offs) {
                    cnt++;
                    continue;
                }

                if (cnt < offs + step) {
                    // download the link
                    try {
                        // writeZip(zipOut, lnkKey, new URL(links.get(lnkKey)).openStream());
                        InputStream linkStream = readUrl(links.get(lnkKey));
                        if (linkStream != null) {
                            writeZip(zipOut, lnkKey, linkStream);
                        }
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
    private HashMap<String, String> updateLinks(String url) throws IOException {
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
     * write out (string) data to a zip entry
     *
     * @param zipOut
     * @param name
     * @param data
     * @throws IOException
     */
    private void writeZip(ZipOutputStream zipOut, String name, String data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zipOut.putNextEntry(entry);
        BufferedWriter zipWriter = new BufferedWriter(new OutputStreamWriter(
                zipOut, Charset.forName("utf-8")));
        zipWriter.write(data);
        zipWriter.flush();

        zipOut.closeEntry();
    }

    /**
     * write out binary data to a zip entry
     * @param zipOut
     * @param name
     * @param in
     * @throws IOException
     */
    private void writeZip(ZipOutputStream zipOut, String name, InputStream in) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zipOut.putNextEntry(entry);

        byte[] buffer = new byte[2048];
        int length = 0;
        while((length = in.read(buffer)) > 0) {
            zipOut.write(buffer, 0, length);
        }

        zipOut.closeEntry();
    }

    /**
     * only get the file name of an URL
     *
     * @param src
     * @return
     */
    private String getFilename(String src) {
        return new File(src).getName();
    }

    private InputStream readUrl(String url) throws Exception {
        try {
        URLConnection urlConn = new URL(url).openConnection();
        urlConn.setReadTimeout(3000);   // no more than 3s of tryout
        urlConn.setConnectTimeout(3000);
        return urlConn.getInputStream();
        } catch (Exception e) {
            System.err.println(url + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Created by minhld on 1/5/2016.
     */
    public static class WebPart implements Serializable {
        public String url;
        public int firstOffset;
        public int lastOffset;

        public WebPart() {}
    }
}
