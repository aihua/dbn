package com.dci.intellij.dbn.database.common.util;

import com.dci.intellij.dbn.common.util.Safe;
import com.dci.intellij.dbn.common.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.dci.intellij.dbn.database.common.util.CachedResultSet.Columns;

public class CachedResultSetRow {
    private final Map<String, Object> values = new HashMap<>();

    private CachedResultSetRow(@Nullable ResultSet source, List<String> columnNames) throws SQLException {
        if (source != null) {
            for (String columnName : columnNames) {
                Object columnValue = source.getObject(columnName);
                values.put(columnName, columnValue);
            }
        }
    }

    public static CachedResultSetRow create(ResultSet source, List<String> columnNames) throws SQLException {
        return new CachedResultSetRow(source, columnNames);
    }

    public Object get(String columnName) {
        return values.get(columnName);
    }


    public boolean matches(CachedResultSetRow that, Columns columns) {
        for (String columnName : columns.names()) {
            Object thisColumnValue = this.get(columnName);
            Object thatColumnValue = that.get(columnName);
            if (!Safe.equal(thisColumnValue, thatColumnValue)) {
                return false;
            }
        }
        return true;
    }

    public CachedResultSetRow clone(Columns columns) throws SQLException {
        CachedResultSetRow clone = new CachedResultSetRow(null, null);
        String[] columnNames = columns.names();
        for (String columnName : values.keySet()) {
            if (StringUtil.isOneOf(columnName, columnNames)) {
                Object columnValue = get(columnName);
                clone.values.put(columnName, columnValue);
            }
        }
        return clone;
    }

    void extend(String columnName, Object columnValue){
        values.put(columnName, columnValue);
    }

    void normalize(CachedResultSet.Mapper<String> columnMapper) {
        for (String columnName : new HashSet<>(values.keySet())) {
            String newColumnName = columnMapper.map(columnName);
            if (newColumnName != null && !newColumnName.equals(columnName)) {
                Object columnValue = values.remove(columnName);
                values.put(newColumnName, columnValue);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Object column : values.values()) {
            if (builder.length() > 0) builder.append(" / ");
            builder.append(column);
        }

        return builder.toString();
    }
}
