/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.io.io_html.tests;

import java.util.HashSet;
import java.util.Set;
import net.clementlevallois.io.io_html.HtmlImporter;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 *
 * @author LEVALLOIS
 */
public class GeneralTest {

    @Test
    public void singlePageImport() {
        String urlExample = "https://viewfindr.net/";
        HtmlImporter importer = new HtmlImporter();
        String rawText = importer.importHtmlPageToSimpleLines(urlExample);
        assertThat(rawText.length()).isGreaterThan(50);
    }

    @Test
    public void websiteImport() {
        String urlExample = "https://viewfindr.net/";
        HtmlImporter importer = new HtmlImporter();
        String rawText = importer.importWebsiteToListOfUrls(urlExample, 2, new HashSet());
        assertThat(rawText.length()).isGreaterThan(50);
    }

    @Test
    public void websiteImportExcludingKeywords() {
        String urlExample = "https://viewfindr.net/de/";
        Set<String> toExclude = Set.of("photo");
        HtmlImporter importer = new HtmlImporter();
        String rawText = importer.importWebsiteToListOfUrls(urlExample, 2, toExclude);
        assertThat(rawText.length()).isGreaterThan(50);
    }

}
