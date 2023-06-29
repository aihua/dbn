package com.dci.intellij.dbn.database.common.statement;

import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.common.util.Compactables;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.Resources;
import com.dci.intellij.dbn.connection.jdbc.*;
import com.dci.intellij.dbn.database.DatabaseActivityTrace;
import com.dci.intellij.dbn.database.DatabaseCompatibility;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaces;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.booleanAttribute;
import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.stringAttribute;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.isDatabaseAccessDebug;

@Slf4j
public class StatementExecutionProcessor {
    public static final SQLFeatureNotSupportedException NO_STATEMENT_DEFINITION_EXCEPTION = new SQLFeatureNotSupportedException("No statement definition found");

    private final DatabaseInterfaces interfaces;
    private final String id;
    private final boolean query;
    private final boolean prepared;
    private int timeout = 30;
    private List<StatementDefinition> statementDefinitions = new ArrayList<>();


    public StatementExecutionProcessor(Element element, DatabaseInterfaces interfaces) {
        this.interfaces = interfaces;
        this.id = stringAttribute(element, "id");
        this.query = booleanAttribute(element, "is-query", false);
        this.prepared = booleanAttribute(element, "is-prepared-statement", false);
        String customTimeout = element.getAttributeValue("timeout");
        if (Strings.isNotEmpty(customTimeout)) {
            timeout = Integer.parseInt(customTimeout);
        }

        List<Element> children = element.getChildren();
        if (children.isEmpty()) {
            String statementText = element.getTextTrim();
            readStatements(statementText, null);
        } else {
            for (Element child : children) {
                String statementText = child.getTextTrim();
                String prefixes = child.getAttributeValue("prefixes");
                readStatements(statementText, prefixes);
            }
        }
        statementDefinitions = Compactables.compact(statementDefinitions);
    }

    private void readStatements(String statementText, String prefixes) {
        if (prefixes == null) {
            StatementDefinition statementDefinition = new StatementDefinition(statementText, null, prepared, false);
            statementDefinitions.add(statementDefinition);
        } else {
            StringTokenizer tokenizer = new StringTokenizer(prefixes, ",");
            while (tokenizer.hasMoreTokens()) {
                String prefix = tokenizer.nextToken().trim();
                boolean hasFallback = tokenizer.hasMoreTokens();
                StatementDefinition statementDefinition = new StatementDefinition(statementText, prefix, prepared, hasFallback);
                statementDefinitions.add(statementDefinition);
            }
        }
    }

    public DatabaseInterfaces getInterfaces() {
        return interfaces;
    }

    public String getId() {
        return id;
    }

    public ResultSet executeQuery(DBNConnection connection, boolean forceExecution, Object... arguments) throws SQLException {
        StatementExecutorContext context = createContext(connection);
        if (statementDefinitions.size() == 1) {
            return executeQuery(statementDefinitions.get(0), context, forceExecution, arguments);
        } else {
            SQLException exception = NO_STATEMENT_DEFINITION_EXCEPTION;
            for (StatementDefinition statementDefinition : statementDefinitions) {
                try {
                    return executeQuery(statementDefinition, context, forceExecution, arguments);
                } catch (SQLException e){
                    conditionallyLog(e);
                    exception = e;
                }
            }
            throw exception;
        }
    }

    private ResultSet executeQuery(
            @NotNull StatementDefinition definition,
            @NotNull StatementExecutorContext context,
            boolean force,
            Object... arguments) throws SQLException {

        boolean hasFallback = definition.hasFallback();
        DatabaseCompatibility compatibility = ConnectionHandler.local().getCompatibility();
        DatabaseActivityTrace activityTrace = compatibility.getActivityTrace(definition.getId());

        if (force || activityTrace.canExecute(hasFallback)) {
            return StatementExecutor.execute(context,
                    () -> {
                        DBNStatement statement = null;
                        ResultSet resultSet = null;
                        String statementText = null;
                        try {
                            activityTrace.init();
                            if (isDatabaseAccessDebug()) {
                                statementText = definition.prepareStatementText(arguments);
                                log.info("[DBN] Executing statement: " + statementText);
                            }

                            DBNConnection connection = context.getConnection();
                            if (prepared) {
                                DBNPreparedStatement preparedStatement = definition.prepareStatement(connection, arguments);
                                statement = preparedStatement;
                                context.setStatement(statement);
                                preparedStatement.setQueryTimeout(timeout);
                                resultSet = preparedStatement.executeQuery();
                                context.log("FETCH_BLOCK", false, false, resultSet.getFetchSize());
                                DBNResultSet.setIdentifier(resultSet, context.getIdentifier());
                                return resultSet;
                            } else {
                                if (statementText == null)
                                    statementText = definition.prepareStatementText(arguments);
                                statement = connection.createStatement();
                                context.setStatement(statement);
                                statement.setQueryTimeout(timeout);
                                statement.execute(statementText);
                                if (query) {
                                    try {
                                        resultSet = statement.getResultSet();
                                        context.log("FETCH_BLOCK", false, false, resultSet.getFetchSize());
                                        DBNResultSet.setIdentifier(resultSet, context.getIdentifier());
                                        return resultSet;
                                    } catch (SQLException e) {
                                        conditionallyLog(e);
                                        Resources.close(statement);
                                        return null;
                                    }
                                } else {
                                    Resources.close(statement);
                                    return null;
                                }
                            }
                        } catch (SQLException e) {
                            conditionallyLog(e);
                            Resources.close(statement);
                            String message = e.getMessage();
                            if (isDatabaseAccessDebug()) log.warn("[DBN] Error executing statement: " + statementText + "\nCause: " + message);

                            boolean isModelException = interfaces.getMessageParserInterface().isModelException(e);
                            SQLException traceException =
                                    isModelException ?
                                            new SQLException("Model exception received while executing query '" + id +"'. " + message) :
                                            new SQLException("Too many failed attempts of executing query '" + id +"'. " + message);

                            activityTrace.fail(traceException, isModelException);
                            throw e;
                        } finally {
                            activityTrace.release();
                            if (resultSet == null && statement != null) {
                                if (statement.isCached()) {
                                    statement.park();
                                } else {
                                    Resources.close(statement);
                                }

                            }
                        }
                    });
        } else {
            throw Commons.nvl(
                    activityTrace.getException(),
                    () -> new SQLException("Too many failed attempts of executing query '" + id + "'."));
        }
    }

    public <T extends CallableStatementOutput> T executeCall(
            @NotNull DBNConnection connection,
            @Nullable T outputReader,
            Object... arguments) throws SQLException {

        StatementExecutorContext context = createContext(connection);
        if (statementDefinitions.size() == 1) {
            StatementDefinition definition = statementDefinitions.get(0);
            return executeCall(definition, context, outputReader, arguments);
        } else {
            SQLException exception = NO_STATEMENT_DEFINITION_EXCEPTION;
            for (StatementDefinition definition : statementDefinitions) {
                try {
                    return executeCall(definition, context, outputReader, arguments);
                } catch (SQLException e){
                    conditionallyLog(e);
                    exception = e;
                }

            }
            throw exception;
        }
    }

    private <T extends CallableStatementOutput> T executeCall(
            @NotNull StatementDefinition definition,
            @NotNull StatementExecutorContext context,
            @Nullable T outputReader,
            Object... arguments) throws SQLException {

        return StatementExecutor.execute(context,
                () -> {
                    String statementText = definition.prepareStatementText(arguments);
                    if (isDatabaseAccessDebug()) log.info("[DBN] Executing statement: " + statementText);

                    DBNConnection connection = context.getConnection();
                    DBNCallableStatement statement = connection.prepareCall(statementText);
                    context.setStatement(statement);
                    try {
                        if (outputReader != null) outputReader.registerParameters(statement);
                        statement.setQueryTimeout(timeout);
                        statement.execute();
                        if (outputReader != null) outputReader.read(statement);
                        return outputReader;
                    } catch (SQLException e) {
                        conditionallyLog(e);
                        if (isDatabaseAccessDebug())
                            log.warn("[DBN] Error executing statement: " + statementText + "\nCause: " + e.getMessage());

                        throw e;
                    } finally {
                        Resources.close(statement);
                    }
                });
    }

    public void executeUpdate(DBNConnection connection, Object... arguments) throws SQLException {
        StatementExecutorContext context = createContext(connection);
        if (statementDefinitions.size() == 1) {
            executeUpdate(statementDefinitions.get(0), context, arguments);
        } else {
            SQLException exception = NO_STATEMENT_DEFINITION_EXCEPTION;
            for (StatementDefinition statementDefinition : statementDefinitions) {
                try {
                    executeUpdate(statementDefinition, context, arguments);
                    return;
                } catch (SQLException e){
                    conditionallyLog(e);
                    exception = e;
                }
            }
            throw exception;
        }
    }

    private void executeUpdate(
            @NotNull StatementDefinition definition,
            @NotNull StatementExecutorContext context,
            Object... arguments) throws SQLException {
        StatementExecutor.execute(context,
                () -> {
                    String statementText = definition.prepareStatementText(arguments);
                    if (isDatabaseAccessDebug()) log.info("[DBN] Executing statement: " + statementText);

                    DBNConnection connection = context.getConnection();
                    DBNStatement statement = connection.createStatement();
                    context.setStatement(statement);
                    try {
                        statement.setQueryTimeout(timeout);
                        statement.executeUpdate(statementText);
                    } catch (SQLException e) {
                        conditionallyLog(e);
                        if (isDatabaseAccessDebug())
                            log.warn("[DBN] Error executing statement: " + statementText + "\nCause: " + e.getMessage());

                        throw e;
                    } finally {
                        Resources.close(statement);
                    }
                    return null;
                });
    }

    public boolean executeStatement(@NotNull DBNConnection connection, Object... arguments) throws SQLException {
        StatementExecutorContext context = createContext(connection);
        if (statementDefinitions.size() == 1) {
            return executeStatement(statementDefinitions.get(0), context, arguments);
        } else {
            SQLException exception = NO_STATEMENT_DEFINITION_EXCEPTION;
            for (StatementDefinition statementDefinition : statementDefinitions) {
                try {
                    return executeStatement(statementDefinition, context, arguments);
                } catch (SQLException e){
                    conditionallyLog(e);
                    exception = e;
                }
            }
            throw exception;
        }
    }

    private boolean executeStatement(
            @NotNull StatementDefinition definition,
            @NotNull StatementExecutorContext context,
            Object... arguments) throws SQLException {
        return StatementExecutor.execute(context,
                () -> {
                    String statementText = definition.prepareStatementText(arguments);
                    if (isDatabaseAccessDebug()) log.info("[DBN] Executing statement: " + statementText);

                    DBNConnection connection = context.getConnection();
                    DBNStatement statement = connection.createStatement();
                    context.setStatement(statement);
                    try {
                        statement.setQueryTimeout(timeout);
                        return statement.execute(statementText);
                    } catch (SQLException e) {
                        conditionallyLog(e);
                        if (isDatabaseAccessDebug())
                            log.warn("[DBN] Error executing statement: " + statementText + "\nCause: " + e.getMessage());

                        throw e;
                    } finally {
                        Resources.close(statement);
                    }
                });
    }

    @NotNull
    public StatementExecutorContext createContext(@NotNull DBNConnection connection) {
        return new StatementExecutorContext(connection, id, timeout);
    }
}
