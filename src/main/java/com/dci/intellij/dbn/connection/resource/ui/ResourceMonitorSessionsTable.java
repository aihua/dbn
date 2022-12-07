package com.dci.intellij.dbn.connection.resource.ui;

import com.dci.intellij.dbn.common.ui.component.DBNComponent;
import com.dci.intellij.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dci.intellij.dbn.common.ui.table.DBNTable;
import com.dci.intellij.dbn.common.ui.util.Borders;
import com.dci.intellij.dbn.connection.ConnectionPool;
import com.dci.intellij.dbn.connection.ConnectionType;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.session.DatabaseSession;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.text.DateFormatUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

class ResourceMonitorSessionsTable extends DBNTable<ResourceMonitorSessionsTableModel> {
    ResourceMonitorSessionsTable(@NotNull DBNComponent parent, ResourceMonitorSessionsTableModel tableModel) {
        super(parent, tableModel, true);
        setDefaultRenderer(DatabaseSession.class, new CellRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellSelectionEnabled(false);
        setRowSelectionAllowed(true);
        adjustRowHeight(2);
        accommodateColumnsSize();
    }


    public static class CellRenderer extends DBNColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            DatabaseSession session = (DatabaseSession) value;
            ConnectionPool connectionPool = session.getConnection().getConnectionPool();

            if (session.isPool()) {
                int connectionPoolSize = connectionPool.getSize();
                SimpleTextAttributes textAttributes = connectionPoolSize == 0 ?
                        SimpleTextAttributes.GRAY_ATTRIBUTES :
                        SimpleTextAttributes.REGULAR_ATTRIBUTES;

                if (column == 0) {
                    append(session.getName(), textAttributes);
                    setIcon(session.getIcon());
                } else if (column == 1) {
                    append(connectionPoolSize == 0 ? "Not connected" : "Connected", textAttributes);
                } else if (column == 2) {
                    long lastAccessTimestamp = connectionPool.getLastAccess();
                    append(lastAccessTimestamp == 0 ? "Never" : DateFormatUtil.formatPrettyDateTime(lastAccessTimestamp), textAttributes);
                } else if (column == 3) {
                    append(connectionPoolSize + " / " + connectionPool.getPeakPoolSize(), textAttributes);
                } else if (column == 4) {
                    List<DBNConnection> conns = connectionPool.getConnections(ConnectionType.POOL);
                    int totalCursorsCount = conns.stream().mapToInt(c -> c.getActiveCursorCount()).sum();
                    append(Integer.toString(totalCursorsCount), textAttributes);
                } else if (column == 5) {
                    List<DBNConnection> conns = connectionPool.getConnections(ConnectionType.POOL);
                    int totalCursorsCount = conns.stream().mapToInt(c -> c.getCachedStatementCount()).sum();
                    append(Integer.toString(totalCursorsCount), textAttributes);
                }
            } else {
                DBNConnection conn = connectionPool.getSessionConnection(session.getId());
                SimpleTextAttributes textAttributes = conn == null ?
                        SimpleTextAttributes.GRAY_ATTRIBUTES :
                        SimpleTextAttributes.REGULAR_ATTRIBUTES;
                if (column == 0) {
                    append(session.getName(), textAttributes);
                    setIcon(session.getIcon());
                } else if (column == 1) {
                    append(conn == null ? "Not connected" : "Connected" + (conn.hasDataChanges() ? " - open transactions" : ""), textAttributes);
                } else if (column == 2) {
                    append(conn == null ? "" : DateFormatUtil.formatPrettyDateTime(conn.getLastAccess()), textAttributes);
                } else if (column == 4) {
                    append(conn == null ? "" : Integer.toString(conn.getActiveCursorCount()), textAttributes);
                } else if (column == 5) {

                }
            }
            setBorder(Borders.TEXT_FIELD_INSETS);

        }
    }
}
