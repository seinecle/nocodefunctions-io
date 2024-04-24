/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.io.io_html;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.QueueScheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import us.codecraft.webmagic.scheduler.BloomFilterDuplicateRemover;

public class SimpleWebCrawler implements PageProcessor {

    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64), this is a crawler, AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36";
    private final Site site = Site.me().setRetryTimes(3).setSleepTime(100).setUseGzip(true).setUserAgent(userAgent);
    private final Set<String> urls = new HashSet();
    private final String domain;
    private final Set<String> exclusionTerms;
    private final int maxUrls;
    private int count = 0;
    private Spider spider;

    public SimpleWebCrawler(String domain, Set<String> exclusionTerms, int maxUrls) {
        this.domain = domain;
        this.exclusionTerms = exclusionTerms;
        this.maxUrls = maxUrls;
    }

    @Override
    public void process(Page page) {
        // Namespace for XML sitemap index
        String namespace = "http://www.sitemaps.org/schemas/sitemap/0.9";

        if (page.getUrl().regex(".*sitemap.*\\.xml$").match()) {
            // Process sitemap.xml or any similar XML file (like page-sitemap.xml, post-sitemap.xml)
            List<String> sitemapUrls = page.getHtml().links().all();
            if (sitemapUrls.isEmpty()) {
                // Alternative XPath to handle different sitemap structures
                sitemapUrls = page.getHtml().xpath("//url/loc/text()").all();
            }
            if (sitemapUrls.isEmpty()) {
                // Alternative XPath to handle different sitemap structures
                sitemapUrls = page.getHtml().xpath("//sitemapindex/sitemap/loc/text()").all();
            }
            if (sitemapUrls.isEmpty()) {
                // Check for alternative structure or incorrect namespace handling
                sitemapUrls = page.getHtml().xpath("//*[local-name()='sitemap']/*[local-name()='loc']/text()").all();
            }
            for (String url : sitemapUrls) {
                if (url.matches(".*sitemap.*\\.xml$")) {
                    // If the URL is another sitemap, add it to the spider's queue for further processing
                    spider.addUrl(url);
                } else {
                    // If the URL is a regular target URL, process it as usual
                    addUrlToProcess(url);
                }
            }
        } else {
            // Process normal pages
            Set<String> allLinks = new HashSet(page.getHtml().links().all());
            String regex = ".*\\.(jpg|jpeg|png|gif|bmp|tiff|tif|svg|webp|heic|jif|jpe|jfif|pdf)";

            Predicate<String> isExcluded = url -> {
                if (url == null || !url.contains(domain) || url.matches(regex)) {
                    return true;
                }
                // Check if any non-blank term from exclusionTerms is contained in the URL
                for (String term : exclusionTerms) {
                    if (url.contains(term)) {
                        return true; // If a term is found, the URL should be excluded
                    }
                }
                return false; // If no terms are found, the URL is not excluded
            };
            Set<String> urlsAfterFilter = new HashSet<>();
            for (String url : allLinks) {
                // If the URL doesn't meet the exclusion criteria, add it to the set
                if (!isExcluded.test(url)) {
                    urlsAfterFilter.add(url); // Add to the set if the predicate is false
                }
            }

            for (String url : urlsAfterFilter) {
                addUrlToProcess(url);
            }
        }
    }

    private void addUrlToProcess(String url) {
        if (count < maxUrls && urls.add(url)) {
            count++;
            spider.addUrl(url); // Queue next URL for the spider to process
        }
        if (count >= maxUrls) {
            spider.stop(); // Stop the spider when max URLs count is reached
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        String domain = "http://www.clementlevallois.net";
        Set<String> exclusionTerms = Set.of("train", "exclude2");
        int maxUrls = 10; // Maximum number of URLs to collect
        SimpleWebCrawler crawler = new SimpleWebCrawler(domain, exclusionTerms, maxUrls);
        Spider spider = Spider.create(crawler)
                .addUrl(domain)
                .addUrl(domain + "/sitemap.xml")
                .thread(5) // Running multiple threads in parallel
                .setScheduler(new QueueScheduler()
                        .setDuplicateRemover(new BloomFilterDuplicateRemover(10000000))); // Ensure breadth-first search
        crawler.setSpider(spider);
        spider.run();
        crawler.getUrls().forEach(System.out::println);
    }

    public void setSpider(Spider spider) {
        this.spider = spider;
    }

    public Set<String> getUrls() {
        return urls;
    }

}
