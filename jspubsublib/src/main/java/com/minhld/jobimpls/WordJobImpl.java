package com.minhld.jobimpls;

import com.minhld.jobex.Job;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by minhld on 11/2/2015.
 */
public class WordJobImpl implements Job {

	public String exec(Object htmlContents) {
        String pageText = (String) htmlContents;// getTextFromUrl((String) urls);
        pageText = Jsoup.parse(pageText).text();
        pageText = pageText + pageText + pageText;
        return findTopWords(pageText, 50);
    }

    private String findTopWords(String pageText, int nTop) {
        pageText = cleanText(pageText);
        String[] words = pageText.split(" ");

        Map<String, Integer> wordCounts = new HashMap<>();

        String newKey = "";
        int count = 0;
        for (int i = 0; i < words.length; i++) {
            if (!checkMinorWords(words[i])) {
                newKey = "_" + words[i];
                count = wordCounts.containsKey(newKey) ? wordCounts.get(newKey) + 1 : 1;
                wordCounts.put(newKey, count);
            }
        }

        String[] keys = wordCounts.keySet().toArray(new String[] {});
        Map<String, Integer> topWords = new HashMap<>();

        for (int i = 0; i < nTop; i++) {
            int maxPos = 0;
            for (int j = i + 1; j < wordCounts.size(); j++) {
                if (wordCounts.get(keys[j]) > wordCounts.get(keys[maxPos])) {
                    maxPos = j;
                }
            }

            String key = keys[maxPos];
            topWords.put(key, wordCounts.get(key));

            // this will be out of the loop
            wordCounts.put(key, 0);
        }

        JSONObject json = new JSONObject();
        try {
            for (String key : topWords.keySet()) {
                json.put(key, topWords.get(key));
            }
        } catch (JSONException jsonEx) {
            jsonEx.printStackTrace();
        }
        return json.toString();
    }

    private String getTextFromUrl(String urls) {
        StringBuffer allText = new StringBuffer();
        String[] urlList = urls.split(";");
        try {

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

            //
            // return Jsoup.parse(buffer.toString()).text();
            // return android.text.Html.fromHtml(buffer.toString()).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    String[] stopWords = new String[] { "i", "im", "a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "amoungst", "amount",  "an", "and", "another", "any", "anyhow", "anyone", "anything", "anyway", "anywhere", "are", "around", "as",  "at", "back", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom", "but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "did", "didnt", "do", "done", "dont", "down", "due", "during", "each", "eg", "eight", "either", "eleven", "else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own", "part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the" };


    /**
     * remove the redundant words out of the string (like the, there, etc.)
     * @param word
     */
    private boolean checkMinorWords(String word) {
        for (int i = 0; i < stopWords.length; i++) {
            if (word.equals(stopWords[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * clean all the redundant characters out of the string
     * @param s
     */
    private String cleanText(String s) {
        return s.replaceAll("(^\\s*)|(\\s*$)", "").
                replaceAll("[ ]{2,}", " ").
                replaceAll("\n ", "\n").
                replaceAll("'", "").
                replaceAll("\"", "").
                replaceAll(",", "").
                toLowerCase();
    }
}
