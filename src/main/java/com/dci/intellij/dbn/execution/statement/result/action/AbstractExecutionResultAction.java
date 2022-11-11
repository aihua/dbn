package com.dci.intellij.dbn.execution.statement.result.action;

import com.dci.intellij.dbn.common.action.DataKeys;
import com.dci.intellij.dbn.common.action.DumbAwareContextAction;
import com.dci.intellij.dbn.execution.ExecutionManager;
import com.dci.intellij.dbn.execution.ExecutionResult;
import com.dci.intellij.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class AbstractExecutionResultAction extends DumbAwareContextAction<StatementExecutionCursorResult> {
    protected AbstractExecutionResultAction(String text, Icon icon) {
        super(text, null, icon);
    }

    @Nullable
    protected StatementExecutionCursorResult getTarget(@NotNull AnActionEvent e) {
        StatementExecutionCursorResult result = e.getData(DataKeys.STATEMENT_EXECUTION_CURSOR_RESULT);
        if (result == null) {
            Project project = e.getProject();
            if (project != null) {
                ExecutionManager executionManager = ExecutionManager.getInstance(project);
                ExecutionResult executionResult = executionManager.getSelectedExecutionResult();
                if (executionResult instanceof StatementExecutionCursorResult) {
                    return (StatementExecutionCursorResult) executionResult;
                }
            }
        }
        return result;
    }
}
