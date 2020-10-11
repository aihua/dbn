package com.dci.intellij.dbn.editor.data.ui.table.listener;

import com.dci.intellij.dbn.common.ui.MouseUtil;
import com.dci.intellij.dbn.editor.data.DatasetEditorManager;
import com.dci.intellij.dbn.editor.data.filter.DatasetFilterInput;
import com.dci.intellij.dbn.editor.data.model.DatasetEditorModelCell;
import com.dci.intellij.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.dci.intellij.dbn.object.DBColumn;
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
    public void mouseReleased(final MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON3) {
            Point mousePoint = event.getPoint();
            DatasetEditorTable table = getTable();
            DatasetEditorModelCell cell = (DatasetEditorModelCell) table.getCellAtLocation(mousePoint);
            if (cell != null) {

                if (table.getSelectedRowCount() <= 1 && table.getSelectedColumnCount() <= 1) {
                    table.cancelEditing();
                    boolean oldEditingSatus = table.isEditingEnabled();
                    table.setEditingEnabled(false);
                    table.selectCell(table.rowAtPoint(mousePoint), table.columnAtPoint(mousePoint));
                    table.setEditingEnabled(oldEditingSatus);
                }

                table.showPopupMenu(event, cell, cell.getColumnInfo());
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (MouseUtil.isNavigationEvent(event)) {
            DatasetEditorTable table = getTable();
            DatasetEditorModelCell cell = (DatasetEditorModelCell) table.getCellAtLocation(event.getPoint());
            if (cell != null){
                DBColumn column = cell.getColumnInfo().getColumn();

                if (column.isForeignKey() && cell.getUserValue() != null) {
                    table.clearSelection();
                    DatasetFilterInput filterInput = table.getModel().resolveForeignKeyRecord(cell);
                    if (filterInput != null && filterInput.getColumns().size() > 0) {
                        DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(column.getProject());
                        datasetEditorManager.navigateToRecord(filterInput, event);
                        event.consume();
                    }
                }
            }
        }
    }
}