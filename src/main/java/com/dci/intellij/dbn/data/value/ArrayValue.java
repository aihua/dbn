package com.dci.intellij.dbn.data.value;

import com.dci.intellij.dbn.data.type.GenericDataType;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.dci.intellij.dbn.common.exception.Exceptions.toSqlException;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;

public class ArrayValue extends ValueAdapter<List<String>>{
    private Array array;
    private List<String> values;

    public ArrayValue() {
    }

    public ArrayValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        this.array = callableStatement.getArray(parameterIndex);
        values = readArray(array);
    }

    public ArrayValue(ResultSet resultSet, int columnIndex) throws SQLException {
        array = resultSet.getArray(columnIndex);
        values = readArray(array);
    }

    private static List<String> readArray(Array array) throws SQLException {
        List<String> values = null;
        if (array != null) {
            ResultSet arrayResultSet = array.getResultSet();
            while (arrayResultSet.next()) {
                if (values == null) values = new ArrayList<>();
                Object object = arrayResultSet.getObject(2);
                values.add(object == null ? null : object.toString());
            }
            arrayResultSet.close();
        }
        return values;
    }

    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.ARRAY;
    }

    @Nullable
    @Override
    public List<String> read() throws SQLException {
        return values;
    }

    @Nullable
    @Override
    public String export() throws SQLException {
        return values == null ? null : values.toString();
    }

    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, @Nullable List<String> values) throws SQLException {
        try {
            this.values = values;
            if (values == null) {
                preparedStatement.setArray(parameterIndex, null);
            } else {
                array = connection.createArrayOf("varchar", values.toArray());
                preparedStatement.setArray(parameterIndex, array);
            }
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e, "Could not write array value. Your JDBC driver may not support this feature");
        }

    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, @Nullable List<String> values) throws SQLException {
        try {
            this.values = values;
            if (values == null) {
                resultSet.updateArray(columnIndex, null);
            } else {
                String columnTypeName = resultSet.getMetaData().getColumnTypeName(columnIndex).substring(1);
                array = connection.createArrayOf(columnTypeName, values.toArray());
                resultSet.updateArray(columnIndex, array);
            }
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e, "Could not write array value. Your JDBC driver may not support this feature");
        }
    }

    @Override
    public String getDisplayValue() {
        return values == null ? "" : values.toString();
    }

    @Override
    public String toString() {
        return getDisplayValue();
    }
}
