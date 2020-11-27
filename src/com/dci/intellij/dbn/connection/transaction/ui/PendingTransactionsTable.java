package com.dci.intellij.dbn.connection.transaction.ui;

import com.dci.intellij.dbn.common.ui.Borders;
import com.dci.intellij.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dci.intellij.dbn.common.ui.table.DBNTable;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.session.DatabaseSession;
import com.dci.intellij.dbn.connection.transaction.PendingTransaction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PendingTransactionsTable extends DBNTable<PendingTransactionsTableModel> {
    public PendingTransactionsTable(@NotNull PendingTransactionsDetailForm parent, @NotNull PendingTransactionsTableModel model) {
        super(parent, model, false);
        CellRenderer cellRenderer = new CellRenderer();
        setDefaultRenderer(PendingTransaction.class, cellRenderer);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellSelectionEnabled(true);
        adjustRowHeight(2);
        accommodateColumnsSize();
        addMouseListener(new MouseListener());
    }

    public List<DBNConnection> getSelectedConnections() {
        int[] selectedRows = getSelectedRows();
        if (selectedRows != null && selectedRows.length > 0) {
            Set<DBNConnection> connections = new LinkedHashSet<>();
            for (int selectedRow : selectedRows) {
                connections.add(getModel().getValueAt(selectedRow, 0).getConnection());
            }
            return new ArrayList<>(connections);
        }
        return Collections.emptyList();
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

    public PendingTransaction getChangeAtMouseLocation() {
        Point location = MouseInfo.getPointerInfo().getLocation();
        location.setLocation(location.getX() - getLocationOnScreen().getX(), location.getY() - getLocationOnScreen().getY());

        int columnIndex = columnAtPoint(location);
        int rowIndex = rowAtPoint(location);
        if (columnIndex > -1 && rowIndex > -1) {
            return getModel().getValueAt(rowIndex, columnIndex);
        }

        return null;
    }

    public class MouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                int selectedRow = getSelectedRow();
                PendingTransaction transaction = getModel().getValueAt(selectedRow, 0);
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(getProject());
                VirtualFile virtualFile = transaction.getFile();
                if (virtualFile != null) {
                    fileEditorManager.openFile(virtualFile, true);
                }
            }
        }
    }

    private static class CellRenderer extends DBNColoredTableCellRenderer {

        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            PendingTransactionsTableModel model = (PendingTransactionsTableModel) table.getModel();
            ConnectionHandler connectionHandler = model.getConnectionHandler();
            PendingTransaction transaction = (PendingTransaction) value;
            if (column == 0) {
                DatabaseSession session = connectionHandler.getSessionBundle().getSession(transaction.getSessionId());
                setIcon(session.getId().getIcon());
                append(session.getName());

            }
            else if (column == 1) {
                setIcon(transaction.getFileIcon());
                append(transaction.getFilePath(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

            } else if (column == 2) {
                int changesCount = transaction.getChangesCount();
                append(changesCount == 1 ?
                        changesCount + " uncommitted change" :
                        changesCount + " uncommitted changes",
                        SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
            setBorder(Borders.TEXT_FIELD_BORDER);
        }
    }
}
