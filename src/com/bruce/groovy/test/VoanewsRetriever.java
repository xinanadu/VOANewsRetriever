package com.bruce.groovy.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileReader;
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
    private PrintWriter writer;
    private final String ARTICLE_ID_PREFIX = "BRUCE_NEW_ARTICLE_ID_";
    private final String WROK_DIRECTORY = "D:\\Bruce\\news\\";
    private final String TEMP_ARTILES_FILE = "D:\\Bruce\\news\\temp.html";
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

        readCataloguesAndList(docHome);

        try {
            writer = new PrintWriter(TEMP_ARTILES_FILE, "UTF-8");
            readAndWriteArticles();
            writer.close();


            writer = new PrintWriter(WROK_DIRECTORY + "VOANews_" + today + ".html", "UTF-8");
            writer.append("<!DOCTYPE html>");
            writer.append("<html>");
            writer.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\">");
            writer.append("<head>");
            writer.append("<title>" + today + "</title>");
            writer.append("<style>");
            writer.append("#catalogue ul li{");
            writer.append("line-height:2em;");
            writer.append("list-style-type: decimal-leading-zero;");
            writer.append("}");
            writer.append("#catalogue ul li a:link{");
            writer.append("text-decoration: none;");
            writer.append("}");
            writer.append("</style>");
            writer.append("</head>");
            writer.append("<body>");
            writer.append("<div id=\"catalogue\">");

            writeCatalogues();

            writer.append("</div>");

            copyArticles();

            writer.append("</body>");
            writer.append("</html>");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long interval = (System.currentTimeMillis() - timeBegin) / 1000;
        System.out.println("spent time: " + (interval / 60) + "m" + (interval % 60) + "s");
    }

    private void readCataloguesAndList(Document docHome) {
        Elements navLinks = docHome.select("a.nav_link");
        for (Element eleCatalogueNav : navLinks) {
            System.out.println("readCatalogue: " + eleCatalogueNav.html());
            try {
                long beginTime = System.currentTimeMillis();
                Document doc = getJsonDocument((PREFIX + eleCatalogueNav.attr("href")), CHARSET);
                System.out.println("getting time: " + (System.currentTimeMillis() - beginTime));
                beginTime = System.currentTimeMillis();
                if (doc == null) return;

                int countListNews = listNews.size();
                Elements links = doc.select("a");
                System.out.println("parsing time: " + (System.currentTimeMillis() - beginTime));
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
        String tempCatalogue = null;
        for (int i = 0; i < listNews.size(); i++) {
            System.out.print(".");
            NewsArticle article = listNews.get(i);
            if (!article.catalogue.equals(tempCatalogue)) {
                if (tempCatalogue != null)
                    writer.append("</ul>");

                tempCatalogue = article.catalogue;
                writer.append("<h2>" + tempCatalogue + "</h2>");
                writer.append("<ul>");
            }

            writer.append("<li>" + article.title + "   <a href=\"#" + (ARTICLE_ID_PREFIX + i) + "\">view</a></li>");

        }
        if (tempCatalogue != null)
            writer.append("</ul>");

        System.out.println();
    }

    private void readAndWriteArticles() {
        for (int i = 0; i < listNews.size(); i++) {
            NewsArticle article = listNews.get(i);
            System.out.println(i + "/" + listNews.size() + " readAndWriteArticle: " + article.href);

            int countException = 0;
            boolean done = false;
            while (!done && countException < 3) {
                if (countException > 0)
                    System.err.println("  try " + (countException + 1) + " times");
                try {
                    long beginTime = System.currentTimeMillis();
                    Document docNewsArticle = getJsonDocument(PREFIX + article.href, CHARSET);
                    int etaSeconds = (int) ((listNews.size() - i) * (System.currentTimeMillis() - beginTime) / 1000);
                    System.out.println("ETA: " + (etaSeconds / 60) + "m" + etaSeconds % 60 + "s");

                    if (docNewsArticle == null) return;

                    Elements eleTitle = docNewsArticle.select("title");
                    article.title = eleTitle.html();
                    listNews.set(i, article);

                    writer.append("<h3 id=\"" + (ARTICLE_ID_PREFIX + i) + "\">" + eleTitle.html() + "</h3>");
                    Elements eleParagraphs = docNewsArticle.select("div.zoomMe p");
                    for (Element ele : eleParagraphs) {
                        String paragraph = "<p>" + ele.html() + "</p>";
                        writer.append(paragraph);
                    }
                    writer.append("<a href=\"#\" onclick=\"history.go(-1)\">back to list</a>");
                    writer.append("<hr />");
                    done = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    countException++;
                }
            }
        }
    }

    private void copyArticles() {
        try {
            FileReader fileReader =
                    new FileReader(TEMP_ARTILES_FILE);
            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader =
                    new BufferedReader(fileReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                writer.append(line);
            }
            // Always close files.
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        new VoanewsRetriever().run();
    }

}
