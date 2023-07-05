package com.dci.intellij.dbn.execution.method.result.ui;

import com.dci.intellij.dbn.common.action.DataKeys;
import com.dci.intellij.dbn.common.action.DataProviders;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.misc.DBNTableScrollPane;
import com.dci.intellij.dbn.common.ui.util.Borders;
import com.dci.intellij.dbn.common.ui.util.UserInterface;
import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.data.find.DataSearchComponent;
import com.dci.intellij.dbn.data.find.SearchableDataComponent;
import com.dci.intellij.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dci.intellij.dbn.data.model.resultSet.ResultSetDataModel;
import com.dci.intellij.dbn.data.record.RecordViewInfo;
import com.dci.intellij.dbn.execution.method.result.MethodExecutionResult;
import com.dci.intellij.dbn.object.DBArgument;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class MethodExecutionCursorResultForm extends DBNFormBase implements SearchableDataComponent {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JPanel resultPanel;
    private JPanel searchPanel;
    private DBNTableScrollPane resultScrollPane;

    private final DBObjectRef<DBArgument> argument;
    private final ResultSetTable<ResultSetDataModel<?, ?>> resultTable;

    private final Latent<DataSearchComponent> dataSearchComponent = Latent.basic(() -> {
        DataSearchComponent dataSearchComponent = new DataSearchComponent(MethodExecutionCursorResultForm.this);
        searchPanel.add(dataSearchComponent.getComponent(), BorderLayout.CENTER);
        DataProviders.register(dataSearchComponent.getSearchField(), this);
        return dataSearchComponent;
    });

    MethodExecutionCursorResultForm(MethodExecutionResultForm parent, MethodExecutionResult executionResult, DBArgument argument) {
        super(parent);
        this.argument = DBObjectRef.of(argument);
        ResultSetDataModel<?, ?> dataModel = executionResult.getTableModel(argument);
        RecordViewInfo recordViewInfo = new RecordViewInfo(
                executionResult.getName(),
                executionResult.getIcon());

        resultTable = new ResultSetTable<>(this, dataModel, true, recordViewInfo);
        resultTable.setPreferredScrollableViewportSize(new Dimension(500, -1));

        resultPanel.setBorder(Borders.lineBorder(JBColor.border(), 1, 0, 1, 0));
        resultScrollPane.setViewportView(resultTable);
        resultTable.initTableGutter();

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, "", true, "DBNavigator.ActionGroup.MethodExecutionCursorResult");
        actionsPanel.add(actionToolbar.getComponent());
        DataProviders.register(actionToolbar.getComponent(), this);
    }

    public DBArgument getArgument() {
        return argument.get();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    /*********************************************************
     *              SearchableDataComponent                  *
     *********************************************************/
    @Override
    public void showSearchHeader() {
        resultTable.clearSelection();

        DataSearchComponent dataSearchComponent = getSearchComponent();
        dataSearchComponent.initializeFindModel();
        JTextField searchField = dataSearchComponent.getSearchField();
        if (searchPanel.isVisible()) {
            searchField.selectAll();
        } else {
            searchPanel.setVisible(true);
        }
        searchField.requestFocus();

    }

    private DataSearchComponent getSearchComponent() {
        return dataSearchComponent.get();
    }

    @Override
    public void hideSearchHeader() {
        getSearchComponent().resetFindModel();
        searchPanel.setVisible(false);
        UserInterface.repaintAndFocus(resultTable);
    }

    @Override
    public void cancelEditActions() {

    }

    @Override
    public String getSelectedText() {
        return null;
    }

    @NotNull
    @Override
    public ResultSetTable<?> getTable() {
        return Failsafe.nn(resultTable);
    }

    /********************************************************
     *                    Data Provider                     *
     ********************************************************/
    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.METHOD_EXECUTION_CURSOR_RESULT_FORM.is(dataId)) {
            return MethodExecutionCursorResultForm.this;
        }
        if (DataKeys.METHOD_EXECUTION_ARGUMENT.is(dataId)) {
            return DBObjectRef.get(argument);
        }
        return super.getData(dataId);
    }
}
