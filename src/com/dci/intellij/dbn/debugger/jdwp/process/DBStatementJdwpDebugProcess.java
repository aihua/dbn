package com.dci.intellij.dbn.debugger.jdwp.process;

import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.debugger.jdwp.DBJdwpSourcePath;
import com.dci.intellij.dbn.execution.ExecutionTarget;
import com.dci.intellij.dbn.execution.statement.StatementExecutionInput;
import com.dci.intellij.dbn.execution.statement.StatementExecutionManager;
import com.dci.intellij.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugSession;
import com.sun.jdi.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.Objects;

public class DBStatementJdwpDebugProcess extends DBJdwpDebugProcess<StatementExecutionInput> {
    DBStatementJdwpDebugProcess(@NotNull XDebugSession session, @NotNull DebuggerSession debuggerSession, ConnectionHandler connectionHandler, int tcpPort) {
        super(session, debuggerSession, connectionHandler, tcpPort);
    }

    @Override
    protected void executeTarget() throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor();
        if (executionProcessor != null) {
            StatementExecutionManager statementExecutionManager = StatementExecutionManager.getInstance(getProject());
            statementExecutionManager.debugExecute(executionProcessor, getTargetConnection());
        }
    }

    @Override
    @Nullable
    public VirtualFile getVirtualFile(Location location) {

        if (location != null) {
            String sourceUrl = "<NULL>";
            try {
                sourceUrl = location.sourcePath();
                DBJdwpSourcePath sourcePath = DBJdwpSourcePath.from(sourceUrl);
                String programType = sourcePath.getProgramType();
                if (Objects.equals(programType, "Block")) {
                    StatementExecutionProcessor executionProcessor = getExecutionProcessor();
                    if (executionProcessor != null) {
                        return executionProcessor.getVirtualFile();
                    }
                }
            } catch (Exception e) {
                getConsole().warning("Error evaluating suspend position '" + sourceUrl + "': " + Commons.nvl(e.getMessage(), e.getClass().getSimpleName()));
            }
        }

        return super.getVirtualFile(location);
    }

    @Nullable
    private StatementExecutionProcessor getExecutionProcessor() {
        StatementExecutionInput executionInput = getExecutionInput();
        return executionInput == null ? null : executionInput.getExecutionProcessor();
    }

    @NotNull
    @Override
    public String getName() {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor();
        if (executionProcessor != null) {
            return executionProcessor.getName();
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
        StatementExecutionProcessor executionProcessor = getExecutionProcessor();
        if (executionProcessor != null) {
            return executionProcessor.getIcon();
        }
        return null;
    }

    @Override
    public ExecutionTarget getExecutionTarget() {
        return ExecutionTarget.STATEMENT;
    }
}
