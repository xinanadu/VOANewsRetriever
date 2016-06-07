package com.bruce.groovy.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by yufei2.du on 2016/5/30.
 */
public class VoanewsRetriever {
    private final String PREFIX = "http://www.voanews.com";
    private final String CHARSET = "UTF-8";
    private String today;
    private File dirToday;

    private Document getJsonDocument(String url, String charset) {
        try {
            return Jsoup.parse(new URL(url).openStream(), charset, url);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void run() {
        Calendar cal = Calendar.getInstance();
        today = cal.get(Calendar.YEAR) + "_" + String.format("%02d", cal.get(Calendar.MONTH) + 1) + "_" + String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));

        dirToday = new File("D:\\Bruce\\news\\VOANews_" + today);

        Document docHome = getJsonDocument("http://www.voanews.com", CHARSET);
        if (docHome == null) return;

        Elements navLinks = docHome.select("a.nav_link");
        for (Element eleNavLink : navLinks) {
            readCateloge(eleNavLink.html(), eleNavLink.attr("href"));
        }
    }

    private void readCateloge(String titleCatelogue, String hrefCateloge) {
        System.out.println("readCateloge: " + titleCatelogue + ", " + hrefCateloge);
        try {
            Document doc = getJsonDocument((PREFIX + hrefCateloge), CHARSET);
            if (doc == null) return;

            Elements links = doc.select("a");
            List<String> listNews = new ArrayList<String>();
            for (Element ele : links) {
                if (ele.attr("href").contains("/content/")) {
                    String href = ele.attr("href");
                    if (!href.startsWith(PREFIX))
                        href = PREFIX + href;
                    if (!listNews.contains(href)) {
                        listNews.add(href);
                    }
                }
            }
            System.out.println("listNews.size() in " + titleCatelogue + ":" + listNews.size());
            if (!dirToday.exists()) dirToday.mkdir();

            PrintWriter writer = new PrintWriter(dirToday.getAbsolutePath() + "\\VOANews_" + today + "_" + titleCatelogue + ".html", "UTF-8");
            writer.append("<!DOCTYPE html>");
            writer.append("<html>");
            writer.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\">");
            writer.append("<head>");
            writer.append("<title>" + today + "</title>");
            writer.append("</head>");
            writer.append("<body>");
            for (int i = 0; i < listNews.size(); i++) {
                int countException = 0;
                boolean done = false;
                while (!done && countException < 3) {
                    if (countException > 0)
                        System.err.println("  try " + (countException + 1) + " times");
                    try {
                        String newsLink = listNews.get(i);
                        System.out.println(i + ", " + newsLink);

                        Document docNewsArticle = getJsonDocument(newsLink, CHARSET);
                        if (docNewsArticle == null) return;

                        Elements eleTitle = docNewsArticle.select("title");
                        writer.append("<h3>" + ((i + 1) + "/" + listNews.size() + "   ") + eleTitle.html() + "</h3>");
                        Elements eleParagraphs = docNewsArticle.select("div.zoomMe p");
                        for (Element ele : eleParagraphs) {
                            String paragraph = "<p>" + ele.html() + "</p>";
                            writer.append(paragraph);
                        }
                        writer.append("<hr />");
                        done = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        countException++;
                    }
                }
            }
            writer.append("</body>");
            writer.append("</html>");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        new VoanewsRetriever().run();

//        new VoanewsRetriever().getJsonDocument("http://blogs.voanews.com/us-opinion/", "utf-8");

    }

}
