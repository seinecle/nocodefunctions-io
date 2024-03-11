/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.clementlevallois.io.xlsx.tests;

import java.util.List;
import net.clementlevallois.io.xlsx.ExcelSaver;
import net.clementlevallois.umigon.model.classification.Document;
import org.junit.Test;

/**
 *
 * @author LEVALLOIS
 */
public class ExportTest {

    @Test
    public void testBundleUmigon() throws InterruptedException {
        Document doc = new Document();
        List<Document> list = List.of(doc);
        ExcelSaver.exportUmigon(list, "fr-FR");
        ExcelSaver.exportUmigon(list, "ja");
        ExcelSaver.exportUmigon(list, "sdsd");
    }
    
    @Test
    public void testBundleOrganic() throws InterruptedException {
        Document doc = new Document();
        List<Document> list = List.of(doc);
        ExcelSaver.exportOrganic(list, "fr-FR");
        ExcelSaver.exportOrganic(list, "ja");
        ExcelSaver.exportOrganic(list, "rfjiefw");
    }
}
