/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.io.io_html;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SimpleWebCrawler implements PageProcessor {

    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64), this is a crawler, AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36";
    private final String SITEMAP_NAMESPACE = "http://www.sitemaps.org/schemas/sitemap/0.9";
    private final Site site = Site.me().setRetryTimes(3).setSleepTime(100).setUseGzip(true).setUserAgent(userAgent);
    private final Set<String> urls = Collections.synchronizedSet(new HashSet<>());
    private final String domain;
    private final Set<String> exclusionTerms;
    private final int maxUrls;
    private final AtomicInteger count = new AtomicInteger(0);
    private Spider spider;

    // Thread-local WebDriver to ensure each thread has its own instance
    private static final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();

    // Semaphore to limit concurrent WebDriver instances
    private static final Semaphore webDriverSemaphore = new Semaphore(50); // Adjust limit as needed

    public SimpleWebCrawler(String domain, Set<String> exclusionTerms, int maxUrls) {
        this.domain = domain;
        this.exclusionTerms = exclusionTerms;
        this.maxUrls = maxUrls;
    }

    private WebDriver getOrCreateWebDriver() {
        WebDriver driver = threadLocalDriver.get();
        if (driver == null) {
            try {
                // Try to acquire a permit with timeout
                if (!webDriverSemaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Timeout waiting for WebDriver semaphore");
                }

                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-gpu");

                // Add retry logic for WebDriver creation
                int maxRetries = 3;
                int retryCount = 0;
                while (retryCount < maxRetries) {
                    try {
                        driver = new ChromeDriver(options);
                        break;
                    } catch (Exception e) {
                        retryCount++;
                        if (retryCount == maxRetries) {
                            webDriverSemaphore.release();
                            throw new RuntimeException("Failed to create WebDriver after " + maxRetries + " attempts", e);
                        }
                        Thread.sleep(1000 * retryCount); // Exponential backoff
                    }
                }

                threadLocalDriver.set(driver);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while creating WebDriver", e);
            }
        }
        return driver;
    }

    private void cleanupWebDriver() {
        WebDriver driver = threadLocalDriver.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                // Log the error but don't throw
                System.err.println("Error closing WebDriver: " + e.getMessage());
            } finally {
                threadLocalDriver.remove();
                webDriverSemaphore.release();
            }
        }
    }

    @Override
    public void process(Page page) {
        try {
            if (page.getUrl().regex(".*sitemap.*\\.xml$").match()) {
                processSitemapPage(page);
            } else {
                processRegularPage(page);
            }
        } catch (Exception e) {
            System.err.println("Error processing page " + page.getUrl() + ": " + e.getMessage());
            // Optionally add the URL to a retry queue or log for manual review
        }
    }

    private void processSitemapPage(Page page) {
        List<String> sitemapUrls = new ArrayList<>();

        // Try namespace-aware XPath first
        sitemapUrls.addAll(page.getHtml()
                .xpath("//*[namespace-uri()='" + SITEMAP_NAMESPACE + "' and local-name()='loc']/text()")
                .all());

        // If no results with namespace, try without
        if (sitemapUrls.isEmpty()) {
            // Try standard links first
            sitemapUrls.addAll(page.getHtml().links().all());

            // Try various XPath patterns as fallback
            if (sitemapUrls.isEmpty()) {
                sitemapUrls.addAll(page.getHtml().xpath("//url/loc/text()").all());
            }
            if (sitemapUrls.isEmpty()) {
                sitemapUrls.addAll(page.getHtml().xpath("//sitemapindex/sitemap/loc/text()").all());
            }
            if (sitemapUrls.isEmpty()) {
                sitemapUrls.addAll(page.getHtml().xpath("//*[local-name()='sitemap']/*[local-name()='loc']/text()").all());
            }
        }

        // Filter and process URLs
        sitemapUrls.stream()
                .filter(Objects::nonNull)
                .filter(url -> !url.trim().isEmpty())
                .forEach(url -> {
                    if (url.matches(".*sitemap.*\\.xml$")) {
                        spider.addUrl(url);
                    } else {
                        addUrlToProcess(url);
                    }
                });
    }

    private void processRegularPage(Page page) {
        Set<String> allLinks = new HashSet<>(page.getHtml().links().all());

        if (allLinks.isEmpty()) {
            WebDriver driver = null;
            try {
                driver = getOrCreateWebDriver();
                driver.get(page.getRequest().getUrl());

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("nav.main_nav")));

                List<WebElement> links = driver.findElements(By.xpath("//a[@href]"));
                for (WebElement link : links) {
                    try {
                        String href = link.getAttribute("href");
                        if (href != null) {
                            allLinks.add(href);
                        }
                    } catch (Exception e) {
                        // Skip problematic links but continue processing
                        continue;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error with WebDriver for URL " + page.getRequest().getUrl() + ": " + e.getMessage());
                // Fall back to normal processing if WebDriver fails
            } finally {
                if (driver != null) {
                    cleanupWebDriver();
                }
            }
        }

        processLinks(allLinks);
    }

    private void processLinks(Set<String> links) {
        String regex = ".*\\.(jpg|jpeg|png|gif|bmp|tiff|tif|svg|webp|heic|jif|jpe|jfif|pdf|zip|exe|deb|dmg)";

        links.stream()
                .map(url -> url.startsWith("/") ? domain + url : url)
                .filter(url -> url != null && url.contains(domain) && !url.matches(regex))
                .filter(url -> exclusionTerms.stream().noneMatch(url::contains))
                .forEach(this::addUrlToProcess);
    }

    private void addUrlToProcess(String url) {
        if (count.get() < maxUrls && urls.add(url)) {
            count.incrementAndGet();
            spider.addUrl(url);
        }
        if (count.get() >= maxUrls) {
            spider.stop();
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public void setSpider(Spider spider) {
        this.spider = spider;
    }

    public Set<String> getUrls() {
        return new HashSet<>(urls); // Return a copy to prevent concurrent modification
    }
}
