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
import java.util.stream.Collectors;
import us.codecraft.webmagic.scheduler.BloomFilterDuplicateRemover;

public class SimpleWebCrawler implements PageProcessor {

    private final Site site = Site.me().setRetryTimes(3).setSleepTime(100).setUseGzip(true);
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
        List<String> targetUrlsAslist = page.getHtml().links().regex(domain + ".*\\.htm.*").all();
        Set<String> targetUrls = targetUrlsAslist.stream()
                .filter(url -> exclusionTerms.stream().filter(s -> !s.isBlank()).noneMatch(url::contains))
                .collect(Collectors.toSet());

        if (page.getUrl().regex(domain + ".*").match()) {
            if (page.getUrl().regex(".*/sitemap\\.xml").match()) {
                for (String url : targetUrls) {
                    if (count < maxUrls) {
                        if (urls.add(url)) {
                            count++;
                        }
                        page.addTargetRequests(targetUrls);
                    } else {
                        spider.stop();
                        break;
                    }
                }
            } else {
                urls.add(page.getUrl().toString());
                for (String url : targetUrls) {
                    if (count < maxUrls) {
                        if (urls.add(url)) {
                            count++;
                        }
                        page.addTargetRequests(targetUrls);
                    } else {
                        spider.stop();
                        break;
                    }
                }
            }
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
