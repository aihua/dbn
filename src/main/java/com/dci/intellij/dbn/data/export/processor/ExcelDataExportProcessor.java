package com.dci.intellij.dbn.data.export.processor;

import com.dci.intellij.dbn.common.load.ProgressMonitor;
import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.export.DataExportException;
import com.dci.intellij.dbn.data.export.DataExportFormat;
import com.dci.intellij.dbn.data.export.DataExportInstructions;
import com.dci.intellij.dbn.data.export.DataExportModel;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

@Slf4j
public class ExcelDataExportProcessor extends DataExportProcessor{

    @Override
    public DataExportFormat getFormat() {
        return DataExportFormat.EXCEL;
    }

    @Override
    public String getFileExtension() {
        return "xls";
    }

    @Override
    public boolean supports(DataExportFeature feature) {
        return feature.isOneOf(
                DataExportFeature.HEADER_CREATION,
                DataExportFeature.FRIENDLY_HEADER,
                DataExportFeature.EXPORT_TO_FILE);
    }

    @Override
    public String adjustFileName(String fileName) {
        if (!fileName.contains(".xls")) {
            fileName = fileName + ".xls";
        }
        return fileName;
    }

    @Override
    public void performExport(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connection) throws DataExportException {
        Workbook workbook = null;
        try {
            workbook = createWorkbook();
            String sheetName = model.getTableName();
            Sheet sheet = createSheet(workbook, sheetName);

            createHeader(model, instructions, workbook, sheet);
            sheet.createFreezePane(0, 1);

            Formatter formatter = getFormatter(connection.getProject());
            CellStyleCache cellStyleCache = new CellStyleCache(workbook, model.getProject());
            for (int r = 0; r < model.getRowCount(); r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < model.getColumnCount(); c++) {
                    createDataCell(model, formatter, cellStyleCache, row, r, c);
                }
            }

            for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }

            createFile(workbook, instructions);
        } catch (DataExportException e) {
            throw e;
        } catch (Exception e) {
            throw new DataExportException("Failed to export data. Cause: " + e.getMessage());
        } finally {
            if (workbook instanceof SXSSFWorkbook) {
                SXSSFWorkbook sxssfWorkbook = (SXSSFWorkbook) workbook;
                sxssfWorkbook.dispose();
            }
        }

    }

    private static void createFile(Workbook workbook, DataExportInstructions instructions) throws DataExportException {
        File file = instructions.getFile();
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)){
            workbook.write(fileOutputStream);
            fileOutputStream.flush();
        } catch (Exception e) {
            log.warn("Failed to export data", e);
            throw new DataExportException("Could not write file " + file.getPath() + ".\nCause: " + e.getMessage());
        }
    }

    private static Sheet createSheet(Workbook workbook, String sheetName) {
        return Strings.isEmpty(sheetName) ? workbook.createSheet() : workbook.createSheet(sheetName);
    }

    private static void createDataCell(DataExportModel model, Formatter formatter, CellStyleCache cellStyleCache, Row row, int r, int c) throws DataExportException {
        ProgressMonitor.checkCancelled();
        Cell cell = row.createCell(c);
        Object value = model.getValue(r, c);
        if (value == null)  return;

        if (value instanceof Number) {
            Number number = (Number) value;
            double doubleValue = number.doubleValue();
            cell.setCellValue(doubleValue);
            cell.setCellStyle(
                    doubleValue % 1 == 0 ?
                            cellStyleCache.getIntegerStyle() :
                            cellStyleCache.getNumberStyle());

        } else if (value instanceof Date) {
            Date date = (Date) value;
            boolean hasTime = hasTimeComponent(date);
            cell.setCellValue(date);
            cell.setCellStyle(hasTime ?
                    cellStyleCache.getDatetimeStyle() :
                    cellStyleCache.getDateStyle());
        } else {
            String stringValue = formatValue(formatter, value);
            cell.setCellValue(stringValue);
        }
    }

    private void createHeader(DataExportModel model, DataExportInstructions instructions, Workbook workbook, Sheet sheet) {
        if (!instructions.isCreateHeader()) return;

        Row headerRow = sheet.createRow(0);

        for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
            String columnName = getColumnName(model, instructions, columnIndex);

            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(columnName);

            CellStyle cellStyle = workbook.createCellStyle();
            Font tableHeadingFont = workbook.createFont();
            tableHeadingFont.setBold(true);
            cellStyle.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cellStyle.setFont(tableHeadingFont);
            cell.setCellStyle(cellStyle);
        }
    }

    protected Workbook createWorkbook() {
        return new HSSFWorkbook();
    }

    protected RichTextString createRichText(String string) {
        return new HSSFRichTextString(string);
    }

    private class CellStyleCache {
        private final Workbook workbook;
        private final Formatter formatter;

        private CellStyle dateStyle;
        private CellStyle datetimeStyle;
        private CellStyle numberStyle;
        private CellStyle integerStyle;

        private CellStyleCache(Workbook workbook, Project project) {
            this.workbook = workbook;
            formatter = getFormatter(project);
        }

        public CellStyle getDateStyle() {
            if (dateStyle == null) {
                dateStyle = workbook.createCellStyle();
                String dateFormatPattern = formatter.getDateFormatPattern();
                short dateFormat = getFormat(dateFormatPattern);
                dateStyle.setDataFormat(dateFormat);
            }
            return dateStyle;
        }

        public CellStyle getDatetimeStyle() {
            if (datetimeStyle == null) {
                datetimeStyle = workbook.createCellStyle();
                String datetimeFormatPattern = formatter.getDatetimeFormatPattern();
                short dateFormat = getFormat(datetimeFormatPattern);
                datetimeStyle.setDataFormat(dateFormat);
            }

            return datetimeStyle;
        }

        public CellStyle getNumberStyle() {
            if (numberStyle == null) {
                numberStyle = workbook.createCellStyle();
                String numberFormatPattern = formatter.getNumberFormatPattern();
                short numberFormat = getFormat(numberFormatPattern);
                numberStyle.setDataFormat(numberFormat);
            }

            return numberStyle;
        }

        public CellStyle getIntegerStyle() {
            if (integerStyle == null) {
                integerStyle = workbook.createCellStyle();
                String integerFormatPattern = formatter.getIntegerFormatPattern();
                short integerFormat = getFormat(integerFormatPattern);
                integerStyle.setDataFormat(integerFormat);
            }

            return integerStyle;
        }

        private short getFormat(String datetimeFormatPattern) {
            CreationHelper creationHelper = workbook.getCreationHelper();
            DataFormat dataFormat = creationHelper.createDataFormat();
            return dataFormat.getFormat(datetimeFormatPattern);
        }

    }
}