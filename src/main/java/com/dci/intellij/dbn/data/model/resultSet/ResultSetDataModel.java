package com.dci.intellij.dbn.data.model.resultSet;

import com.dci.intellij.dbn.common.dispose.BackgroundDisposer;
import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.dci.intellij.dbn.connection.Resources;
import com.dci.intellij.dbn.connection.context.DatabaseContextBase;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.jdbc.DBNResultSet;
import com.dci.intellij.dbn.connection.jdbc.DBNStatement;
import com.dci.intellij.dbn.connection.jdbc.ResourceStatus;
import com.dci.intellij.dbn.data.model.sortable.SortableDataModel;
import com.dci.intellij.dbn.data.model.sortable.SortableDataModelState;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TableModelEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultSetDataModel<
        R extends ResultSetDataModelRow<? extends ResultSetDataModel<R, C>, C>,
        C extends ResultSetDataModelCell<R, ? extends ResultSetDataModel<R, C>>>
        extends SortableDataModel<R, C> implements DatabaseContextBase {

    private final ConnectionRef connection;
    private DBNResultSet resultSet;

    @Getter
    @Setter
    private boolean resultSetExhausted = false;

    /** execute duration, -1 unknown */
    @Getter
    private long executeDuration = -1;
    /** fetch duration, -1 unknown */
    @Getter
    private long fetchDuration = -1;

    public ResultSetDataModel(@NotNull ConnectionHandler connection) {
        super(connection.getProject());
        this.connection = connection.ref();
    }

    public ResultSetDataModel(DBNResultSet resultSet, @NotNull ConnectionHandler connection, int maxRecords) throws SQLException {
        super(connection.getProject());
        this.connection = connection.ref();
        this.resultSet = resultSet;
        DBNStatement<?> statement = resultSet.getStatement();
        this.executeDuration = statement == null ? 0 : statement.getExecuteDuration();
        setHeader(new ResultSetDataModelHeader<>(connection, resultSet));
        fetchNextRecords(maxRecords, false);

        Disposer.register(connection, this);
    }

    protected R createRow(int resultSetRowIndex) throws SQLException {
        return (R) new ResultSetDataModelRow(this, getResultSet(), resultSetRowIndex);
    }

    @NotNull
    protected DBNResultSet getResultSet() {
        return Failsafe.nn(resultSet);
    }

    @NotNull
    public DBNConnection getResultConnection() {
        return getResultSet().getConnection();
    }

    public void setResultSet(DBNResultSet resultSet) throws SQLException {
        this.resultSet = resultSet;
    }

    @Nullable
    public R getRowAtResultSetIndex(int index) {
        // model may be reloading when this is called, hence
        // IndexOutOfBoundsException is thrown if the range is not checked
        List<R> rows = getRows();
        for (R row : rows) {
            if (row.getResultSetRowIndex() == index) {
                return row;
            }
        }
        return null;
    }

    public int fetchNextRecords(int records, boolean reset) throws SQLException {
        checkDisposed();
        // reset fetch duration
        fetchDuration = -1;
        int originalRowCount = getRowCount();
        if (resultSetExhausted) return originalRowCount;

        int initialIndex = reset ? 0 : originalRowCount;
        int count = 0;

        final List<R> oldRows = getRows();
        List<R> newRows = reset ? new ArrayList<>(oldRows.size()) : new ArrayList<>(oldRows);

        if (resultSet == null || Resources.isClosed(resultSet)) {
            resultSetExhausted = true;
        } else {
            DBNConnection connection = resultSet.getConnection();
            long init = System.currentTimeMillis();
            try {
                connection.set(ResourceStatus.ACTIVE, true);
                connection.updateLastAccess();
                while (count < records) {
                    checkDisposed();
                    if (resultSet != null && resultSet.next()) {
                        count++;
                        R row = createRow(initialIndex + count);
                        newRows.add(row);
                    } else {
                        resultSetExhausted = true;
                        break;
                    }
                }
            } finally {
                fetchDuration = System.currentTimeMillis() - init;
                connection.set(ResourceStatus.ACTIVE, false);
                connection.updateLastAccess();
            }
        }

        checkDisposed();

        sort(newRows);
        setRows(newRows);

        if (reset) {
            oldRows.removeAll(getChangedRows());
            BackgroundDisposer.queue(() -> Disposer.disposeCollection(oldRows));
        }

        int newRowCount = getRowCount();
        if (reset) notifyListeners(null, new TableModelEvent(ResultSetDataModel.this, TableModelEvent.HEADER_ROW));
        if (newRowCount > originalRowCount) notifyRowsInserted(originalRowCount, newRowCount);
        if (newRowCount < originalRowCount) notifyRowsDeleted(newRowCount, originalRowCount);
        int updateIndex = Math.min(originalRowCount, newRowCount);
        if (updateIndex > 0) notifyRowsUpdated(0, updateIndex);


        return newRowCount;
    }

    protected List<R> getChangedRows() {
        return Collections.emptyList();
    }

    public void closeResultSet() {
        Background.run(getProject(), () -> Resources.close(resultSet));
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    protected SortableDataModelState createState() {
        return new SortableDataModelState();
    }

    @Override
    public boolean isReadonly() {
        return true;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return getColumnInfo(columnIndex).getDataType().getTypeClass();
    }

    @Override
    public void disposeInner() {
        closeResultSet();
        super.disposeInner();
    }
}
