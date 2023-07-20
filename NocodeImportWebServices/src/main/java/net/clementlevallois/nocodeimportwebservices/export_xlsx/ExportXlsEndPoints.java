/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.nocodeimportwebservices.export_xlsx;

import io.javalin.Javalin;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.clementlevallois.functions.model.Occurrence;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.io.xlsx.ExcelSaver;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;
import net.clementlevallois.umigon.model.Document;
import net.clementlevallois.utils.Multiset;

/**
 *
 * @author LEVALLOIS
 */
public class ExportXlsEndPoints {

    public static void main(String[] args) {
    }

    public static Javalin addAll(Javalin app) throws Exception {

        app.post("/api/export/xlsx/umigon", ctx -> {
            increment();

            String lang = ctx.queryParam("lang");

            byte[] bodyAsBytes = ctx.bodyAsBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bodyAsBytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            List<Document> documents = (List<Document>) ois.readObject();

            byte[] exportUmigon = ExcelSaver.exportUmigon(documents, lang);

            ctx.result(exportUmigon).status(HttpURLConnection.HTTP_OK);
        });

        app.post("/api/export/xlsx/pdf_region_extractor", ctx -> {
            increment();

            byte[] bodyAsBytes = ctx.bodyAsBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bodyAsBytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Map<String,SheetModel> documents = (Map<String,SheetModel>) ois.readObject();

            byte[] exportPdfRegionExtractor = ExcelSaver.exportPdfRegionExtractor(documents);

            ctx.result(exportPdfRegionExtractor).status(HttpURLConnection.HTTP_OK);
        });

        app.post("/api/export/xlsx/organic", ctx -> {
            increment();

            String lang = ctx.queryParam("lang");

            byte[] bodyAsBytes = ctx.bodyAsBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bodyAsBytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            List<Document> documents = (List<Document>) ois.readObject();

            byte[] exportOrganic = ExcelSaver.exportOrganic(documents, lang);

            ctx.result(exportOrganic).status(HttpURLConnection.HTTP_OK);
        });

        app.post("/api/export/xlsx/topics", ctx -> {
            increment();

            int nbTerms = Integer.parseInt(ctx.queryParam("nbTerms"));
            Map<Integer, Multiset<String>> keywordsPerTopic = new TreeMap();
            Map<Integer, Multiset<Integer>> topicsPerLine = new TreeMap();

            byte[] bodyAsBytes = ctx.bodyAsBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bodyAsBytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            String jsonResultAsString = (String) ois.readObject();
            JsonReader jsonReader = Json.createReader(new StringReader(jsonResultAsString));
            JsonObject jsonObject = jsonReader.readObject();
            JsonObject keywordsPerTopicAsJson = jsonObject.getJsonObject("keywordsPerTopic");
            for (String keyCommunity : keywordsPerTopicAsJson.keySet()) {
                JsonObject termsAndFrequenciesForThisCommunity = keywordsPerTopicAsJson.getJsonObject(keyCommunity);
                Iterator<String> iteratorTerms = termsAndFrequenciesForThisCommunity.keySet().iterator();
                Multiset<String> termsAndFreqs = new Multiset();
                while (iteratorTerms.hasNext()) {
                    String nextTerm = iteratorTerms.next();
                    termsAndFreqs.addSeveral(nextTerm, termsAndFrequenciesForThisCommunity.getInt(nextTerm));
                }
                keywordsPerTopic.put(Integer.valueOf(keyCommunity), termsAndFreqs);
            }
            JsonObject topicsPerLineAsJson = jsonObject.getJsonObject("topicsPerLine");
            for (String lineNumber : topicsPerLineAsJson.keySet()) {
                JsonObject topicsAndTheirCountsForOneLine = topicsPerLineAsJson.getJsonObject(lineNumber);
                Iterator<String> iteratorTopics = topicsAndTheirCountsForOneLine.keySet().iterator();
                Multiset<Integer> topicsAndFreqs = new Multiset();
                while (iteratorTopics.hasNext()) {
                    String nextTopic = iteratorTopics.next();
                    topicsAndFreqs.addSeveral(Integer.valueOf(nextTopic), topicsAndTheirCountsForOneLine.getInt(nextTopic));
                }
                topicsPerLine.put(Integer.valueOf(lineNumber), topicsAndFreqs);
            }

            byte[] exportUmigon = ExcelSaver.exportTopics(keywordsPerTopic, topicsPerLine, nbTerms);

            ctx.result(exportUmigon).status(HttpURLConnection.HTTP_OK);
        });

        app.post("/api/export/xlsx/highlighter", ctx -> {
            increment();
            byte[] bodyAsBytes = ctx.bodyAsBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bodyAsBytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            List<String[]> results = (List<String[]>) ois.readObject();

            byte[] exportHighlights = ExcelSaver.exportHighlighted(results);

            ctx.result(exportHighlights).status(HttpURLConnection.HTTP_OK);
        });

        app.post("/api/export/xlsx/pdfmatches", ctx -> {
            increment();
            byte[] bodyAsBytes = ctx.bodyAsBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bodyAsBytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Map<String, List<Occurrence>> results = (Map<String, List<Occurrence>>) ois.readObject();

            byte[] exportPdfMatches = ExcelSaver.exportPdfMatcher(results);

            ctx.result(exportPdfMatches).status(HttpURLConnection.HTTP_OK);
        });

        return app;
    }

}
