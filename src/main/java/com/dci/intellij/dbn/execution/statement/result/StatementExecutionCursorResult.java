package com.dci.intellij.dbn.execution.statement.result;

import com.dci.intellij.dbn.common.action.DataKeys;
import com.dci.intellij.dbn.common.dispose.Checks;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.jdbc.DBNResultSet;
import com.dci.intellij.dbn.connection.jdbc.DBNStatement;
import com.dci.intellij.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dci.intellij.dbn.data.model.resultSet.ResultSetDataModel;
import com.dci.intellij.dbn.execution.common.options.ExecutionEngineSettings;
import com.dci.intellij.dbn.execution.statement.StatementExecutionContext;
import com.dci.intellij.dbn.execution.statement.StatementExecutionInput;
import com.dci.intellij.dbn.execution.statement.options.StatementExecutionSettings;
import com.dci.intellij.dbn.execution.statement.processor.StatementExecutionCursorProcessor;
import com.dci.intellij.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dci.intellij.dbn.execution.statement.result.ui.StatementExecutionResultForm;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

import static com.dci.intellij.dbn.common.dispose.Failsafe.nd;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dci.intellij.dbn.execution.ExecutionStatus.EXECUTING;

public class StatementExecutionCursorResult extends StatementExecutionBasicResult {
    private ResultSetDataModel<?, ?> dataModel;

    public StatementExecutionCursorResult(
            @NotNull StatementExecutionProcessor executionProcessor,
            @NotNull String resultName,
            DBNResultSet resultSet,
            int updateCount) throws SQLException {
        super(executionProcessor, resultName, updateCount);

        ConnectionHandler connection = nd(executionProcessor.getConnection());
        int fetchBlockSize = executionProcessor.getExecutionInput().getResultSetFetchBlockSize();
        dataModel = new ResultSetDataModel<>(resultSet, connection, fetchBlockSize);
    }

    private StatementExecutionSettings getQueryExecutionSettings() {
        ExecutionEngineSettings settings = ExecutionEngineSettings.getInstance(getProject());
        return settings.getStatementExecutionSettings();
    }

    public StatementExecutionCursorResult(
            StatementExecutionProcessor executionProcessor,
            @NotNull String resultName,
            int updateCount) throws SQLException {
        super(executionProcessor, resultName, updateCount);
    }

    @Override
    @NotNull
    public StatementExecutionCursorProcessor getExecutionProcessor() {
        return (StatementExecutionCursorProcessor) super.getExecutionProcessor();
    }

    public void reload() {
        StatementExecutionCursorProcessor executionProcessor = getExecutionProcessor();
        ConnectionAction.invoke("Reload data", false, executionProcessor, action -> {
            Progress.background(getProject(), action, false,
                    "Loading data",
                    "Reloading result for " + executionProcessor.getStatementName(),
                    progress -> {
                        StatementExecutionResultForm resultForm = getForm();
                        if (Checks.isValid(resultForm)) {
                            StatementExecutionContext context = executionProcessor.initExecutionContext();
                            context.set(EXECUTING, true);

                            try {
                                resultForm.highlightLoading(true);
                                StatementExecutionInput executionInput = getExecutionInput();
                                try {
                                    ConnectionHandler connection = getConnection();
                                    SchemaId currentSchema = getDatabaseSchema();
                                    DBNConnection conn = connection.getMainConnection(currentSchema);
                                    DBNStatement<?> statement = conn.createStatement();
                                    statement.setQueryTimeout(executionInput.getExecutionTimeout());
                                    statement.setFetchSize(executionInput.getResultSetFetchBlockSize());
                                    statement.execute(executionInput.getExecutableStatementText());
                                    DBNResultSet resultSet = statement.getResultSet();
                                    if (resultSet != null) {
                                        loadResultSet(resultSet);
                                    }
                                } catch (final SQLException e) {
                                    conditionallyLog(e);
                                    Messages.showErrorDialog(getProject(), "Could not perform reload operation.", e);
                                }
                            } finally {
                                calculateExecDuration();
                                resultForm.highlightLoading(false);
                                context.reset();
                            }
                        }
                    });
        });
    }

    public void loadResultSet(DBNResultSet resultSet) throws SQLException {
        StatementExecutionResultForm resultForm = getForm();
        if (Checks.isValid(resultForm)) {
            int rowCount = Math.max(dataModel == null ? 0 : dataModel.getRowCount() + 1, 100);
            dataModel = new ResultSetDataModel<>(resultSet, getConnection(), rowCount);
            resultForm.rebuildForm();
            resultForm.updateVisibleComponents();
        }
    }

    @Nullable
    @Override
    public StatementExecutionResultForm createForm() {
        StatementExecutionResultForm form = new StatementExecutionResultForm(this);
        form.updateVisibleComponents();
        return form;
    }

    public void fetchNextRecords() {
        Project project = getProject();
        Progress.background(project, getConnection(), false,
                "Loading data",
                "Loading next records for " + getExecutionProcessor().getStatementName(),
                progress -> {
                    StatementExecutionResultForm resultForm = getForm();
                    if (Checks.isValid(resultForm)) {
                        resultForm.highlightLoading(true);
                        try {
                            if (hasResult() && !dataModel.isResultSetExhausted()) {
                                int fetchBlockSize = getExecutionInput().getResultSetFetchBlockSize();
                                dataModel.fetchNextRecords(fetchBlockSize, false);
                                //tResult.accommodateColumnsSize();
                                if (dataModel.isResultSetExhausted()) {
                                    dataModel.closeResultSet();
                                }
                                resultForm.updateVisibleComponents();
                            }

                        } catch (SQLException e) {
                            conditionallyLog(e);
                            Messages.showErrorDialog(project, "Could not perform operation.", e);
                        } finally {
                            resultForm.highlightLoading(false);
                        }
                    }
                });
    }

    public ResultSetDataModel<?, ?> getTableModel() {
        return dataModel;
    }

    @Nullable
    public ResultSetTable<?> getResultTable() {
        StatementExecutionResultForm resultForm = getForm();
        return Checks.isValid(resultForm) ? resultForm.getResultTable() : null;
    }

    public boolean hasResult() {
        return dataModel != null;
    }

    public void navigateToResult() {
        StatementExecutionResultForm resultForm = getForm();
        if (Checks.isValid(resultForm)) {
            resultForm.show();
        }
    }

    @Override
    public void disposeInner() {
        dataModel = null;
        super.disposeInner();
    }


    /********************************************************
     *                    Data Provider                     *
     ********************************************************/
    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.STATEMENT_EXECUTION_CURSOR_RESULT.is(dataId)) {
            return StatementExecutionCursorResult.this;
        }
        return null;
    }
}
