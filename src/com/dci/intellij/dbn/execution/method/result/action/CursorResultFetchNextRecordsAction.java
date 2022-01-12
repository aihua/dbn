package com.dci.intellij.dbn.execution.method.result.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dci.intellij.dbn.data.model.resultSet.ResultSetDataModel;
import com.dci.intellij.dbn.execution.common.options.ExecutionEngineSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class CursorResultFetchNextRecordsAction extends MethodExecutionCursorResultAction {
    public CursorResultFetchNextRecordsAction() {
        super("Fetch Next Records", Icons.EXEC_RESULT_RESUME);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ResultSetTable resultSetTable = getResultSetTable(e);
        if (Failsafe.check(resultSetTable)) {
            Progress.prompt(project, "Loading cursor result records", false,
                    (progress) -> {
                        try {
                            ResultSetDataModel model = resultSetTable.getModel();
                            if (!model.isResultSetExhausted()) {
                                ExecutionEngineSettings settings = ExecutionEngineSettings.getInstance(project);
                                int fetchBlockSize = settings.getStatementExecutionSettings().getResultSetFetchBlockSize();

                                model.fetchNextRecords(fetchBlockSize, false);
                            }

                        } catch (SQLException ex) {
                            Messages.showErrorDialog(project, "Could not perform operation.", ex);
                        }

                    });
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        super.update(e, project);
        ResultSetTable resultSetTable = getResultSetTable(e);
        Presentation presentation = e.getPresentation();
        presentation.setText("Fetch Next Records");
        if (resultSetTable != null) {
            ResultSetDataModel model = resultSetTable.getModel();
            boolean enabled = !model.isResultSetExhausted();
            presentation.setEnabled(enabled);
        } else {
            presentation.setEnabled(false);
        }
    }
}
