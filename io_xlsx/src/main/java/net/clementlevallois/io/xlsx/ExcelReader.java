/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.io.xlsx;


import com.github.pjfanning.xlsx.StreamingReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.clementlevallois.importers.model.CellRecord;
import net.clementlevallois.importers.model.ColumnModel;
import net.clementlevallois.importers.model.SheetModel;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 *
 * @author LEVALLOIS
 */
public class ExcelReader {

    public static List<String> getListOfSheets(InputStream inputStream) throws FileNotFoundException, IOException {
        List<String> sheetNames = new ArrayList();
        try {
            try (
                    OPCPackage pkg = OPCPackage.open(inputStream)) {
                XSSFReader r = new XSSFReader(pkg);
                XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) r.getSheetsData();
                while (iter.hasNext()) {
                    try (InputStream stream = iter.next()) {
                        String sheetName = iter.getSheetName();
                        sheetNames.add(sheetName);
                    }
                }
            }

            return sheetNames;
        } catch (InvalidFormatException ex) {
            Logger.getLogger(ExcelReader.class.getName()).log(Level.SEVERE, null, ex);
            return sheetNames;
        } catch (OpenXML4JException ex) {
            Logger.getLogger(ExcelReader.class.getName()).log(Level.SEVERE, null, ex);
            return sheetNames;
        }
    }

    public static List<SheetModel> readExcelFile(byte[] bytes, String gazeOption, String separator) throws FileNotFoundException, IOException {

        List<SheetModel> sheets = new ArrayList();

        if (bytes == null || bytes.length == 0) {
            return sheets;
        }

        InputStream is = new ByteArrayInputStream(bytes);

        try (Workbook wb = StreamingReader.builder()
                .rowCacheSize(100) // number of rows to keep in memory (defaults to 10)
                .bufferSize(4096) // buffer size to use when reading InputStream to file (defaults to 1024)
                .open(is)) {
            int sheetNumber = 0;
            for (Sheet sheet : wb) {
                sheetNumber++;
                List<ColumnModel> headerNames = new ArrayList();
                SheetModel sheetModel = new SheetModel();
                sheetModel.setName(sheet.getSheetName());
                int rowNumber = 0;

                int leftiestColumnIndex = -1;

                for (Row r : sheet) {
                    if (rowNumber == 0) {
                        for (Cell cell : r) {
                            if (cell == null) {
                                continue;
                            }
                            int columnIndex = cell.getColumnIndex();
                            int rowIndex = cell.getRowIndex();

                            // this condition has for effect to store permanently the column index of the first column non empty on the left - which might not always be the one at index 0
                            if ((leftiestColumnIndex == -1) && columnIndex > leftiestColumnIndex) {
                                leftiestColumnIndex = columnIndex;
                            }

                            String cellStringValue = ExcelReader.returnStringValue(cell);
                            cellStringValue = Jsoup.clean(cellStringValue, Safelist.basicWithImages().addAttributes("span", "style"));

                            // adding the first line as a header
                            ColumnModel cmHeader = new ColumnModel(String.valueOf(columnIndex), cellStringValue);
                            headerNames.add(cmHeader);

                            if (gazeOption.equals("sim") && columnIndex > leftiestColumnIndex) {
                                // adding the first line as a cells (in case the first line is not a header)
                                String[] valuesInColumn = cellStringValue.split(separator);
                                int valueCount = 0;
                                for (String value : valuesInColumn) {
                                    CellRecord cellRecord = new CellRecord(rowIndex, columnIndex + valueCount++, value.trim());
                                    sheetModel.addCellRecord(cellRecord);
                                }
                            } else {
                                CellRecord cellRecord = new CellRecord(rowIndex, columnIndex, cellStringValue);
                                sheetModel.addCellRecord(cellRecord);
                            }
                        }
                        sheetModel.setTableHeaderNames(headerNames);
                    }
                    rowNumber++;

                    for (Cell cell : r) {
                        if (cell == null) {
                            continue;
                        }

                        int columnIndex = cell.getColumnIndex();
                        int rowIndex = cell.getRowIndex();

                        String returnStringValue = ExcelReader.returnStringValue(cell);
                        returnStringValue = Jsoup.clean(returnStringValue, Safelist.basicWithImages().addAttributes("span", "style"));

                        // in the case of the similarity computation function, it is desirable to split the content of the target cell so that each value is taken as a separate target
                        if (gazeOption.equals("sim") && columnIndex > leftiestColumnIndex) {
                            String[] valuesInColumn = returnStringValue.split(separator);
                            int valueCount = 0;
                            for (String value : valuesInColumn) {
                                CellRecord cellRecord = new CellRecord(rowIndex, columnIndex + valueCount++, value.trim());
                                sheetModel.addCellRecord(cellRecord);
                            }
                        } else {
                            CellRecord cellRecord = new CellRecord(cell.getRowIndex(), cell.getColumnIndex(), returnStringValue);
                            sheetModel.addCellRecord(cellRecord);
                        }

                    }
                }
                sheets.add(sheetModel);
            }
        }
        return sheets;
    }

    public static String returnStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();
        try {

            switch (cellType) {
                case NUMERIC -> {
                    try {
                        double doubleVal = cell.getNumericCellValue();
                        if (doubleVal == (int) doubleVal) {
                            int value = Double.valueOf(doubleVal).intValue();
                            return String.valueOf(value);
                        } else {
                            return String.valueOf(doubleVal);
                        }
                    } catch (java.lang.NumberFormatException e) {
                        System.out.println("error reading double value in cell");
                        return "error decoding value of the cell";
                    }
                }
                case STRING -> {
                    String stringCellValue = cell.getStringCellValue();
                    return stringCellValue;
                }
                case ERROR -> {
                    return "#ERR";
                }
                case BLANK -> {
                    return "";
                }
                case FORMULA -> {
                    return cell.getCellFormula();
                }
                case BOOLEAN -> {
                    return String.valueOf(cell.getBooleanCellValue());
                }
            }
        } catch (Exception e) {
            return "error decoding value of the cell";
        }
        return "error decoding value of the cell";
    }

}
