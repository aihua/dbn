package com.dci.intellij.dbn.data.export.processor;

import com.dci.intellij.dbn.code.common.style.DBLCodeStyleManager;
import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.export.DataExportException;
import com.dci.intellij.dbn.data.export.DataExportFormat;
import com.dci.intellij.dbn.data.export.DataExportInstructions;
import com.dci.intellij.dbn.data.export.DataExportModel;
import com.dci.intellij.dbn.data.type.GenericDataType;
import com.dci.intellij.dbn.database.DatabaseMetadataInterface;
import com.dci.intellij.dbn.language.sql.SQLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

import java.util.Date;


public class SQLDataExportProcessor extends DataExportProcessor{
    @Override
    public DataExportFormat getFormat() {
        return DataExportFormat.SQL;
    }

    @Override
    public String getFileExtension() {
        return "sql";
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
        if (fileName != null && !fileName.contains(".sql")) {
            fileName = fileName + ".sql";
        }
        return fileName;
    }

    @Override
    public void performExport(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connection) throws DataExportException {
        Project project = connection.getProject();
        CodeStyleCaseSettings styleCaseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(SQLLanguage.INSTANCE);
        CodeStyleCaseOption kco = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = styleCaseSettings.getObjectCaseOption();

        StringBuilder buffer = new StringBuilder();
        for (int rowIndex=0; rowIndex < model.getRowCount(); rowIndex++) {
            buffer.append(kco.format("insert into "));
            buffer.append(oco.format(model.getTableName()));
            buffer.append(" (");

            int realColumnIndex = 0;
            for (int columnIndex=0; columnIndex < model.getColumnCount(); columnIndex++){
                GenericDataType genericDataType = model.getGenericDataType(columnIndex);
                if (genericDataType == GenericDataType.LITERAL ||
                        genericDataType == GenericDataType.NUMERIC ||
                        genericDataType == GenericDataType.DATE_TIME) {
                    if (realColumnIndex > 0) buffer.append(", ");
                    buffer.append(oco.format(model.getColumnName(columnIndex)));
                    realColumnIndex++;
                }
            }
            buffer.append(kco.format(") values ("));

            realColumnIndex = 0;
            for (int columnIndex=0; columnIndex < model.getColumnCount(); columnIndex++){
                checkCancelled();
                GenericDataType genericDataType = model.getGenericDataType(columnIndex);
                if (genericDataType == GenericDataType.LITERAL ||
                        genericDataType == GenericDataType.NUMERIC ||
                        genericDataType == GenericDataType.DATE_TIME) {
                    if (columnIndex > 0) buffer.append(", ");
                    Object object = model.getValue(rowIndex, columnIndex);
                    String value = object == null ? null : object.toString();
                    if (value == null) {
                        buffer.append(kco.format("null"));
                    } else {
                        if (genericDataType == GenericDataType.LITERAL) {
                            buffer.append("'");
                            value = StringUtil.replace(value, "'", "''");
                            buffer.append(value);
                            buffer.append("'");
                        } else if (genericDataType == GenericDataType.NUMERIC) {
                            buffer.append(value);
                        } else if (genericDataType == GenericDataType.DATE_TIME) {
                            Date date = (Date) object;
                            DatabaseMetadataInterface metadataInterface = connection.getInterfaceProvider().getMetadataInterface();
                            String dateString = metadataInterface.createDateString(date);
                            buffer.append(dateString);
                        }
                    }
                    realColumnIndex++;
                }
            }

            buffer.append(");\n\n");
        }
        writeContent(instructions, buffer.toString());
    }
}
