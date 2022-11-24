package com.dci.intellij.dbn.data.record;


import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.PooledConnection;
import com.dci.intellij.dbn.connection.Resources;
import com.dci.intellij.dbn.connection.jdbc.DBNPreparedStatement;
import com.dci.intellij.dbn.connection.jdbc.DBNResultSet;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.editor.data.filter.DatasetFilterInput;
import com.dci.intellij.dbn.object.DBColumn;
import com.dci.intellij.dbn.object.DBDataset;
import com.intellij.openapi.Disposable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dci.intellij.dbn.common.util.Lists.isLast;

public class DatasetRecord implements Disposable {
    private DatasetFilterInput filterInput;
    private Map<String, Object> values = new HashMap<>();

    public DatasetRecord(DatasetFilterInput filterInput) throws SQLException {
        this.filterInput = filterInput;
        loadRecordValues(filterInput);
    }

    public DatasetFilterInput getFilterInput() {
        return filterInput;
    }

    private void loadRecordValues(DatasetFilterInput filterInput) throws SQLException {
        DBDataset dataset = getDataset();
        StringBuilder selectStatement = new StringBuilder();
        selectStatement.append("select ");

        List<DBColumn> columns = dataset.getColumns();
        for (DBColumn column : columns) {
            selectStatement.append(column.getName());
            if (!isLast(columns, column)) {
                selectStatement.append(", ");
            }
        }

        selectStatement.append(" from ");
        selectStatement.append(dataset.getQualifiedName());
        selectStatement.append(" where ");

        List<DBColumn> filterColumns = filterInput.getColumns();
        for (DBColumn column : filterColumns) {
            selectStatement.append(column.getName());
            selectStatement.append(" = ? ");
            if (!isLast(filterColumns, column)) {
                selectStatement.append(" and ");
            }
        }

        ConnectionHandler connection = dataset.getConnection();
        PooledConnection.run(connection.createInterfaceContext(), conn -> {
            DBNPreparedStatement statement = null;
            DBNResultSet resultSet = null;
            try {
                statement = conn.prepareStatement(selectStatement.toString());

                int index = 1;

                for (DBColumn column : filterColumns) {
                    Object value = filterInput.getColumnValue(column);
                    DBDataType dataType = column.getDataType();
                    dataType.setValueToPreparedStatement(statement, index, value);
                    index++;
                }

                resultSet = statement.executeQuery();
                try {
                    if (resultSet.next()) {
                        index = 1;

                        for (DBColumn column : dataset.getColumns()) {
                            DBDataType dataType = column.getDataType();
                            Object value = dataType.getValueFromResultSet(resultSet, index);
                            values.put(column.getName(), value);
                            index++;
                        }
                    }
                } finally {
                    conn.updateLastAccess();
                }
            } finally {
                Resources.close(resultSet);
                Resources.close(statement);
            }
        });
    }

    public DBDataset getDataset() {
        return filterInput.getDataset();
    }

    public Object getColumnValue(DBColumn column) {
        return values.get(column.getName());
    }

    @Override
    public void dispose() {
        filterInput = null;
        values.clear();
        values = null;
    }
}
