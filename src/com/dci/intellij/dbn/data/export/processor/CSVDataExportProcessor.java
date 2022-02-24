package com.dci.intellij.dbn.data.export.processor;

import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.export.DataExportException;
import com.dci.intellij.dbn.data.export.DataExportFormat;
import com.dci.intellij.dbn.data.export.DataExportInstructions;
import com.dci.intellij.dbn.data.export.DataExportModel;

public class CSVDataExportProcessor extends CustomDataExportProcessor{
    @Override
    public DataExportFormat getFormat() {
        return DataExportFormat.CSV;
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public void performExport(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connection) throws DataExportException {
        instructions.setValueSeparator(",");
        super.performExport(model, instructions, connection);
    }
}
