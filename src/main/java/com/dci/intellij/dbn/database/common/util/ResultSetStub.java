package com.dci.intellij.dbn.database.common.util;

import com.dci.intellij.dbn.common.dispose.StatefulDisposable;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * Abstract stub implementation of {@link ResultSet}
 * All methods throw {@link AbstractMethodError}
 * Concrete implementations must override the methods expected to be invoked!
 */
public interface ResultSetStub extends ResultSet, StatefulDisposable {


    @Override
    default boolean next() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void close() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean wasNull() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default String getString(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean getBoolean(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default byte getByte(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default short getShort(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default int getInt(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default long getLong(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default float getFloat(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default double getDouble(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default byte[] getBytes(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Date getDate(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Time getTime(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Timestamp getTimestamp(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default InputStream getAsciiStream(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default InputStream getUnicodeStream(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default InputStream getBinaryStream(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default String getString(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean getBoolean(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default byte getByte(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default short getShort(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default int getInt(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default long getLong(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default float getFloat(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default double getDouble(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default byte[] getBytes(String columnLabel) throws SQLException {
        return new byte[0];
    }

    @Override
    default Date getDate(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Time getTime(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Timestamp getTimestamp(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default InputStream getAsciiStream(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default InputStream getUnicodeStream(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default InputStream getBinaryStream(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default SQLWarning getWarnings() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void clearWarnings() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default String getCursorName() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default ResultSetMetaData getMetaData() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Object getObject(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Object getObject(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default int findColumn(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Reader getCharacterStream(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Reader getCharacterStream(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean isBeforeFirst() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean isAfterLast() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean isFirst() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean isLast() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void beforeFirst() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void afterLast() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean first() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean last() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default int getRow() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean absolute(int row) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean relative(int rows) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean previous() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void setFetchDirection(int direction) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default int getFetchDirection() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void setFetchSize(int rows) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default int getFetchSize() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default int getType() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default int getConcurrency() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean rowUpdated() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean rowInserted() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean rowDeleted() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNull(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateByte(int columnIndex, byte x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateShort(int columnIndex, short x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateInt(int columnIndex, int x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateLong(int columnIndex, long x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateFloat(int columnIndex, float x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateDouble(int columnIndex, double x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateString(int columnIndex, String x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateDate(int columnIndex, Date x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateTime(int columnIndex, Time x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateObject(int columnIndex, Object x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNull(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBoolean(String columnLabel, boolean x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateByte(String columnLabel, byte x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateShort(String columnLabel, short x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateInt(String columnLabel, int x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateLong(String columnLabel, long x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateFloat(String columnLabel, float x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateDouble(String columnLabel, double x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateString(String columnLabel, String x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBytes(String columnLabel, byte[] x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateDate(String columnLabel, Date x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateTime(String columnLabel, Time x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateObject(String columnLabel, Object x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void insertRow() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateRow() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void deleteRow() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void refreshRow() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void cancelRowUpdates() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void moveToInsertRow() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void moveToCurrentRow() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Statement getStatement() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Ref getRef(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Blob getBlob(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Clob getClob(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Array getArray(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Ref getRef(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Blob getBlob(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Clob getClob(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Array getArray(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Date getDate(int columnIndex, Calendar cal) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Date getDate(String columnLabel, Calendar cal) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Time getTime(int columnIndex, Calendar cal) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Time getTime(String columnLabel, Calendar cal) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default URL getURL(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default URL getURL(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateRef(int columnIndex, Ref x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateRef(String columnLabel, Ref x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBlob(String columnLabel, Blob x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateClob(int columnIndex, Clob x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateClob(String columnLabel, Clob x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateArray(int columnIndex, Array x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateArray(String columnLabel, Array x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default RowId getRowId(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default RowId getRowId(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default int getHoldability() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean isClosed() throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNString(int columnIndex, String nString) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNString(String columnLabel, String nString) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default NClob getNClob(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default NClob getNClob(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default SQLXML getSQLXML(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default String getNString(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default String getNString(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Reader getNCharacterStream(int columnIndex) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default Reader getNCharacterStream(String columnLabel) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default <T> T unwrap(Class<T> iface) throws SQLException {
        throw new AbstractMethodError();
    }

    @Override
    default boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new AbstractMethodError();
    }
}
