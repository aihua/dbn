package com.dci.intellij.dbn.database.common.execution;

import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.thread.CancellableDatabaseCall;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.Resources;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.connection.SessionId;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.jdbc.DBNPreparedStatement;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.dci.intellij.dbn.execution.ExecutionContext;
import com.dci.intellij.dbn.execution.ExecutionOption;
import com.dci.intellij.dbn.execution.ExecutionOptions;
import com.dci.intellij.dbn.execution.logging.DatabaseLoggingManager;
import com.dci.intellij.dbn.execution.method.MethodExecutionInput;
import com.dci.intellij.dbn.execution.method.result.MethodExecutionResult;
import com.dci.intellij.dbn.object.DBArgument;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class MethodExecutionProcessorImpl implements MethodExecutionProcessor {
    private final DBObjectRef<DBMethod> method;

    protected MethodExecutionProcessorImpl(DBMethod method) {
        this.method = DBObjectRef.of(method);
    }

    @Override
    @NotNull
    public DBMethod getMethod() {
        return DBObjectRef.ensure(method);
    }

    public List<DBArgument> getArguments() {
        DBMethod method = getMethod();
        return method.getArguments();
    }

    protected int getArgumentsCount() {
        return getArguments().size();
    }

    protected DBArgument getReturnArgument() {
        DBMethod method = getMethod();
        return method.getReturnArgument();
    }


    @Override
    public void execute(MethodExecutionInput executionInput, DBDebuggerType debuggerType) throws SQLException {
        ConnectionHandler connection = getConnection();
        SessionId targetSessionId = executionInput.getTargetSessionId();
        SchemaId targetSchemaId = executionInput.getTargetSchemaId();
        DBNConnection conn = connection.getConnection(targetSessionId, targetSchemaId);

        if (targetSessionId == SessionId.POOL) {
            Resources.setAutoCommit(conn, false);
        }

        execute(executionInput, conn, debuggerType);
    }

    @Override
    public void execute(final MethodExecutionInput executionInput, @NotNull DBNConnection conn, DBDebuggerType debuggerType) throws SQLException {
        ExecutionContext context = executionInput.initExecution(debuggerType);
        ExecutionOptions options = executionInput.getOptions();
        ConnectionHandler connection = getConnection();
        SessionId targetSessionId = executionInput.getTargetSessionId();

        boolean loggingEnabled = debuggerType != DBDebuggerType.JDBC && options.is(ExecutionOption.ENABLE_LOGGING);
        Project project = getProject();
        DatabaseLoggingManager loggingManager = DatabaseLoggingManager.getInstance(project);

        try {
            String command = buildExecutionCommand(executionInput);
            DBMethod method = getMethod();
            loggingEnabled = loggingEnabled && loggingManager.supportsLogging(connection);
            if (loggingEnabled) {
                loggingEnabled = loggingManager.enableLogger(connection, conn);
            }

            DBNPreparedStatement<?> statement = isQuery() ?
                    conn.prepareStatement(command) :
                    conn.prepareCall(command);

            context.setConnection(conn);
            context.setStatement(statement);

            bindParameters(executionInput, statement);

            int timeout = debuggerType.isDebug() ?
                    executionInput.getDebugExecutionTimeout() :
                    executionInput.getExecutionTimeout();

            statement.setQueryTimeout(timeout);
            MethodExecutionResult executionResult = new CancellableDatabaseCall<MethodExecutionResult>(connection, conn, timeout, TimeUnit.SECONDS) {
                @Override
                public MethodExecutionResult execute() throws Exception {
                    statement.execute();
                    return executionInput.getExecutionResult();
                }

                @Override
                public void cancel() throws Exception {
                    Resources.cancel(statement);
                }
            }.start();

            if (executionResult != null) {
                loadValues(executionResult, statement);
                executionResult.calculateExecDuration();

                if (loggingEnabled) {
                    String logOutput = loggingManager.readLoggerOutput(connection, conn);
                    executionResult.setLogOutput(logOutput);
                }
            }

            if (targetSessionId != SessionId.POOL) conn.notifyDataChanges(method.getVirtualFile());
        } catch (SQLException e) {
            Resources.cancel(context.getStatement());
            throw e;
        } finally {
            if (loggingEnabled) {
                loggingManager.disableLogger(connection, conn);
            }

            if (options.is(ExecutionOption.COMMIT_AFTER_EXECUTION)) {
                Resources.commitSilently(conn);
            }

            if (conn.isDebugConnection()) {
                Resources.close(conn);

            } else if (conn.isPoolConnection()) {
                connection.freePoolConnection(conn);
            }
        }
    }

    @NotNull
    private ConnectionHandler getConnection() {
        return getMethod().getConnection();
    }

    protected boolean isQuery() {
        return false;
    }

    protected void bindParameters(MethodExecutionInput executionInput, PreparedStatement preparedStatement) throws SQLException {
        for (DBArgument argument : getArguments()) {
            DBDataType dataType = argument.getDataType();
            if (argument.isInput()) {
                String stringValue = executionInput.getInputValue(argument);
                setParameterValue(preparedStatement, argument.getPosition(), dataType, stringValue);
            }
            if (argument.isOutput() && preparedStatement instanceof CallableStatement) {
                CallableStatement callableStatement = (CallableStatement) preparedStatement;
                callableStatement.registerOutParameter(argument.getPosition(), dataType.getSqlType());
            }
        }
    }

    public void loadValues(MethodExecutionResult executionResult, DBNPreparedStatement<?> preparedStatement) throws SQLException {
        for (DBArgument argument : getArguments()) {
            if (argument.isOutput() && preparedStatement instanceof CallableStatement) {
                CallableStatement callableStatement = (CallableStatement) preparedStatement;
                Object result = callableStatement.getObject(argument.getPosition());
                executionResult.addArgumentValue(argument, result);
            }
        }
    }

    private Project getProject() {
        DBMethod method = getMethod();
        return method.getProject();
    }

    protected void setParameterValue(PreparedStatement preparedStatement, int parameterIndex, DBDataType dataType, String stringValue) throws SQLException {
        try {
            Object value = null;
            if (Strings.isNotEmptyOrSpaces(stringValue))  {
                Formatter formatter = Formatter.getInstance(getProject());
                value = formatter.parseObject(dataType.getTypeClass(), stringValue);
                value = dataType.getNativeType().getDefinition().convert(value);
            }
            dataType.setValueToPreparedStatement(preparedStatement, parameterIndex, value);

        } catch (SQLException e) {
            throw e;
        }  catch (Exception e) {
            throw new SQLException("Invalid value for data type " + dataType.getName() + " provided: \"" + stringValue + "\"");
        }
    }

    public abstract String buildExecutionCommand(MethodExecutionInput executionInput) throws SQLException;
}