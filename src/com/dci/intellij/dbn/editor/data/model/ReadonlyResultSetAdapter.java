package com.dci.intellij.dbn.editor.data.model;

import com.dci.intellij.dbn.common.dispose.AlreadyDisposedException;
import com.dci.intellij.dbn.common.util.CollectionUtil;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.jdbc.DBNResultSet;
import com.dci.intellij.dbn.connection.transaction.ConnectionSavepoint;
import com.dci.intellij.dbn.data.model.ColumnInfo;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.type.DBNativeDataType;
import com.dci.intellij.dbn.data.value.ValueAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReadonlyResultSetAdapter extends ResultSetAdapter {
    private DBNConnection connection;
    private Row currentRow;

    ReadonlyResultSetAdapter(DatasetEditorModel model, DBNResultSet resultSet) {
        super(model);
        this.connection = resultSet.getConnection();
    }

    @Override
    public synchronized void scroll(final int rowIndex) throws SQLException {
        if (!isInsertMode()) {
            DatasetEditorModelRow modelRow = getModel().getRowAtResultSetIndex(rowIndex);
            if (modelRow == null) {
                throw new SQLException("Could not scroll to row index " + rowIndex);
            }

            currentRow = new Row();
            List<DatasetEditorModelCell> modelCells = modelRow.getCells();
            for (DatasetEditorModelCell modelCell : modelCells) {
                DatasetEditorColumnInfo columnInfo = modelCell.getColumnInfo();
                if (columnInfo.isPrimaryKey()) {
                    currentRow.addKeyCell(columnInfo, modelCell.getUserValue());
                }
            }
        }
    }

    @Override
    public synchronized void updateRow() throws SQLException {
        if (!isInsertMode())  {
            if (isUseSavePoints()) {
                ConnectionSavepoint.run(connection, () -> this.executeUpdate());
            } else {
                executeUpdate();
            }
        }
    }

    @Override
    public synchronized void refreshRow() {
        // not supported
    }


    @Override
    public synchronized void startInsertRow() {
        if (!isInsertMode())  {
            setInsertMode(true);
            currentRow = new Row();
        }
    }

    @Override
    public synchronized void cancelInsertRow() {
        if (isInsertMode())  {
            setInsertMode(false);
            currentRow = null;
        }
    }

    @Override
    public synchronized void insertRow() throws SQLException {
        if (isInsertMode())  {
            if (isUseSavePoints()) {
                ConnectionSavepoint.run(connection, () -> {
                    executeInsert();
                    setInsertMode(false);
                });
            } else {
                executeInsert();
                setInsertMode(false);
            }
        }
    }

    @Override
    public synchronized void deleteRow() throws SQLException {
        if (!isInsertMode())  {
            if (isUseSavePoints()) {
                ConnectionSavepoint.run(connection, this::executeDelete);
            } else {
                executeDelete();
            }
        }
    }

    @Override
    public synchronized void setValue(final int columnIndex, @NotNull final ValueAdapter valueAdapter, @Nullable final Object value) {
        DatasetEditorColumnInfo columnInfo = getColumnInfo(columnIndex);
        currentRow.addChangedCell(columnInfo, value);
    }

    @Override
    public synchronized void setValue(final int columnIndex, @NotNull final DBDataType dataType, @Nullable final Object value) {
        DatasetEditorColumnInfo columnInfo = getColumnInfo(columnIndex);
        currentRow.addChangedCell(columnInfo, value);
    }

    DatasetEditorColumnInfo getColumnInfo(int columnIndex) {
        return getModel().getHeader().getResultSetColumnInfo(columnIndex);
    }

    private void executeUpdate() throws SQLException {
        List<Cell> keyCells = currentRow.getKeyCells();
        if (keyCells.size() == 0) {
            throw new SQLException("No primary key defined for table");
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append("update ");
        buffer.append(getModel().getDataset().getQualifiedName());
        buffer.append(" set ");

        List<Cell> changedCells = currentRow.getChangedCells();
        for (Cell cell : changedCells) {
            buffer.append(cell.getColumnName());
            buffer.append(" = ?");
            if (!CollectionUtil.isLast(changedCells, cell)) {
                buffer.append(", ");
            }
        }
        buffer.append(" where ");

        for (Cell cell : keyCells) {
            buffer.append(cell.getColumnName());
            buffer.append(" = ?");
            if (!CollectionUtil.isLast(keyCells, cell)) {
                buffer.append(" and ");
            }
        }

        PreparedStatement preparedStatement = connection.prepareStatement(buffer.toString());
        int paramIndex = 0;
        for (Cell cell : changedCells) {
            paramIndex++;
            DBNativeDataType nativeDataType = cell.getDataType();
            nativeDataType.setValueToStatement(preparedStatement, paramIndex, cell.getValue());
        }
        for (Cell cell : keyCells) {
            paramIndex++;
            DBNativeDataType nativeDataType = cell.getDataType();
            nativeDataType.setValueToStatement(preparedStatement, paramIndex, cell.getValue());
        }
        preparedStatement.executeUpdate();
    }

    private void executeInsert() throws SQLException {
        List<Cell> changedCells = currentRow.getChangedCells();
        if (changedCells.size() == 0) {
            throw AlreadyDisposedException.INSTANCE;
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append("insert into ");
        buffer.append(getModel().getDataset().getQualifiedName());
        buffer.append(" (");

        for (Cell cell : changedCells) {
            buffer.append(cell.getColumnName());
            buffer.append(", ");
        }
        buffer.delete(buffer.length() -2, buffer.length());
        buffer.append(" ) values (");

        for (Cell cell : changedCells) {
            buffer.append(" ? ");
            buffer.append(", ");
        }
        buffer.delete(buffer.length() -2, buffer.length());
        buffer.append(")");


        PreparedStatement preparedStatement = connection.prepareStatement(buffer.toString());
        int paramIndex = 0;
        for (Cell cell : changedCells) {
            paramIndex++;
            DBNativeDataType nativeDataType = cell.getDataType();
            nativeDataType.setValueToStatement(preparedStatement, paramIndex, cell.getValue());
        }
        preparedStatement.executeUpdate();
    }

    private void executeDelete() throws SQLException {
        List<Cell> keyCells = currentRow.getKeyCells();
        if (keyCells.size() == 0) {
            throw new SQLException("No primary key defined for table");
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append("delete from ");
        buffer.append(getModel().getDataset().getQualifiedName());
        buffer.append(" where ");

        for (Cell cell : keyCells) {
            buffer.append(cell.getColumnName());
            buffer.append(" = ? ");
        }

        PreparedStatement preparedStatement = connection.prepareStatement(buffer.toString());
        int paramIndex = 0;
        for (Cell cell : keyCells) {
            paramIndex++;
            DBNativeDataType nativeDataType = cell.getDataType();
            nativeDataType.setValueToStatement(preparedStatement, paramIndex, cell.getValue());
        }
        preparedStatement.executeUpdate();
    }

    private static class Cell {
        private final ColumnInfo columnInfo;
        private final Object value;

        Cell(ColumnInfo columnInfo, Object value) {
            this.columnInfo = columnInfo;
            this.value = value;
        }

        public ColumnInfo getColumnInfo() {
            return columnInfo;
        }

        public Object getValue() {
            return value;
        }

        @NotNull
        public DBNativeDataType getDataType() throws SQLException {
            DBDataType dataType = columnInfo.getDataType();
            DBNativeDataType nativeDataType = dataType.getNativeDataType();
            if (nativeDataType == null) {
                throw new SQLException("Operation not supported for " + dataType.getName());
            }
            return nativeDataType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Cell cell = (Cell) o;

            return columnInfo.getName().equals(cell.getColumnInfo().getName());

        }

        @Override
        public int hashCode() {
            return columnInfo.getName().hashCode();
        }

        public String getColumnName() {
            return columnInfo.getName();
        }
    }

    private static class Row {
        private final Set<Cell> keyCells = new HashSet<>();
        private final Set<Cell> changedCells = new HashSet<>();

        List<Cell> getKeyCells() {
            return new ArrayList<>(keyCells);
        }

        List<Cell> getChangedCells() {
            return new ArrayList<>(changedCells);
        }

        void addKeyCell(ColumnInfo columnInfo, Object value) {
            Cell cell = new Cell(columnInfo, value);
            keyCells.remove(cell);
            keyCells.add(cell);
        }

        void addChangedCell(ColumnInfo columnInfo, Object value) {
            Cell cell = new Cell(columnInfo, value);
            changedCells.remove(cell);
            changedCells.add(cell);
        }
    }

    @Override
    protected void disposeInner() {
        currentRow = null;
        connection = null;
        super.disposeInner();
    }
}
