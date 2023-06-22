package com.dci.intellij.dbn.database.mysql;

import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.common.DatabaseMetadataInterfaceImpl;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaces;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import static com.dci.intellij.dbn.common.dispose.Failsafe.conditionallyLog;


public class MySqlMetadataInterface extends DatabaseMetadataInterfaceImpl {

    MySqlMetadataInterface(DatabaseInterfaces provider) {
        super("mysql_metadata_interface.xml", provider);
    }

    @Override
    public ResultSet loadCompileObjectErrors(String ownerName, String objectName, DBNConnection connection) throws SQLException {
        return null;
    }

    @Override
    public ResultSet loadMethodArguments(String ownerName, String methodName, String methodType, short overload, DBNConnection connection) throws SQLException {
        try {
            return super.loadMethodArguments(ownerName, methodName, methodType, overload, connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            ResultSet resultSet = executeQuery(connection, "alternative-method-arguments", ownerName, methodName, methodType, overload);
            return new MySqlArgumentsResultSet(resultSet);
        }
    }

    @Override
    public ResultSet loadAllMethodArguments(String ownerName, DBNConnection connection) throws SQLException {
        try {
            return super.loadAllMethodArguments(ownerName, connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            ResultSet resultSet = executeQuery(connection, "alternative-all-method-arguments", ownerName);
            return new MySqlArgumentsResultSet(resultSet);
        }
    }

    @Override
    public String createDateString(Date date) {
        String dateString = META_DATE_FORMAT.get().format(date);
        return "str_to_date('" + dateString + "', '%Y-%m-%d %T')";
    }

    @Override
    public void terminateSession(Object sessionId, Object serialNumber, boolean immediate, DBNConnection connection) throws SQLException {
        executeStatement(connection, "kill-session", sessionId);
    }
}