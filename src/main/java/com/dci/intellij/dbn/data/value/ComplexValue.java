package com.dci.intellij.dbn.data.value;

import com.dci.intellij.dbn.data.type.GenericDataType;
import org.jetbrains.annotations.Nullable;

import java.sql.*;

import static com.dci.intellij.dbn.common.util.Commons.nvl;

public class ComplexValue extends ValueAdapter<String> implements Comparable<ComplexValue>{
    private String displayValue;
    private Object value;

    public ComplexValue() {
    }

    public ComplexValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        this.value = callableStatement.getObject(parameterIndex);
        this.displayValue = callableStatement.getString(parameterIndex);
    }

    public ComplexValue(ResultSet resultSet, int columnIndex) throws SQLException {
        this.value = resultSet.getObject(columnIndex);
        this.displayValue = resultSet.getString(columnIndex);
    }

    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.COMPLEX;
    }

    @Nullable
    @Override
    public String read() throws SQLException {
        return displayValue;
    }

    @Nullable
    @Override
    public String export() throws SQLException {
        return read();
    }

    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, @Nullable String value) throws SQLException {
        try {
            if (value == null) {
                preparedStatement.setObject(parameterIndex, null);
            } else {
                preparedStatement.setString(parameterIndex, value);
            }
        } catch (Throwable e) {
            if (e instanceof SQLException) throw (SQLException) e;
            throw new SQLException("Could not write array value. Your JDBC driver may not support this feature", e);
        }

    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, @Nullable String value) throws SQLException {
        try {
            if (value == null) {
                resultSet.updateObject(columnIndex, null);
            } else {
                resultSet.updateString(columnIndex, value);
            }
        } catch (Throwable e) {
            if (e instanceof SQLException) throw (SQLException) e;
            throw new SQLException("Could not write array value. Your JDBC driver may not support this feature", e);
        }
    }

    @Override
    public String getDisplayValue() {
        return displayValue;
    }

    @Override
    public String toString() {
        return getDisplayValue();
    }

    @Override
    public int compareTo(ComplexValue that) {
        return nvl(this.displayValue, "").compareTo(nvl(that.displayValue, ""));
    }
}
