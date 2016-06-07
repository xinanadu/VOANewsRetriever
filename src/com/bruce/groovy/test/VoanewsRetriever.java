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
    private PrintWriter writer;
    private String ARTICLE_ID_PREFIX = "BRUCE_NEW_ARTICLE_ID_";
    private List<NewsArticle> listNews = new ArrayList<NewsArticle>();

    private Document getJsonDocument(String url, String charset) {
        try {
            return Jsoup.parse(new URL(url).openStream(), charset, url);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void run() {
        long timeBegin = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        today = cal.get(Calendar.YEAR) + "_" + String.format("%02d", cal.get(Calendar.MONTH) + 1) + "_" + String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));


        Document docHome = getJsonDocument("http://www.voanews.com", CHARSET);
        if (docHome == null) return;

        readCatalogues(docHome);

        try {
            dirToday = new File("D:\\Bruce\\news\\VOANews_" + today);
            if (!dirToday.exists()) dirToday.mkdir();

            writer = new PrintWriter(dirToday.getAbsolutePath() + "\\VOANews_" + today + ".html", "UTF-8");
            writer.append("<!DOCTYPE html>");
            writer.append("<html>");
            writer.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\">");
            writer.append("<head>");
            writer.append("<title>" + today + "</title>");
            writer.append("<style>");
            writer.append("#catalogue ul li{");
            writer.append("line-height:2em;");
            writer.append("}");
            writer.append("</style>");
            writer.append("</head>");
            writer.append("<body>");
            writer.append("<div id=\"catalogue\">");

            writeCatalogues();

            readAndWriteArticles();

            writer.append("</body>");
            writer.append("</html>");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("spent time: " + (System.currentTimeMillis() - timeBegin));
    }

    private void readCatalogues(Document docHome) {
        Elements navLinks = docHome.select("a.nav_link");
        for (Element eleCatalogueNav : navLinks) {
            System.out.println("readCatalogue: " + eleCatalogueNav.html());
            try {
                long beginTime = System.currentTimeMillis();
                Document doc = getJsonDocument((PREFIX + eleCatalogueNav.attr("href")), CHARSET);
                System.out.println("getting document spent time: " + (System.currentTimeMillis() - beginTime));
                beginTime = System.currentTimeMillis();
                if (doc == null) return;

                int countListNews = listNews.size();
                Elements links = doc.select("a");
                System.out.println("parsing document spent time: " + (System.currentTimeMillis() - beginTime));
                String tempHref = "";
                for (Element ele : links) {
                    if (ele.attr("href").contains("/content/")) {
                        String href = ele.attr("href");
                        if (href.startsWith(PREFIX))
                            href = href.replace(PREFIX, "");
                        if (!tempHref.equalsIgnoreCase(href)) {
                            listNews.add(new NewsArticle(eleCatalogueNav.html(), ele.html(), href));
                            tempHref = href;
                        }
                    }
                }
                System.out.println("listNews.size() in " + eleCatalogueNav.html() + ":" + (listNews.size() - countListNews));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void writeCatalogues() {
        String tempCatalgue = null;
        for (int i = 0; i < listNews.size(); i++) {
            System.out.print(".");
            NewsArticle article = listNews.get(i);
            if (!article.catalogue.equals(tempCatalgue)) {
                if (tempCatalgue != null)
                    writer.append("</ul>");

                tempCatalgue = article.catalogue;
                writer.append("<h2>" + tempCatalgue + "</h2>");
                writer.append("<ul>");
            }

            writer.append("<li><a href=\"#" + (ARTICLE_ID_PREFIX + i) + "\">" + article.href + "</a></li>");

        }
        if (tempCatalgue != null)
            writer.append("</ul>");

        System.out.println();
    }

    private void readAndWriteArticles() {
        for (int i = 0; i < listNews.size(); i++) {
            NewsArticle article = listNews.get(i);
            System.out.println("readAndWriteArticle: " + article.href);

            int countException = 0;
            boolean done = false;
            while (!done && countException < 3) {
                if (countException > 0)
                    System.err.println("  try " + (countException + 1) + " times");
                try {
                    Document docNewsArticle = getJsonDocument(PREFIX + article.href, CHARSET);
                    if (docNewsArticle == null) return;

                    Elements eleTitle = docNewsArticle.select("title");

                    writer.append("<h3 id=\"" + (ARTICLE_ID_PREFIX + i) + "\">" + eleTitle.html() + "</h3>");
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
    }

    public static void main(String[] args) throws Exception {
        new VoanewsRetriever().run();

//        new VoanewsRetriever().getJsonDocument("http://blogs.voanews.com/us-opinion/", "utf-8");

    }

}
