package com.dci.intellij.dbn.data.export.processor;

import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.export.DataExportException;
import com.dci.intellij.dbn.data.export.DataExportFormat;
import com.dci.intellij.dbn.data.export.DataExportInstructions;
import com.dci.intellij.dbn.data.export.DataExportInstructions.Scope;
import com.dci.intellij.dbn.data.export.DataExportModel;
import com.dci.intellij.dbn.data.value.ValueAdapter;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.dci.intellij.dbn.common.dispose.Failsafe.conditionallyLog;

public abstract class DataExportProcessor {
    public abstract boolean supports(DataExportFeature feature);

    public abstract void performExport(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connection) throws DataExportException;

    Formatter getFormatter(Project project) {
        return Formatter.getInstance(project).clone();
    }

    public abstract String getFileExtension();

    public void export(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connection)
            throws DataExportException {
        try {
            int cols = model.getColumnCount();
            int rows = model.getRowCount();
            boolean selectionScope = instructions.getScope() == Scope.SELECTION;
            if ((cols == 0 || rows == 0) && selectionScope) {
                throw new DataExportException("No content selected for export. Uncheck the Scope \"Selection\" if you want to export the entire content.");
            }
            String fileName = adjustFileName(instructions.getFileName());
            instructions.setFileName(fileName);
            performExport(model, instructions, connection);
        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
        } catch (DataExportException e) {
            throw e;
        } catch (Exception e) {
            throw new DataExportException(e.getMessage());
        }
    }

    public abstract DataExportFormat getFormat();

    void writeContent(DataExportInstructions instructions, String content) throws DataExportException {
        if (instructions.getDestination() == DataExportInstructions.Destination.CLIPBOARD) {
            writeToClipboard(content);
        } else {
            writeToFile(instructions.getFile(), content, instructions.getCharset());
        }
    }

    private void writeToFile(File file, String content, Charset charset) throws DataExportException {
        // TODO (java 11+) Files.writeString(file.toPath(), content, charset);
        Path filePath = file.toPath();
        try (BufferedWriter writer = createFileWriter(filePath, charset)){
                writer.write(content);
                writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new DataExportException("Could not write file " + file.getPath() + ".\nCause: " + e.getMessage());
        }
    }

    @NotNull
    private static BufferedWriter createFileWriter(Path path, Charset charset) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(path), charset));
    }

    private void writeToClipboard(String content) {
        Transferable clipboardContent = createClipboardContent(content);

        CopyPasteManager copyPasteManager = CopyPasteManager.getInstance();
        copyPasteManager.setContents(clipboardContent);
    }

    public Transferable createClipboardContent(String content) {
        return new StringSelection(content);
    }

    public String adjustFileName(String fileName) {
        return fileName;
    }

    public static boolean hasTimeComponent(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);

        return
            calendar.get(Calendar.HOUR) != 0 ||
            calendar.get(Calendar.MINUTE) != 0 ||
            calendar.get(Calendar.SECOND) != 0 ||
            calendar.get(Calendar.MILLISECOND) != 0;
    }

    protected static String formatValue(Formatter formatter, Object value) throws DataExportException {
        if (value != null) {
            if (value instanceof Number) {
                Number number = (Number) value;
                return formatter.formatNumber(number);
            } else if (value instanceof Date) {
                Date date = (Date) value;
                return hasTimeComponent(date) ?
                        formatter.formatDateTime(date) :
                        formatter.formatDate(date);
            } else if (value instanceof ValueAdapter){
                ValueAdapter valueAdapter = (ValueAdapter) value;
                try {
                    return Commons.nvl(valueAdapter.export(), "");
                } catch (SQLException e) {
                    throw new DataExportException("Failed to export " + valueAdapter.getGenericDataType() + " cell. Cause: "  + e.getMessage());
                }
            } else {
                return value.toString();
            }
        }
        return "";
    }

    protected String getColumnName(DataExportModel model, DataExportInstructions instructions, int columnIndex) {
        return instructions.isFriendlyHeaders() ?
                model.getColumnFriendlyName(columnIndex) :
                model.getColumnName(columnIndex);
    }
}
