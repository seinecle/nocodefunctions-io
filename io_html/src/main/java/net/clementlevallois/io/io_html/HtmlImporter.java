package net.clementlevallois.io.io_html;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import static java.util.stream.Collectors.toSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.scheduler.BloomFilterDuplicateRemover;
import us.codecraft.webmagic.scheduler.QueueScheduler;

/**
 *
 * @author LEVALLOIS
 */
public class HtmlImporter {

    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64), this is a crawler, AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36";

    public static void main(String[] args) throws IOException {
        String urlExample = "https://viewfindr.net/";
        HtmlImporter importer = new HtmlImporter();
        String rawText = importer.importHtmlPageToSimpleLines(urlExample);
        System.out.println("raw text:");
        System.out.println(rawText);

    }

    public String importHtmlPageToSimpleLines(String urlParam) {

        urlParam = urlParam.trim();

        if (!urlParam.startsWith("http")) {
            urlParam = "https://" + urlParam;
        }
        String text = "";
        try {
            Document doc = Jsoup
                    .connect(urlParam)
                    .userAgent(userAgent).get();

            // Remove unwanted elements by selectors, including all <a> tags
            doc.select("div.advertisement, footer, .sidebar").remove();

            doc.select("*[class*='menu'], *[class*='logo'], *[class*='-toc']").not("html, header, body, p").remove();

            // Add a line break after the text of every element
            addLineBreaksToAllElements(doc);

            // After removing unwanted elements, get the text of the remaining document
            text = doc.wholeText();

            while (text.contains("  ")) {
                text = text.replace("  ", " ");
            }
            text = text.replaceAll("\r", "\n");
            text = text.replaceAll("\t", "\n");
            while (text.contains("\n\n")) {
                text = text.replace("\n\n", "\n");
            }

        } catch (IOException e) {
            System.out.println("error in html import function when accessing " + urlParam);
        }
        return text;
    }

    public String importHtmlPageToListOfUrls(String urlParam) {
        if (!urlParam.startsWith("http")) {
            urlParam = "https://" + urlParam;
        }
        if (urlParam.endsWith("/")) {
            urlParam = urlParam.substring(0, urlParam.length() - 1);
        }

        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        try {
            Document doc = Jsoup
                    .connect(urlParam)
                    .userAgent(userAgent)
                    .get();

            // Select all links within the document
            Elements links = doc.select("a[href]");

            // Iterate over all links and print their absolute URLs
            for (Element link : links) {
                String linkHref = link.attr("abs:href");
                String linkText = link.text(); // Get the text of the link
                JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                jsonObjectBuilder.add("linkHref", linkHref);
                jsonObjectBuilder.add("linkText", linkText);
                jsonArrayBuilder.add(jsonObjectBuilder);
            }

        } catch (IOException e) {
            System.out.println("error in html import function when accessing " + urlParam);
        }
        return jsonArrayBuilder.build().toString();
    }

    public String importWebsiteToListOfUrls(String domaineName, int maxUrl, Set<String> exclusionTerms) {
        exclusionTerms = exclusionTerms.stream().filter(term -> term != null && !term.isBlank()).collect(toSet());
        Set<String> urls = new HashSet();
        if (!domaineName.startsWith("http")) {
            domaineName = "https://" + domaineName;
        }
        try {
            SimpleWebCrawler crawler = new SimpleWebCrawler(domaineName, exclusionTerms, maxUrl);
            int processors = Runtime.getRuntime().availableProcessors();
            int threads = Math.max(1, processors * 2 - 2);
            Spider spider = Spider.create(crawler)
                    .addUrl(domaineName)
                    .addUrl(domaineName + "/sitemap.xml")
                    .thread(threads)
                    .setScheduler(new QueueScheduler()
                            .setDuplicateRemover(new BloomFilterDuplicateRemover(10000000))); // Ensure breadth-first search

            crawler.setSpider(spider);
            spider.run();

            urls.addAll(crawler.getUrls());
        } catch (Exception e) {
            System.out.println("error with the crawler:");
            e.printStackTrace();
        }
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        // Iterate over all links and print their absolute URLs
        int i = 0;
        for (String url : urls) {
            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("linkHref", url);
            jsonObjectBuilder.add("linkText", String.valueOf(i++));
            jsonArrayBuilder.add(jsonObjectBuilder);
        }
        return jsonArrayBuilder.build().toString();
    }

    private static void addLineBreaksToAllElements(Document doc) {
        Elements elements = doc.getAllElements();
        for (Element element : elements) {
            element.appendText("\n");
        }
    }

}
