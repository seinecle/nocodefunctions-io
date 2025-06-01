/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.nocodeimportwebservices.import_pdf;

import io.javalin.Javalin;
import io.javalin.http.util.NaiveRateLimit;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.clementlevallois.importers.import_pdf.controller.PdfImporter;
import net.clementlevallois.importers.import_pdf.controller.PdfExtractorByRegion;
import net.clementlevallois.importers.import_pdf.controller.PdfToPngConverter;
import net.clementlevallois.importers.model.CellRecord;
import net.clementlevallois.importers.model.ImagesPerFile;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.nocodeimportwebservices.APIController;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;
import net.clementlevallois.nocodeimportwebservices.SynchronizedFileWrite;

/**
 *
 * @author LEVALLOIS
 */
public class ImportPdfEndPoints {

    public static void main(String[] args) {
    }

    public static Javalin addAll(Javalin app) throws Exception {

        app.post("/api/import/pdf", ctx -> {
            increment();
            byte[] bodyAsBytes = ctx.bodyAsBytes();

            String fileName = ctx.queryParam("fileName");
            String localizedEmptyLineMessage = ctx.queryParam("localizedEmptyLineMessage");

            byte[] byteArray;
            try (InputStream is = new ByteArrayInputStream(bodyAsBytes)) {
                PdfImporter pdfImporter = new PdfImporter();
                List<SheetModel> sheets = pdfImporter.importPdfFile(is, fileName, localizedEmptyLineMessage);
                byteArray = APIController.byteArraySerializerForSheets(sheets);
            }

            ctx.result(byteArray).status(HttpURLConnection.HTTP_OK);
        });

        app.get("/api/import/pdf/simpleLines", ctx -> {
            increment();
            String fileName = ctx.queryParam("fileName");
            String uniqueFileId = ctx.queryParam("uniqueFileId");
            String jobId = ctx.queryParam("jobId");

            Path tempDataPath = APIController.tempFilesFolder.resolve(jobId).resolve(jobId + uniqueFileId);
            if (Files.exists(tempDataPath)) {
                try (InputStream is = new FileInputStream(tempDataPath.toFile())) {
                    PdfImporter pdfImporter = new PdfImporter();
                    List<SheetModel> sheets = pdfImporter.importPdfFile(is, fileName, "");
                    StringBuilder sb = new StringBuilder();
                    for (SheetModel sm : sheets) {
                        List<CellRecord> cellRecords = sm.getColumnIndexToCellRecords().get(0);
                        if (cellRecords == null || cellRecords.isEmpty()) {
                            ctx.result("reading the pdf file returned no text: app error or wrong file?").status(HttpURLConnection.HTTP_BAD_REQUEST);
                        } else {
                            for (CellRecord cr : cellRecords) {
                                String line = cr.getRawValue();
                                if (line != null && !line.isBlank() && line.trim().contains(" ")) {
                                    sb.append(cr.getRawValue()).append("\n");
                                }
                            }
                        }
                    }
                    Path fullPathForFileContainingTextInput = Path.of(APIController.tempFilesFolder.toString(), jobId, jobId);
                    if (Files.notExists(fullPathForFileContainingTextInput)) {
                        Files.createFile(fullPathForFileContainingTextInput);
                    }
                    SynchronizedFileWrite.concurrentWriting(fullPathForFileContainingTextInput, sb.toString());
                }
                Files.deleteIfExists(tempDataPath);
                ctx.result("ok").status(HttpURLConnection.HTTP_OK);
            } else {
                ctx.result("pdf import simple lines API endpoint: file not found").status(HttpURLConnection.HTTP_BAD_REQUEST);
            }

        });

        app.post("/api/import/pdf/return-png", ctx -> {
            increment();
            byte[] bodyAsBytes = ctx.bodyAsBytes();

            String fileName = ctx.queryParam("fileName");
            byte[] byteArraySerializerForAnyObject;
            try (InputStream is = new ByteArrayInputStream(bodyAsBytes)) {
                PdfToPngConverter pdfToPngConverter = new PdfToPngConverter();
                byte[][] images = pdfToPngConverter.convertPdfFileToPngs(is);
                ImagesPerFile ipf = new ImagesPerFile();
                ipf.setImages(images);
                ipf.setFileName(fileName);
                byteArraySerializerForAnyObject = APIController.byteArraySerializerForAnyObject(ipf);
            }
            ctx.result(byteArraySerializerForAnyObject).status(HttpURLConnection.HTTP_OK);
        });

        app.post("/api/import/pdf/extract-region", ctx -> {
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            String owner = ctx.queryParam("owner");
            if (owner == null || !owner.equals(APIController.pwdOwner)) {
                NaiveRateLimit.requestPerTimeUnit(ctx, 50, TimeUnit.SECONDS);
                increment();
            }
            byte[] pdfAsBytes = null;
            String fileName = null;
            boolean allPages = false;
            Integer selectedPage = null;
            Float leftCornerX = null;
            Float leftCornerY = null;
            Float width = null;
            Float height = null;
            byte[] bodyAsBytes = ctx.bodyAsBytes();
            String body = new String(bodyAsBytes, StandardCharsets.US_ASCII);
            if (body.isEmpty()) {
                objectBuilder.add("-99", "body of the request should not be empty");
                JsonObject jsonObject = objectBuilder.build();
                ctx.result(jsonObject.toString()).status(HttpURLConnection.HTTP_BAD_REQUEST);
            } else {
                JsonReader jsonReader = Json.createReader(new StringReader(body));
                JsonObject jsonObject = jsonReader.readObject();
                for (String nextKey : jsonObject.keySet()) {
                    if (nextKey.equals("pdfBytes")) {
                        pdfAsBytes = Base64.getDecoder().decode(jsonObject.getString(nextKey));
                    }
                    if (nextKey.equals("fileName")) {
                        fileName = jsonObject.getString(nextKey);
                    }
                    if (nextKey.equals("allPages")) {
                        allPages = jsonObject.getBoolean(nextKey);
                    }
                    if (nextKey.equals("selectedPage")) {
                        selectedPage = jsonObject.getInt(nextKey);
                    }
                    if (nextKey.equals("leftCornerX")) {
                        leftCornerX = jsonObject.getJsonNumber(nextKey).bigDecimalValue().floatValue();
                    }
                    if (nextKey.equals("leftCornerY")) {
                        leftCornerY = jsonObject.getJsonNumber(nextKey).bigDecimalValue().floatValue();
                    }
                    if (nextKey.equals("width")) {
                        width = jsonObject.getJsonNumber(nextKey).bigDecimalValue().floatValue();
                    }
                    if (nextKey.equals("height")) {
                        height = jsonObject.getJsonNumber(nextKey).bigDecimalValue().floatValue();
                    }
                }

                if (pdfAsBytes == null) {
                    objectBuilder.add("-99", "no pdf in the payload");
                    jsonObject = objectBuilder.build();
                    ctx.result(jsonObject.toString()).status(HttpURLConnection.HTTP_BAD_REQUEST);
                }
                byte[] byteArray;
                try (InputStream stream = new ByteArrayInputStream(pdfAsBytes)) {
                    PdfExtractorByRegion pibr = new PdfExtractorByRegion();
                    SheetModel data = pibr.extractTextFromRegionInPdf(stream, fileName, allPages, selectedPage, leftCornerX, leftCornerY, width, height);
                    byteArray = APIController.byteArraySerializerForAnyObject(data);
                }

                ctx.result(byteArray).status(HttpURLConnection.HTTP_OK);
            }
        });
        return app;
    }
}
