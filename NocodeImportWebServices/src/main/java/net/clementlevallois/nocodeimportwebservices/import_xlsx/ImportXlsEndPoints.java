/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.nocodeimportwebservices.import_xlsx;

import io.javalin.Javalin;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Objects;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.io.xlsx.ExcelReader;
import net.clementlevallois.nocodeimportwebservices.APIController;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;

/**
 *
 * @author LEVALLOIS
 */
public class ImportXlsEndPoints {

    public static void main(String[] args) {
    }

    public static Javalin addAll(Javalin app) throws Exception {

        app.post("/api/import/xlsx", ctx -> {
            increment();
            byte[] bodyAsBytes = ctx.bodyAsBytes();

            if (bodyAsBytes.length == 0) {
                ctx.result("error excel file was null in excel importer API endpoint").status(HttpURLConnection.HTTP_BAD_REQUEST);
            }

            String gazeOption = Objects.requireNonNullElse(ctx.queryParam("gaze_option"), "none");
            String separator = Objects.requireNonNullElse(ctx.queryParam("separator"), ",");

            List<SheetModel> listsOfSheets = ExcelReader.readExcelFile(bodyAsBytes, gazeOption, separator);

            byte[] byteArray = APIController.byteArraySerializerForSheets(listsOfSheets);

            ctx.result(byteArray).status(HttpURLConnection.HTTP_OK);
        });

        return app;
    }

}
