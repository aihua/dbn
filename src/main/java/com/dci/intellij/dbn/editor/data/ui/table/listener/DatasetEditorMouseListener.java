package com.dci.intellij.dbn.editor.data.ui.table.listener;

import com.dci.intellij.dbn.common.ref.WeakRef;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.ui.util.Mouse;
import com.dci.intellij.dbn.editor.data.DatasetEditorManager;
import com.dci.intellij.dbn.editor.data.filter.DatasetFilterInput;
import com.dci.intellij.dbn.editor.data.model.DatasetEditorModelCell;
import com.dci.intellij.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dci.intellij.dbn.object.DBColumn;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DatasetEditorMouseListener extends MouseAdapter {
    private final WeakRef<DatasetEditorTable> table;

    public DatasetEditorMouseListener(DatasetEditorTable table) {
        this.table = WeakRef.of(table);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }


    @NotNull
    public DatasetEditorTable getTable() {
        return table.ensure();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            Point mousePoint = e.getPoint();
            DatasetEditorTable table = getTable();
            DatasetEditorModelCell cell = (DatasetEditorModelCell) table.getCellAtLocation(mousePoint);
            if (cell != null) {

                if (table.getSelectedRowCount() <= 1 && table.getSelectedColumnCount() <= 1) {
                    table.cancelEditing();
                    boolean oldEditingStatus = table.isEditingEnabled();
                    table.setEditingEnabled(false);
                    table.selectCell(table.rowAtPoint(mousePoint), table.columnAtPoint(mousePoint));
                    table.setEditingEnabled(oldEditingStatus);
                }

                table.showPopupMenu(e, cell, cell.getColumnInfo());
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (Mouse.isNavigationEvent(e)) {
            DatasetEditorTable table = getTable();
            DatasetEditorModelCell cell = (DatasetEditorModelCell) table.getCellAtLocation(e.getPoint());
            if (cell != null) {
                DBColumn column = cell.getColumn();

                if (column.isForeignKey() && cell.getUserValue() != null) {
                    table.clearSelection();

                    Project project = table.getProject();
                    Progress.prompt(project, column, true,
                            "Opening record",
                            "Opening record details for " + column.getQualifiedNameWithType(),
                            progress -> {
                                DatasetFilterInput filterInput = table.getModel().resolveForeignKeyRecord(cell);
                                if (filterInput != null && filterInput.getColumns().size() > 0) {
                                    Dispatch.run(() -> {
                                        DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(column.getProject());
                                        datasetEditorManager.navigateToRecord(filterInput, e);
                                    });
                                }
                            });
                }
            }
            e.consume();
        }
    }
}