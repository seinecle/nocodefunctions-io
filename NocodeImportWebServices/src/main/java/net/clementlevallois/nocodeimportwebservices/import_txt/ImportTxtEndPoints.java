/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.nocodeimportwebservices.import_txt;

import io.javalin.Javalin;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import net.clementlevallois.importers.import_txt.controller.TxtImporter;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.nocodeimportwebservices.APIController;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;

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

            InputStream is = new ByteArrayInputStream(bodyAsBytes);
            TxtImporter txtImporter = new TxtImporter();
            List<SheetModel> sheets = txtImporter.importTextFile(is, fileName, functionName, gazeOption);
            
            byte[] byteArray = APIController.byteArraySerializerForSheets(sheets);

            ctx.result(byteArray).status(HttpURLConnection.HTTP_OK);
        });

        return app;
    }

}
