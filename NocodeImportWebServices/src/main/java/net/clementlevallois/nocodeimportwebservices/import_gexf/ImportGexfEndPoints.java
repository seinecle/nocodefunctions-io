/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.clementlevallois.nocodeimportwebservices.import_gexf;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.util.NaiveRateLimit;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import net.clementlevallois.importers.import_gexf.controller.GetNamesOfNodeAttributes;
import net.clementlevallois.nocodeimportwebservices.APIController;

/**
 *
 * @author clevallois
 */
public class ImportGexfEndPoints {
     public static Javalin addAll(Javalin app) throws Exception {

       app.get("/api/graphops/getNamesOfNodeAttributes", (Context ctx) -> {
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            NaiveRateLimit.requestPerTimeUnit(ctx, 50, TimeUnit.SECONDS);

            String dataPersistenceId = ctx.queryParam("dataPersistenceId");
            Path tempDataPath = Path.of(APIController.tempFilesFolder.toString(), dataPersistenceId + "_result");
            String gexfAsString = "";
            try {
                gexfAsString = Files.readString(tempDataPath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                objectBuilder.add("-99", "gexf file not readable on disk");
                objectBuilder.add("gexf file: ", tempDataPath.toString());
                JsonObject jsonObject = objectBuilder.build();
                ctx.result(jsonObject.toString()).status(HttpURLConnection.HTTP_BAD_REQUEST);
                return;
            }
            var getNames = new GetNamesOfNodeAttributes(gexfAsString);
            String jsonResult = getNames.returnNamesOfNodeAttributes();
            jsonResult = Json.encodePointer(jsonResult);
            if (jsonResult == null || jsonResult.isBlank()) {
                ctx.result("error in graph ops API to fetch node attribute names, return json is null or empty".getBytes(StandardCharsets.UTF_8)).status(HttpURLConnection.HTTP_INTERNAL_ERROR);
            } else {
                ctx.result(jsonResult.getBytes(StandardCharsets.UTF_8)).status(HttpURLConnection.HTTP_OK);
            }
        });
        return app;
    }

}
