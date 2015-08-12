package com.dci.intellij.dbn.debugger.jdwp.process;

import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.dci.intellij.dbn.execution.ExecutionTarget;
import com.dci.intellij.dbn.execution.method.MethodExecutionInput;
import com.dci.intellij.dbn.execution.method.MethodExecutionManager;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;

public class DBMethodJdwpDebugProcess extends DBJdwpDebugProcess<MethodExecutionInput> {
    public DBMethodJdwpDebugProcess(@NotNull XDebugSession session, @NotNull DebuggerSession debuggerSession, ConnectionHandler connectionHandler, int tcpPort) {
        super(session, debuggerSession, connectionHandler, tcpPort);
    }

    @NotNull
    @Override
    public String getName() {
        MethodExecutionInput executionInput = getExecutionInput();
        if (executionInput != null) {
            DBMethod method = executionInput.getMethod();
            DBSchemaObject object = getMainDatabaseObject(method);
            if (object != null) {
                return object.getQualifiedName();
            }
        }
        return "Debug Process";
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        MethodExecutionInput executionInput = getExecutionInput();
        if (executionInput != null) {
            DBMethod method = executionInput.getMethod();
            DBSchemaObject object = getMainDatabaseObject(method);
            if (object != null) {
                return object.getIcon();
            }
        }
        return null;
    }

    @Nullable
    protected DBSchemaObject getMainDatabaseObject(DBMethod method) {
        return method != null && method.isProgramMethod() ? method.getProgram() : method;
    }

    @Override
    protected void executeTarget() throws SQLException {
        MethodExecutionInput executionInput = getExecutionInput();
        if (executionInput != null) {
            MethodExecutionManager methodExecutionManager = MethodExecutionManager.getInstance(getProject());
            methodExecutionManager.debugExecute(executionInput, getTargetConnection(), DBDebuggerType.JDWP);
        }
    }

    @Override
    protected void releaseTargetConnection() {
        targetConnection = null;
    }

    @Override
    public ExecutionTarget getExecutionTarget() {
        return ExecutionTarget.METHOD;
    }

    @NotNull
    @Override
    public XDebuggerEditorsProvider getEditorsProvider() {
        return null;
    }

    @Override
    public void startStepOver() {

    }

    @Override
    public void startStepInto() {

    }

    @Override
    public void startStepOut() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void runToPosition(@NotNull XSourcePosition position) {

    }
}
