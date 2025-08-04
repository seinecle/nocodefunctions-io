package net.clementlevallois.importers.model;

import java.io.Serializable;
import java.util.*;

public class SheetModel implements Serializable {

    private String name;
    private List<CellRecord> cellRecords = new ArrayList<>();

    private final Map<Integer, List<CellRecord>> rowIndexToCellRecords = new HashMap<>();
    private final Map<Integer, List<CellRecord>> columnIndexToCellRecords = new HashMap<>();
    private Map<Integer, Integer> pageAndStartingLine = new TreeMap<>();

    private List<Map<String, ColumnModel>> sheetData = new ArrayList();
    private List<String> sheetDataWholeLines = new ArrayList();

    private List<ColumnModel> tableHeaderNames = new ArrayList();
    private boolean hasHeaders = false;

    public SheetModel() {
    }

    public SheetModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CellRecord> getCellRecords() {
        return cellRecords;
    }

    public void setCellRecord(List<CellRecord> cellRecords) {
        this.cellRecords = cellRecords;
    }

    public void addCellRecordToVariousDataStructures(CellRecord cellRecord) {
        cellRecords.add(cellRecord);
        int rowIndex = cellRecord.getRowIndex();
        int colIndex = cellRecord.getColIndex();

        rowIndexToCellRecords.computeIfAbsent(rowIndex, k -> new ArrayList<>()).add(cellRecord);
        columnIndexToCellRecords.computeIfAbsent(colIndex, k -> new ArrayList<>()).add(cellRecord);

        if (rowIndex < 10) {
            String fullRow;
            StringBuilder fullRowStringBuilder;
            Map<String, ColumnModel> row;
            if (rowIndex > sheetData.size() - 1) {
                row = new HashMap();
                fullRowStringBuilder = new StringBuilder();
            } else {
                row = sheetData.get(rowIndex);
                fullRow = sheetDataWholeLines.get(rowIndex);
                if (row == null) {
                    row = new HashMap();
                }
                if (fullRow == null) {
                    fullRowStringBuilder = new StringBuilder();
                } else {
                    fullRowStringBuilder = new StringBuilder(fullRow);
                    fullRowStringBuilder.append(" |x| ");
                }
            }
            // this is for the case when the value to be shown in the datatable is formatted with html,
            // not the text stored as a raw value.
            String valueToShowInTable;
            if (cellRecord.getValueWithExtraFormatting() == null) {
                valueToShowInTable = cellRecord.getRawValue();
            } else {
                valueToShowInTable = cellRecord.getValueWithExtraFormatting();
            }
            if (valueToShowInTable == null) {
                valueToShowInTable = "";
            }
            fullRowStringBuilder.append(valueToShowInTable.trim());

            ColumnModel valueInColumn = new ColumnModel(String.valueOf(colIndex), valueToShowInTable);
            row.put(String.valueOf(colIndex), valueInColumn);

            if (rowIndex < sheetData.size()) {
                sheetData.remove(rowIndex);
            }
            if (cellRecord.getRowIndex() > sheetData.size()) {
                sheetData.add(sheetData.size(), row);
            } else {
                sheetData.add(cellRecord.getRowIndex(), row);
            }
            // this is to cover the case when the first row of data starts at row 3 or 4:
            // it should not be inserted at index 3 or 4, because arraylists start at 0.
            if (rowIndex < sheetDataWholeLines.size()) {
                sheetDataWholeLines.remove(rowIndex);
            }
            sheetDataWholeLines.add(fullRowStringBuilder.toString());
        }
    }

    public CellRecord getCellRecord(int rowIndex, int colIndex) {
        for (CellRecord cellRecord : cellRecords) {
            if (cellRecord.getRowIndex() == rowIndex && cellRecord.getColIndex() == colIndex) {
                return cellRecord;
            }
        }
        return null;
    }

    public List<Map<String, ColumnModel>> getSheetData() {
        if (sheetData == null || !hasHeaders || sheetData.isEmpty()) {
            return sheetData;
        } else {
            return sheetData.subList(1, sheetData.size());
        }
    }

    public List<String> getSheetDataWholeLines() {
        if (!hasHeaders || sheetDataWholeLines.size() < 1) {
            return sheetDataWholeLines;
        } else {
            return sheetDataWholeLines.subList(1, sheetDataWholeLines.size());
        }
    }

    public List<ColumnModel> getTableHeaderNames() {
        tableHeaderNames = new ArrayList();
        if (sheetData != null && !sheetData.isEmpty()) {
            Map<String, ColumnModel> firstRow = sheetData.get(0);
            for (Map.Entry<String, ColumnModel> entry : firstRow.entrySet()) {
                String colIndex = entry.getKey();
                String label = hasHeaders
                        ? entry.getValue().getCellValue()
                        : "column " + (Integer.parseInt(colIndex) + 1);
                tableHeaderNames.add(new ColumnModel(colIndex, label));
            }
        }
        return tableHeaderNames;
    }

    public void setTableHeaderNames(List<ColumnModel> tableHeaderNames) {
        this.tableHeaderNames = tableHeaderNames;
    }

    public boolean isHasHeaders() {
        return hasHeaders;
    }

    public void setHasHeaders(boolean hasHeaders) {
        this.hasHeaders = hasHeaders;
    }

    public Map<Integer, List<CellRecord>> getRowIndexToCellRecords() {
        return rowIndexToCellRecords;
    }

    public Map<Integer, List<CellRecord>> getColumnIndexToCellRecords() {
        return columnIndexToCellRecords;
    }

    public Map<Integer, Integer> getPageAndStartingLine() {
        return pageAndStartingLine;
    }

    public void setPageAndStartingLine(Map<Integer, Integer> pageAndStartingLine) {
        this.pageAndStartingLine = pageAndStartingLine;
    }
}
