package com.dci.intellij.dbn.database.common;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.util.XmlContents;
import com.dci.intellij.dbn.connection.DatabaseType;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.common.statement.CallableStatementOutput;
import com.dci.intellij.dbn.database.common.statement.StatementExecutionProcessor;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterface;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaces;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;

public abstract class DatabaseInterfaceBase implements DatabaseInterface{
    private final String fileName;
    private final DatabaseInterfaces interfaces;
    protected Map<String, StatementExecutionProcessor> processors = new HashMap<>();

    public DatabaseInterfaceBase(String fileName, DatabaseInterfaces interfaces) {
        this.fileName = fileName;
        this.interfaces = interfaces;
        reset();
    }

    @Override
    public void reset() {
        processors.clear();
        Element root = loadDefinition();
        for (Element child : root.getChildren()) {
            StatementExecutionProcessor executionProcessor = new StatementExecutionProcessor(child, interfaces);
            String id = executionProcessor.getId();
            processors.put(id, executionProcessor);
        }
    }

    @SneakyThrows
    private Element loadDefinition() {
        return XmlContents.fileToElement(getClass(), fileName);
    }

    protected ResultSet executeQuery(@NotNull DBNConnection connection, String loaderId, @Nullable Object... arguments) throws SQLException {
        return executeQuery(connection, true, loaderId, arguments);
    }

    protected ResultSet executeQuery(@NotNull DBNConnection connection, boolean forceExecution, String loaderId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(loaderId);
        ResultSet result = executionProcessor.executeQuery(connection, forceExecution, arguments);
        checkDisposed(connection);
        return result;
    }

    protected <T extends CallableStatementOutput> T executeCall(@NotNull DBNConnection connection, @Nullable T outputReader, String loaderId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(loaderId);
        T result = executionProcessor.executeCall(connection, outputReader, arguments);
        checkDisposed(connection);
        return result;
    }

    protected boolean executeStatement(@NotNull DBNConnection connection, String loaderId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(loaderId);
        boolean result = executionProcessor.executeStatement(connection, arguments);
        checkDisposed(connection);
        return result;
    }

    protected void executeUpdate(@NotNull DBNConnection connection, String loaderId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(loaderId);
        executionProcessor.executeUpdate(connection, arguments);
        checkDisposed(connection);
    }

    @NotNull
    private StatementExecutionProcessor getExecutionProcessor(String loaderId) throws SQLException {
        StatementExecutionProcessor executionProcessor = processors.get(loaderId);
        if (executionProcessor == null) {
            DatabaseType databaseType = interfaces.getDatabaseType();
            throw new SQLFeatureNotSupportedException("Feature [" + loaderId + "] not implemented / supported for " + databaseType.getName() + " database type");
        }
        return executionProcessor;
    }

    private void checkDisposed(DBNConnection connection) {
        Failsafe.nd(connection.getProject());
    }

    public DatabaseInterfaces getInterfaces() {
        return interfaces;
    }
}
