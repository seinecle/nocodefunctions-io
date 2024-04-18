/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.nocodeimportwebservices.import_txt;

import io.javalin.Javalin;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.clementlevallois.importers.import_txt.controller.TxtImporter;
import net.clementlevallois.importers.model.CellRecord;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.nocodeimportwebservices.APIController;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;
import net.clementlevallois.nocodeimportwebservices.SynchronizedFileWrite;

/**
 *
 * @author LEVALLOIS
 */
public class ImportTxtEndPoints {

    public static void main(String[] args) {
    }

    public static Javalin addAll(Javalin app) throws Exception {

        app.post("/api/import/txt", ctx -> {
            increment();
            byte[] bodyAsBytes = ctx.bodyAsBytes();

            String fileName = ctx.queryParam("fileName");
            String functionName = ctx.queryParam("functionName");
            String gazeOption = ctx.queryParam("gazeOption");

            TxtImporter txtImporter = new TxtImporter();
            List<SheetModel> sheets = txtImporter.importTextFile(bodyAsBytes, fileName, functionName, gazeOption);

            byte[] byteArray = APIController.byteArraySerializerForSheets(sheets);

            ctx.result(byteArray).status(HttpURLConnection.HTTP_OK);
        });

        app.get("/api/import/txt/simpleLines", ctx -> {
            increment();
            String fileName = ctx.queryParam("fileName");
            String dataPersistenceId = ctx.queryParam("dataPersistenceId");
            String uniqueFileId = ctx.queryParam("uniqueFileId");
            Path tempDataPath = Path.of(APIController.tempFilesFolder.toString(), dataPersistenceId + uniqueFileId);
            if (Files.exists(tempDataPath)) {
                byte[] readAllBytes = Files.readAllBytes(tempDataPath);
                String functionName = Optional.ofNullable(ctx.queryParam("functionName")).orElse("function not set");
                String gazeOption = Optional.ofNullable(ctx.queryParam("gazeOption")).orElse("option not set");
                TxtImporter txtImporter = new TxtImporter();
                List<SheetModel> sheets = txtImporter.importTextFile(readAllBytes, fileName, functionName, gazeOption);
                StringBuilder sb = new StringBuilder();
                for (SheetModel sm : sheets) {
                    List<CellRecord> cellRecords = sm.getColumnIndexToCellRecords().get(0);
                    if (cellRecords == null) {
                        break;
                    }
                    for (CellRecord cr : cellRecords) {
                        String line = cr.getRawValue();
                        if (line != null && !line.isBlank() && line.trim().contains(" ")) {
                            sb.append(cr.getRawValue()).append("\n");
                        }
                    }
                }
                Path fullPathForFileContainingTextInput = Path.of(APIController.tempFilesFolder.toString(), dataPersistenceId);
                if (Files.notExists(fullPathForFileContainingTextInput)) {
                    Files.createFile(fullPathForFileContainingTextInput);
                }
                SynchronizedFileWrite.concurrentWriting(fullPathForFileContainingTextInput, sb.toString());
                Files.deleteIfExists(tempDataPath);

                ctx.result("ok").status(HttpURLConnection.HTTP_OK);
            } else {
                ctx.result("txt import simple lines import API endpoint: file not found").status(HttpURLConnection.HTTP_BAD_REQUEST);
            }
        });

        return app;
    }

}
