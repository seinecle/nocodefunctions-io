/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.nocodeimportwebservices.export_xlsx;

import io.javalin.Javalin;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import net.clementlevallois.functions.model.Occurrence;
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

        app.post("/api/export/xlsx/topics", ctx -> {
            increment();

            int nbTerms = Integer.parseInt(ctx.queryParam("nbTerms"));

            byte[] bodyAsBytes = ctx.bodyAsBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bodyAsBytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Map<Integer, Multiset<String>> communitiesResult = (Map<Integer, Multiset<String>>) ois.readObject();

            byte[] exportUmigon = ExcelSaver.exportTopics(communitiesResult, nbTerms);

            ctx.result(exportUmigon).status(HttpURLConnection.HTTP_OK);
        });

        app.post("/api/export/xlsx/organic", ctx -> {
            increment();
            String lang = ctx.pathParam("lang");
            byte[] bodyAsBytes = ctx.bodyAsBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bodyAsBytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            List<Document> documents = (List<Document>) ois.readObject();

            byte[] exportOrganic = ExcelSaver.exportOrganic(documents, lang);

            ctx.result(exportOrganic).status(HttpURLConnection.HTTP_OK);
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
