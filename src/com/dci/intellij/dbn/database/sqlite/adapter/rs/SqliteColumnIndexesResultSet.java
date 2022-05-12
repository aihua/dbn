package com.dci.intellij.dbn.database.sqlite.adapter.rs;

import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.sqlite.adapter.SqliteMetadataResultSetRow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static com.dci.intellij.dbn.common.cache.CacheKey.key;
import static com.dci.intellij.dbn.database.sqlite.adapter.SqliteRawMetaData.RawIndexDetailInfo;
import static com.dci.intellij.dbn.database.sqlite.adapter.SqliteRawMetaData.RawIndexInfo;

/**
 * COLUMN_NAME
 * INDEX_NAME
 * TABLE_NAME
 * POSITION
 */

public abstract class SqliteColumnIndexesResultSet extends SqliteDatasetInfoResultSetStub<SqliteColumnIndexesResultSet.IndexColumn> {
    protected SqliteColumnIndexesResultSet(String ownerName, SqliteDatasetNamesResultSet datasetNames, DBNConnection connection) throws SQLException {
        super(ownerName, datasetNames, connection);
    }

    protected SqliteColumnIndexesResultSet(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        super(ownerName, datasetName, connection);
    }

    @Override
    protected void init(String ownerName, String tableName) throws SQLException {
        RawIndexInfo indexInfo = getIndexInfo(tableName);
        for (RawIndexInfo.Row row : indexInfo.getRows()) {
            String indexName = row.getName();
            RawIndexDetailInfo indexDetailInfo = getIndexDetailInfo(indexName);
            for (RawIndexDetailInfo.Row detailRow : indexDetailInfo.getRows()) {
                String columnName = detailRow.getName();
                if (Strings.isNotEmpty(columnName)) {
                    IndexColumn indexColumn = new IndexColumn();
                    indexColumn.tableName = tableName;
                    indexColumn.indexName = indexName;
                    indexColumn.columnName = columnName;
                    indexColumn.position = detailRow.getSeqno();
                    add(indexColumn);
                }
            }
        }
    }

    private RawIndexInfo getIndexInfo(final String tableName) throws SQLException {
        return cache().get(
                key(ownerName, tableName, "INDEX_INFO"),
                () -> new RawIndexInfo(loadIndexInfo(tableName)));
    }

    private RawIndexDetailInfo getIndexDetailInfo(final String indexName) throws SQLException {
        return cache().get(
                key(ownerName,  indexName, "INDEX_DETAIL_INFO"),
                () -> new RawIndexDetailInfo(loadIndexDetailInfo(indexName)));
    }


    protected abstract ResultSet loadIndexInfo(String tableName) throws SQLException;
    protected abstract ResultSet loadIndexDetailInfo(String indexName) throws SQLException;


    @Override
    public String getString(String columnLabel) throws SQLException {
        IndexColumn element = current();
        return Objects.equals(columnLabel, "INDEX_NAME") ? element.indexName :
               Objects.equals(columnLabel, "COLUMN_NAME") ? element.columnName :
               Objects.equals(columnLabel, "TABLE_NAME") ? element.tableName : null;
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        IndexColumn element = current();
        return (short) (Objects.equals(columnLabel, "POSITION") ? element.position : 0);
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        IndexColumn element = current();
        return Objects.equals(columnLabel, "POSITION") ? element.position : 0;
    }

    public static class IndexColumn implements SqliteMetadataResultSetRow<IndexColumn> {
        private String tableName;
        private String indexName;
        private String columnName;
        private int position;

        @Override
        public String identifier() {
            return tableName + "." + indexName + "." + columnName;
        }
    }
}
