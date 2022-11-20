package com.dci.intellij.dbn.execution.statement.result.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dci.intellij.dbn.data.model.resultSet.ResultSetDataModel;
import com.dci.intellij.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dci.intellij.dbn.common.dispose.Checks.isValid;

public class ExecutionResultResumeAction extends AbstractExecutionResultAction {
    public ExecutionResultResumeAction() {
        super("Fetch Next Records", Icons.EXEC_RESULT_RESUME);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull StatementExecutionCursorResult executionResult) {
        executionResult.fetchNextRecords();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable StatementExecutionCursorResult executionResult) {
        boolean enabled = false;

        if (isValid(executionResult) && executionResult.hasResult()) {
            ResultSetTable resultTable = executionResult.getResultTable();
            if (isValid(resultTable)) {
                ResultSetDataModel tableModel = resultTable.getModel();
                if (isValid(tableModel)) {
                    enabled = !resultTable.isLoading() && !tableModel.isResultSetExhausted();
                }
            }
        }

        presentation.setEnabled(enabled);
        presentation.setText("Fetch Next Records");
    }
}
