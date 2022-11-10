package com.dci.intellij.dbn.execution.statement.result.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dci.intellij.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExecutionResultRerunAction extends AbstractExecutionResultAction {
    public ExecutionResultRerunAction() {
        super("Rerun Statement", Icons.EXEC_RESULT_RERUN);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull StatementExecutionCursorResult executionResult) {
        executionResult.reload();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable StatementExecutionCursorResult executionResult) {
        boolean enabled = false;
        if (Failsafe.check(executionResult)) {
            ResultSetTable resultTable = executionResult.getResultTable();
            if (Failsafe.check(resultTable)) {
                enabled = !resultTable.isLoading();
            }
        }

        presentation.setEnabled(enabled);
        presentation.setText("Rerun Statement");
    }
}
