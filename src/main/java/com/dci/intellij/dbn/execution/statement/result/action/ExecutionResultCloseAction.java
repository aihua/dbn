package com.dci.intellij.dbn.execution.statement.result.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.execution.ExecutionManager;
import com.dci.intellij.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ExecutionResultCloseAction extends AbstractExecutionResultAction {
    public ExecutionResultCloseAction() {
        super("Close", Icons.EXEC_RESULT_CLOSE);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull StatementExecutionCursorResult executionResult) {
        ExecutionManager executionManager = ExecutionManager.getInstance(project);
        executionManager.removeResultTab(executionResult);
    }
}
