package com.dci.intellij.dbn.data.export.processor;

import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.export.DataExportException;
import com.dci.intellij.dbn.data.export.DataExportFormat;
import com.dci.intellij.dbn.data.export.DataExportInstructions;
import com.dci.intellij.dbn.data.export.DataExportModel;
import com.dci.intellij.dbn.data.type.GenericDataType;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;


public class HTMLDataExportProcessor extends DataExportProcessor{
    @Override
    public DataExportFormat getFormat() {
        return DataExportFormat.HTML;
    }

    @Override
    public String getFileExtension() {
        return "html";
    }

    @Override
    public String adjustFileName(String fileName) {
        if (!fileName.contains(".html")) {
            fileName = fileName + ".html";
        }
        return fileName;
    }

    @Override
    public boolean canCreateHeader() {
        return true;
    }

    @Override
    public boolean canExportToClipboard() {
        return true;
    }

    @Override
    public boolean canQuoteValues() {
        return false;
    }

    @Override
    public boolean supportsFileEncoding() {
        return true;
    }

    @Override
    public Transferable createClipboardContent(String content) {
        return new HtmlContent(content);
    }

    public class HtmlContent implements Transferable {
        private DataFlavor[] dataFlavors;
        private String content;

        public HtmlContent(String htmlText) {
            content = htmlText;
            try {
                dataFlavors = new DataFlavor[3];
                dataFlavors[0] = new DataFlavor("text/html;class=java.lang.String");
                dataFlavors[1] = new DataFlavor("text/rtf;class=java.lang.String");
                dataFlavors[2] = new DataFlavor("text/plain;class=java.lang.String");

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return dataFlavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return
                    "text/html".equals(flavor.getMimeType()) ||
                    "text/rtf".equals(flavor.getMimeType()) ||
                    "text/plain".equals(flavor.getMimeType());
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException{
            return content;
        }
    }


    @Override
    public void performExport(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connectionHandler) throws DataExportException {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<html>\n");
        buffer.append("    <head>\n");
        buffer.append("        <style type=\"text/css\">\n");
        buffer.append("            tr{vertical-align:top;}\n");
        buffer.append("            td {border:solid #a9a9a9; border-width:1px 0 0 1px; font-family:Verdana,serif; font-size:70%;}\n");
        buffer.append("            table{border:solid #a9a9a9; border-width:0 1px 1px 0;}\n");
        buffer.append("        </style>\n");
        buffer.append("    </head>\n");
        buffer.append("    <body>\n");
        buffer.append("        <table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n");
        buffer.append("            <tr bgcolor=\"#d3d3d3\">\n");

        if (instructions.isCreateHeader()) {
            for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++){
                String columnName = model.getColumnName(columnIndex);
                buffer.append("                <td><b>").append(columnName).append("</b></td>\n");
            }
        }

        buffer.append("            </tr>\n");

        Formatter formatter = getFormatter(connectionHandler.getProject());

        for (int rowIndex=0; rowIndex < model.getRowCount(); rowIndex++) {
            buffer.append("            <tr>\n");

            for (int columnIndex=0; columnIndex < model.getColumnCount(); columnIndex++){
                checkCancelled();
                GenericDataType genericDataType = model.getGenericDataType(columnIndex);
                Object object = model.getValue(rowIndex, columnIndex);
                String value = formatValue(formatter, object);
                value = value.replaceAll("<", "&lt;");
                value = value.replaceAll(">", "&gt;");

                if (StringUtil.isEmptyOrSpaces(value)) value = "&nbsp;";

                boolean isNoWrap =
                        genericDataType == GenericDataType.NUMERIC ||
                        genericDataType == GenericDataType.DATE_TIME ||
                        value.length() < 100;

                boolean isAlignRight = genericDataType == GenericDataType.NUMERIC;

                buffer.append("                <td");
                if (isNoWrap) buffer.append(" nowrap");
                if (isAlignRight) buffer.append(" align=\"right\"");
                buffer.append(">");
                buffer.append(value);
                buffer.append("</td>\n");
            }

            buffer.append("            </tr>\n");
        }
        buffer.append("        </table>\n");
        buffer.append("    </body>\n");
        buffer.append("</html>\n");


        writeContent(instructions, buffer.toString());
    }
}
