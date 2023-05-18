package com.dci.intellij.dbn.database.sqlite.adapter;

import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.database.common.util.ResultSetReader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class SqliteRawMetaData {
    public static class RawForeignKeyInfo extends RawMetaData<RawForeignKeyInfo.Row> {

        public RawForeignKeyInfo(ResultSet resultSet) throws SQLException {
            super(resultSet);
        }

        @Override
        protected Row createRow(ResultSet resultSet) throws SQLException {
            return new Row(resultSet);
        }

        @Getter
        public static class Row {
            int id;
            short seq;
            String table;
            String from;
            String to;

            Row (ResultSet resultSet) throws SQLException {
                id = resultSet.getInt("id");
                seq = resultSet.getShort("seq");
                table = resultSet.getString("table");
                from = resultSet.getString("from");
                to = resultSet.getString("to");
            }
        }
    }

    public static class RawIndexInfo extends RawMetaData<RawIndexInfo.Row> {

        public RawIndexInfo(ResultSet resultSet) throws SQLException {
            super(resultSet);
        }

        @Override
        protected Row createRow(ResultSet resultSet) throws SQLException {
            return new Row(resultSet);
        }

        @Getter
        public static class Row {
            int seq;
            int unique;
            int partial;
            String name;
            String origin;

            Row (ResultSet resultSet) throws SQLException {
                seq = resultSet.getInt("seq");
                name = resultSet.getString("name");
                try {unique = resultSet.getInt("unique");} catch (SQLException ignore) {}
                try {partial = resultSet.getInt("partial");} catch (SQLException ignore) {}
                try {origin = resultSet.getString("origin");} catch (SQLException ignore) {}
            }
        }
    }

    public static class RawIndexDetailInfo extends RawMetaData<RawIndexDetailInfo.Row> {

        public RawIndexDetailInfo(ResultSet resultSet) throws SQLException {
            super(resultSet);
        }

        @Override
        protected Row createRow(ResultSet resultSet) throws SQLException {
            return new Row(resultSet);
        }

        @Getter
        public static class Row {
            short seqno;
            int cid;
            int desc;
            int key;
            String name;
            String coll;

            Row (ResultSet resultSet) throws SQLException {
                seqno = resultSet.getShort("seqno");
                cid = resultSet.getInt("cid");
                key = resultSet.getInt("key");
                name = resultSet.getString("name");
                desc = resultSet.getInt("desc");
                coll = resultSet.getString("coll");
            }
        }
    }

    public static class RawTableInfo extends RawMetaData<RawTableInfo.Row> {

        public RawTableInfo(ResultSet resultSet) throws SQLException {
            super(resultSet);
        }

        @Override
        protected Row createRow(ResultSet resultSet) throws SQLException {
            return new Row(resultSet);
        }

        @Getter
        public static class Row {
            int cid;
            String name;
            String type;
            int length;
            int precision;
            int scale;
            int notnull;
            int pk;

            Row (ResultSet resultSet) throws SQLException {
                cid = resultSet.getInt("cid");
                name = resultSet.getString("name");
                type = resultSet.getString("type");

                String[] typeTokens = type.split("[(,)]");
                if (typeTokens.length > 1) {
                    try {
                        if (typeTokens.length > 2) {
                            precision = Integer.parseInt(typeTokens[1]);
                            scale = Integer.parseInt(typeTokens[2]);
                        } else {
                            length = Integer.parseInt(typeTokens[1]);
                        }
                    } catch (Throwable ignore) {
                        log.warn("Failed to parse type " + type);
                    }
                    type = typeTokens[0];
                }

                notnull = resultSet.getInt("notnull");
                pk = resultSet.getInt("pk");
            }
        }
    }

    public static class TableNames extends RawMetaData<TableNames.Row> {

        public TableNames(ResultSet resultSet) throws SQLException {
            super(resultSet);
        }

        @Override
        protected Row createRow(ResultSet resultSet) throws SQLException {
            return new Row(resultSet);
        }

        @Getter
        public static class Row {
            String name;

            Row (ResultSet resultSet) throws SQLException {
                name = resultSet.getString("DATASET_NAME");
            }
        }
    }

    private abstract static class RawMetaData<T> extends ResultSetReader {
        private List<T> rows;
        RawMetaData(ResultSet resultSet) throws SQLException {
            super(resultSet);
        }

        @Override
        protected final void processRow(ResultSet resultSet) throws SQLException {
            T row = createRow(resultSet);
            if (rows == null) {
                rows = new ArrayList<>();
            }
            rows.add(row);
        }

        @NotNull
        public List<T> getRows() {
            return Commons.nvl(rows, Collections.emptyList());
        }

        protected abstract T createRow(ResultSet resultSet) throws SQLException;
    }
}
