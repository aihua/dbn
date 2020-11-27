package com.dci.intellij.dbn.connection.resource.ui;

import com.dci.intellij.dbn.common.ui.Borders;
import com.dci.intellij.dbn.common.ui.component.DBNComponent;
import com.dci.intellij.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dci.intellij.dbn.common.ui.table.DBNTable;
import com.dci.intellij.dbn.connection.transaction.PendingTransaction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ResourceMonitorTransactionsTable extends DBNTable<ResourceMonitorTransactionsTableModel> {

    ResourceMonitorTransactionsTable(@NotNull DBNComponent parent, ResourceMonitorTransactionsTableModel model) {
        super(parent, model, false);
        setDefaultRenderer(PendingTransaction.class, new CellRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellSelectionEnabled(true);
        adjustRowHeight(2);
        accommodateColumnsSize();
        addMouseListener(new MouseListener());
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (e.getID() != MouseEvent.MOUSE_DRAGGED && getChangeAtMouseLocation() != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            super.processMouseMotionEvent(e);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private PendingTransaction getChangeAtMouseLocation() {
        Point location = MouseInfo.getPointerInfo().getLocation();
        location.setLocation(location.getX() - getLocationOnScreen().getX(), location.getY() - getLocationOnScreen().getY());

        int columnIndex = columnAtPoint(location);
        int rowIndex = rowAtPoint(location);
        if (columnIndex > -1 && rowIndex > -1) {
            return (PendingTransaction) getModel().getValueAt(rowIndex, columnIndex);
        }

        return null;
    }

    public static class CellRenderer extends DBNColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            PendingTransaction transaction = (PendingTransaction) value;
            if (column == 0) {
                setIcon(transaction.getFileIcon());
                append(transaction.getFilePath(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

            } else if (column == 1) {
                append(transaction.getChangesCount() + " uncommitted changes", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
            setBorder(Borders.TEXT_FIELD_BORDER);

        }
    }

    public class MouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                int selectedRow = getSelectedRow();
                PendingTransaction change = (PendingTransaction) getModel().getValueAt(selectedRow, 0);
                VirtualFile virtualFile = change.getFile();
                if (virtualFile != null) {
                    FileEditorManager fileEditorManager = FileEditorManager.getInstance(getProject());
                    fileEditorManager.openFile(virtualFile, true);
                }
            }

        }
    }
}
