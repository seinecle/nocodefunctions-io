/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.nocodeimportwebservices.import_json;

import io.javalin.Javalin;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import net.clementlevallois.io.io_json.JsonImporter;
import net.clementlevallois.nocodeimportwebservices.APIController;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;
import net.clementlevallois.nocodeimportwebservices.SynchronizedFileWrite;

/**
 *
 * @author LEVALLOIS
 */
public class ImportJsonEndPoints {

    public static Javalin addAll(Javalin app) throws Exception {

        app.get("/api/import/json/simpleLines", ctx -> {
            increment();
            String dataPersistenceId = ctx.queryParam("dataPersistenceId");
            String uniqueFileId = ctx.queryParam("uniqueFileId");
            String jsonKey = ctx.queryParam("jsonKey");
            Path tempDataPath = Path.of(APIController.tempFilesFolder.toString(), dataPersistenceId + uniqueFileId);
            if (Files.exists(tempDataPath)) {
                byte[] readAllBytes = Files.readAllBytes(tempDataPath);
                Path fullPathForFileContainingTextInput = Path.of(APIController.tempFilesFolder.toString(), dataPersistenceId);
                JsonImporter jsonImporter = new JsonImporter();
                String simpleLines = jsonImporter.importJsonFileToSimpleLines(readAllBytes, jsonKey);
                if (Files.notExists(fullPathForFileContainingTextInput)) {
                    Files.createFile(fullPathForFileContainingTextInput);
                }
                SynchronizedFileWrite.concurrentWriting(fullPathForFileContainingTextInput, simpleLines);
                Files.deleteIfExists(tempDataPath);
                ctx.result("ok").status(HttpURLConnection.HTTP_OK);
            } else {
                ctx.result("json import simple lines import API endpoint: file not found").status(HttpURLConnection.HTTP_BAD_REQUEST);
            }
        });
        return app;
    }
}
