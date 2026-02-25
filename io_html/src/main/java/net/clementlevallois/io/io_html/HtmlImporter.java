package net.clementlevallois.io.io_html;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public static void main(String[] args) throws Exception {
        String urlExample = "https://viewfindr.net/";
        HtmlImporter importer = new HtmlImporter();
        String rawText = importer.importHtmlPageToSimpleLines(urlExample);
        System.out.println("raw text:");
        System.out.println(rawText);

    }

    public String importHtmlPageToSimpleLines(String urlParam) throws Exception {

        urlParam = urlParam.trim();

        if (!urlParam.startsWith("http")) {
            urlParam = "https://" + urlParam;
        }

        String htmlContent;

        // First try with JSoup
        try {
            Document doc = Jsoup.connect(urlParam)
                    .userAgent(userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Cache-Control", "max-age=0")
                    .header("Sec-Ch-Ua", "\"Google Chrome\";v=\"120\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"120\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(30000)
                    .followRedirects(true)
                    .ignoreHttpErrors(false)
                    .get();
            htmlContent = doc.html();
        } catch (Exception e) {
            // If JSoup fails with HTTP error (like 403), try with curl
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            System.out.println("JSoup error for " + urlParam + ": " + e.getClass().getName() + " - " + errorMsg);
            if (errorMsg.contains("Status=403") || errorMsg.contains("Status=406") || e.getClass().getSimpleName().equals("HttpStatusException")) {
                System.out.println("Attempting curl fallback for: " + urlParam);
                htmlContent = fetchWithNativeClient(urlParam);
            } else {
                throw e;
            }
        }

        // Parse the HTML content with JSoup
        Document doc = Jsoup.parse(htmlContent, urlParam);

        // Remove unwanted elements by selectors
        doc.select("div.advertisement, footer, .sidebar").remove();

        doc.select("*[class*='menu'], *[class*='logo'], *[class*='-toc']").not("html, header, body, p").remove();

        // Add a line break after the text of every element
        addLineBreaksToAllElements(doc);

        // After removing unwanted elements, get the text of the remaining document
        String text = doc.wholeText();

        while (text.contains("  ")) {
            text = text.replace("  ", " ");
        }
        text = text.replaceAll("\r", "\n");
        text = text.replaceAll("\t", "\n");
        while (text.contains("\n\n")) {
            text = text.replace("\n\n", "\n");
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
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9,fr;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(30000)
                    .followRedirects(true)
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
        // Remove trailing slash to avoid double slashes when appending paths
        if (domaineName.endsWith("/")) {
            domaineName = domaineName.substring(0, domaineName.length() - 1);
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

    /**
     * Fetch HTML content using system curl command.
     * This is used as a fallback when JSoup is blocked by anti-bot measures.
     * Using curl because some sites use TLS fingerprinting to block Java clients.
     */
    private String fetchWithNativeClient(String urlParam) throws Exception {
        System.out.println("Attempting to fetch with curl: " + urlParam);

        ProcessBuilder pb = new ProcessBuilder(
                "curl", "-sL",
                "-H", "User-Agent: " + userAgent,
                "-H", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                "-H", "Accept-Language: fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7",
                "-H", "Cache-Control: max-age=0",
                "-H", "Sec-Ch-Ua: \"Google Chrome\";v=\"120\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"120\"",
                "-H", "Sec-Ch-Ua-Mobile: ?0",
                "-H", "Sec-Ch-Ua-Platform: \"Windows\"",
                "-H", "Sec-Fetch-Dest: document",
                "-H", "Sec-Fetch-Mode: navigate",
                "-H", "Sec-Fetch-Site: none",
                "-H", "Sec-Fetch-User: ?1",
                "-H", "Upgrade-Insecure-Requests: 1",
                "--max-time", "30",
                urlParam
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String htmlContent = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        if (exitCode != 0 || htmlContent.isEmpty()) {
            throw new IOException("Curl failed with exit code " + exitCode + " for URL: " + urlParam);
        }

        System.out.println("Successfully fetched " + htmlContent.length() + " bytes with curl");
        return htmlContent;
    }

}
