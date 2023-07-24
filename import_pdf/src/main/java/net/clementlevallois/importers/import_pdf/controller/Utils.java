/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.importers.import_pdf.controller;

import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 *
 * @author LEVALLOIS
 */
public class Utils {

    private static final Set<String> hyphens = Set.of(
            Character.toString('\u2010'),
            Character.toString('\u2011'),
            Character.toString('\u2012'),
            Character.toString('\u2013'),
            Character.toString('\u002D'),
            Character.toString('\u007E'),
            Character.toString('\u00AD'),
            Character.toString('\u058A'),
            Character.toString('\u05BE'),
            Character.toString('\u1806'),
            Character.toString('\u2014'),
            Character.toString('\u2015'),
            Character.toString('\u2053'),
            Character.toString('\u207B'),
            Character.toString('\u208B'),
            Character.toString('\u2212'),
            Character.toString('\u301C'),
            Character.toString('\uFE58'),
            Character.toString('\uFE63'),
            Character.toString('\uFF0D'));

    public static String fixHyphenatedWordsAndReturnAllLinesInOnePara(String[] lines) {
        StringBuilder sb = new StringBuilder();
        boolean cutWordDetected = false;
        String stichtedWord = "";
        for (String line : lines) {
            if (cutWordDetected) {
                int indexFirstSpace = line.indexOf(" ");
                if (indexFirstSpace > 0) {
                    stichtedWord += line.substring(0, indexFirstSpace);
                    line = stichtedWord + line.substring(indexFirstSpace);
                }
                stichtedWord = "";
                cutWordDetected = false;
            }
            if (line.length() > 0) {
                String lastChar = line.substring(line.length() - 1);
                if (hyphens.contains(lastChar)) {
                    cutWordDetected = true;
                    int indexLastSpace = line.lastIndexOf(" ");
                    if (indexLastSpace > 0) {
                        stichtedWord += line.substring(indexLastSpace, line.length() - 1);
                        line = line.substring(0, indexLastSpace);
                    }
                } else {
                    stichtedWord = "";
                }
            }
            boolean needExtraSpace = !sb.toString().endsWith(" ") & !line.startsWith(" ");
            if (needExtraSpace) {
                sb.append(" ");
            }
            sb.append(line);
        }
        return sb.toString();

    }

    public static Map<Integer, String> fixHyphenatedWordsAndReturnOriginalEntries(Map<Integer, String> lines) {
        boolean cutWordDetected = false;
        String stichtedWord = "";

        for (Map.Entry<Integer, String> entry : lines.entrySet()) {
            String line = entry.getValue().trim();
            if (cutWordDetected) {
                int indexFirstSpace = line.indexOf(" ");
                if (indexFirstSpace > 0) {
                    stichtedWord += line.substring(0, indexFirstSpace);
                    line = stichtedWord + line.substring(indexFirstSpace);
                }
                line = Jsoup.clean(line, Safelist.basicWithImages().addAttributes("span", "style"));
                entry.setValue(line);
                stichtedWord = "";
                cutWordDetected = false;
            }
            if (line.length() > 0) {
                String lastChar = line.substring(line.length() - 1);
                if (hyphens.contains(lastChar)) {
                    cutWordDetected = true;
                    int indexLastSpace = line.lastIndexOf(" ");
                    if (indexLastSpace > 0) {
                        stichtedWord += line.substring(indexLastSpace, line.length() - 1);
                        line = line.substring(0, indexLastSpace);
                    }
                    line = Jsoup.clean(line, Safelist.basicWithImages().addAttributes("span", "style"));
                    entry.setValue(line);
                } else {
                    stichtedWord = "";
                }
            }
        }
        return lines;
    }

}
