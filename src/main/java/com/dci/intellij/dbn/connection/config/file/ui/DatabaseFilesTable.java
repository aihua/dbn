package com.dci.intellij.dbn.connection.config.file.ui;

import com.dci.intellij.dbn.common.ui.component.DBNComponent;
import com.dci.intellij.dbn.common.ui.table.DBNEditableTable;
import com.dci.intellij.dbn.common.ui.table.FileBrowserTableCellEditor;
import com.dci.intellij.dbn.connection.config.file.DatabaseFileBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableColumn;

public class DatabaseFilesTable extends DBNEditableTable<DatabaseFilesTableModel> {

    public DatabaseFilesTable(@NotNull DBNComponent parent, DatabaseFileBundle databaseFiles) {
        super(parent, new DatabaseFilesTableModel(databaseFiles), false);
        setDefaultRenderer(Object.class, new DatabaseFilesTableCellRenderer());
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, true, false, false, false, false);
        FileBrowserTableCellEditor fileChooser = new FileBrowserTableCellEditor(fileChooserDescriptor);
        getColumnModel().getColumn(0).setCellEditor(fileChooser);
        setFixedWidth(columnModel.getColumn(1), 100);
    }

    public void setFilePaths(DatabaseFileBundle filesBundle) {
        super.setModel(new DatabaseFilesTableModel(filesBundle));
        setFixedWidth(columnModel.getColumn(1), 100);
    }

    void setFixedWidth(TableColumn tableColumn, int width) {
        tableColumn.setMaxWidth(width);
        tableColumn.setMinWidth(width);
    }
}
