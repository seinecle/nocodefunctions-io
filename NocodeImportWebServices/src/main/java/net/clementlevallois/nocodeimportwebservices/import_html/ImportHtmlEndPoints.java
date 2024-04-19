/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.nocodeimportwebservices.import_html;

import io.javalin.Javalin;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.clementlevallois.io.io_html.HtmlImporter;
import net.clementlevallois.nocodeimportwebservices.APIController;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;
import net.clementlevallois.nocodeimportwebservices.SynchronizedFileWrite;

/**
 *
 * @author LEVALLOIS
 */
public class ImportHtmlEndPoints {

    public static Javalin addAll(Javalin app) throws Exception {

        app.get("/api/import/html/getLinksContainedInPage", ctx -> {
            increment();
            String url = Optional.ofNullable(ctx.queryParam("url")).orElse("none");
            if (!url.isBlank() && !url.equals("none")) {
                HtmlImporter htmlImporter = new HtmlImporter();
                String simpleLines = htmlImporter.importHtmlPageToListOfUrls(url);
                ctx.result(simpleLines).status(HttpURLConnection.HTTP_OK);
            } else {
                ctx.result("html extract links from html page import API endpoint: url not included in params").status(HttpURLConnection.HTTP_BAD_REQUEST);
            }
        });

        app.get("/api/import/html/getPagesContainedInWebsite", ctx -> {
            increment();
            String url = Optional.ofNullable(ctx.queryParam("url")).orElse("none");
            int maxUrls = Integer.parseInt(Optional.ofNullable(ctx.queryParam("maxUrls")).orElse("30"));
            String commaSeparatedListOfExclusionTerms = Optional.ofNullable(ctx.queryParam("exclusionTerms")).orElse("");
            Set<String> exclusionTerms = Arrays.stream(commaSeparatedListOfExclusionTerms.split(",")).map(String::trim).collect(Collectors.toSet());
            if (!url.isBlank() && !url.equals("none")) {
                HtmlImporter htmlImporter = new HtmlImporter();
                String simpleLines = htmlImporter.importWebsiteToListOfUrls(url, maxUrls, exclusionTerms);
                ctx.result(simpleLines).status(HttpURLConnection.HTTP_OK);
            } else {
                ctx.result("html crawl pages in website import API endpoint: url not included in params").status(HttpURLConnection.HTTP_BAD_REQUEST);
            }
        });

        app.post("/api/import/html/getRawTextFromLinks", ctx -> {
            increment();
            String dataPersistenceId = ctx.queryParam("dataPersistenceId");
            byte[] bodyAsBytes = ctx.bodyAsBytes();

            JsonArray jsonArray = Json.createReader(new ByteArrayInputStream(bodyAsBytes)).readArray();

            if (dataPersistenceId != null) {
                Path fullPathForFileContainingTextInput = Path.of(APIController.tempFilesFolder.toString(), dataPersistenceId);
                StringBuilder sb = new StringBuilder();
                HtmlImporter htmlImporter = new HtmlImporter();
                for (JsonObject jsonObject : jsonArray.getValuesAs(JsonObject.class)) {
                    // Process each JsonObject
                    String simpleLines = htmlImporter.importHtmlPageToSimpleLines(jsonObject.keySet().iterator().next());
                    sb.append(simpleLines);
                    sb.append(System.lineSeparator());
                }
                if (Files.notExists(fullPathForFileContainingTextInput)) {
                    Files.createFile(fullPathForFileContainingTextInput);
                }
                SynchronizedFileWrite.concurrentWriting(fullPathForFileContainingTextInput, sb.toString());
                ctx.result("ok").status(HttpURLConnection.HTTP_OK);
            } else {
                ctx.result("html import simple lines import API endpoint: file not found").status(HttpURLConnection.HTTP_BAD_REQUEST);
            }
        });

        return app;
    }
}
