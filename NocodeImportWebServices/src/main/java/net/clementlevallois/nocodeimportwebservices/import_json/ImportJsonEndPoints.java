/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.nocodeimportwebservices.import_json;

import io.javalin.Javalin;
import jakarta.json.JsonArray;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import net.clementlevallois.functions.model.Globals;
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
            String jobId = ctx.queryParam("jobId");
            String fileUniqueId = ctx.queryParam("fileUniqueId");
            String jsonKey = ctx.queryParam("jsonKey");
            Path tempDataPath = APIController.tempFilesFolder.resolve(jobId).resolve(jobId + fileUniqueId);
            if (Files.exists(tempDataPath)) {
                byte[] readAllBytes = Files.readAllBytes(tempDataPath);
                Path fullPathForFileContainingTextInput = Path.of(APIController.tempFilesFolder.toString(), jobId, jobId);
                JsonImporter jsonImporter = new JsonImporter();
                String simpleLines = jsonImporter.importJsonFileToSimpleLines(readAllBytes, jsonKey);
                SynchronizedFileWrite.concurrentWriting(fullPathForFileContainingTextInput, simpleLines);
                Files.delete(tempDataPath);
                ctx.result("ok").status(HttpURLConnection.HTTP_OK);
            } else {
                ctx.result("json import simple lines import API endpoint: file not found").status(HttpURLConnection.HTTP_BAD_REQUEST);
            }
        });
        
        app.get("/api/import/json/appendToPersistedJsonArray", ctx -> {
            increment();
            String jobId = ctx.queryParam("jobId");
            String fileUniqueId = ctx.queryParam("fileUniqueId");
            Path tempDataPath = APIController.globals.getInputFileCompletePath(jobId, fileUniqueId);
            if (Files.exists(tempDataPath)) {
                byte[] readAllBytes = Files.readAllBytes(tempDataPath);
                Path fullPathForJsonArrayFile = Path.of(APIController.tempFilesFolder.toString(), jobId, jobId+Globals.JSON_ARRAY_FILE_EXTENSION);
                JsonImporter jsonImporter = new JsonImporter();
                jsonImporter.appendToPersistedJsonArray(readAllBytes, fullPathForJsonArrayFile);
                Files.delete(tempDataPath);
                ctx.result("ok").status(HttpURLConnection.HTTP_OK);
            } else {
                ctx.result("json import simple lines import API endpoint: file not found").status(HttpURLConnection.HTTP_BAD_REQUEST);
            }
        });

        return app;
    }
}
