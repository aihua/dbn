package com.dci.intellij.dbn.data.export.processor;

import com.dci.intellij.dbn.common.clipboard.Clipboard;
import com.dci.intellij.dbn.common.load.ProgressMonitor;
import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.export.DataExportException;
import com.dci.intellij.dbn.data.export.DataExportFormat;
import com.dci.intellij.dbn.data.export.DataExportInstructions;
import com.dci.intellij.dbn.data.export.DataExportModel;
import com.dci.intellij.dbn.data.type.GenericDataType;

import java.awt.datatransfer.Transferable;


public class XMLDataExportProcessor extends DataExportProcessor{
    @Override
    public DataExportFormat getFormat() {
        return DataExportFormat.XML;
    }

    @Override
    public String getFileExtension() {
        return "xml";
    }

    @Override
    public boolean supports(DataExportFeature feature) {
        return feature.isOneOf(
                DataExportFeature.EXPORT_TO_FILE,
                DataExportFeature.EXPORT_TO_CLIPBOARD,
                DataExportFeature.FILE_ENCODING);
    }

    @Override
    public String adjustFileName(String fileName) {
        if (!fileName.contains(".xml")) {
            fileName = fileName + ".xml";
        }
        return fileName;
    }

    @Override
    public Transferable createClipboardContent(String content) {
        return Clipboard.createXmlContent(content);
    }

    @Override
    public void performExport(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connection) throws DataExportException {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<table name=\"");
        buffer.append(model.getTableName());
        buffer.append("\">\n");
        Formatter formatter = getFormatter(connection.getProject());

        for (int rowIndex=0; rowIndex < model.getRowCount(); rowIndex++) {
            buffer.append("    <row index=\"");
            buffer.append(rowIndex);
            buffer.append("\">\n");
            for (int columnIndex=0; columnIndex < model.getColumnCount(); columnIndex++){
                ProgressMonitor.checkCancelled();
                String columnName = getColumnName(model, instructions, columnIndex);
                GenericDataType genericDataType = model.getGenericDataType(columnIndex);

                String value = null;
                if (genericDataType.isOneOf(
                        GenericDataType.BOOLEAN,
                        GenericDataType.LITERAL,
                        GenericDataType.NUMERIC,
                        GenericDataType.ROWID,
                        GenericDataType.DATE_TIME,
                        GenericDataType.XMLTYPE,
                        GenericDataType.CLOB,
                        GenericDataType.BLOB)) {

                    Object object = model.getValue(rowIndex, columnIndex);
                    value = formatValue(formatter, object);
                }

                if (value == null) value = "";

                boolean isCDATA = Strings.containsOneOf(value, "\n", "<", ">");
                boolean isWrap = isCDATA || value.length() > 100;

                buffer.append("        <column name=\"");
                buffer.append(columnName);
                buffer.append("\">");
                if (isWrap) {
                    buffer.append("\n");
                }
                
                if (isCDATA) {
                    buffer.append("<![CDATA[");
                    buffer.append(value);
                    buffer.append("]]>");
                } else {
                    buffer.append(value);
                }
                buffer.append(isWrap ? "\n        </column>\n" : "</column>\n");
            }

            buffer.append("    </row>\n");
        }
        buffer.append("</table>\n");


        writeContent(instructions, buffer.toString());
    }
}
