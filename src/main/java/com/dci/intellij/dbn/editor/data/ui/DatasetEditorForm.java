package com.dci.intellij.dbn.editor.data.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.DataProviders;
import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.AutoCommitLabel;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.util.Borders;
import com.dci.intellij.dbn.common.ui.util.UserInterface;
import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.SessionId;
import com.dci.intellij.dbn.data.find.DataSearchComponent;
import com.dci.intellij.dbn.data.find.SearchableDataComponent;
import com.dci.intellij.dbn.data.grid.options.DataGridAuditColumnSettings;
import com.dci.intellij.dbn.data.grid.options.DataGridSettings;
import com.dci.intellij.dbn.data.grid.ui.table.basic.BasicTable;
import com.dci.intellij.dbn.data.grid.ui.table.basic.BasicTableScrollPane;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.data.DatasetEditor;
import com.dci.intellij.dbn.editor.data.state.column.DatasetColumnState;
import com.dci.intellij.dbn.editor.data.statusbar.DatasetEditorStatusBarWidget;
import com.dci.intellij.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dci.intellij.dbn.editor.data.ui.table.cell.DatasetTableCellEditor;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.dci.intellij.dbn.object.DBDataset;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatasetEditorForm extends DBNFormBase implements SearchableDataComponent {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JLabel loadingLabel;
    private JPanel loadingIconPanel;
    private JPanel searchPanel;
    private JPanel loadingActionPanel;
    private JPanel loadingDataPanel;
    private JPanel datasetTablePanel;
    private JScrollPane datasetTableScrollPane;

    private AutoCommitLabel autoCommitLabel;
    private JPanel toolbarPanel;
    private DatasetEditorTable datasetEditorTable;
    private final WeakRef<DatasetEditor> datasetEditor;

    private final Latent<DataSearchComponent> dataSearchComponent = Latent.basic(() -> {
        DataSearchComponent dataSearchComponent = new DataSearchComponent(DatasetEditorForm.this);
        searchPanel.add(dataSearchComponent.getComponent(), BorderLayout.CENTER);
        DataProviders.register(dataSearchComponent.getSearchField(), this);
        return dataSearchComponent;
    });


    public DatasetEditorForm(DatasetEditor datasetEditor) {
        super(datasetEditor, datasetEditor.getProject());
        this.datasetEditor = WeakRef.of(datasetEditor);

        DBDataset dataset = getDataset();
        try {
            this.toolbarPanel.setBorder(Borders.insetBorder(2));

            datasetTablePanel.setBorder(Borders.lineBorder(Colors.getTableHeaderGridColor(), 1, 0, 0, 0));
            datasetEditorTable = new DatasetEditorTable(this, datasetEditor);
            datasetTableScrollPane.setViewportView(datasetEditorTable);
            datasetEditorTable.initTableGutter();

            ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel,"", true, "DBNavigator.ActionGroup.DataEditor");

            actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
            loadingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
            hideLoadingHint();

            ActionToolbar loadingActionToolbar = Actions.createActionToolbar(actionsPanel,"", true, new CancelLoadingAction());
            loadingActionPanel.add(loadingActionToolbar.getComponent(), BorderLayout.CENTER);

            Disposer.register(this, autoCommitLabel);
        } catch (SQLException e) {
            Messages.showErrorDialog(
                    getProject(),
                    "Error",
                    "Error opening data editor for " + dataset.getQualifiedNameWithType(), e);
        }

        if (dataset.isEditable(DBContentType.DATA)) {
            ConnectionHandler connection = getConnectionHandler();
            autoCommitLabel.init(getProject(), datasetEditor.getDatabaseFile(), connection, SessionId.MAIN);
        }

        Disposer.register(datasetEditor, this);
    }

    public DatasetEditorTable beforeRebuild() throws SQLException {
        Project project = ensureProject();

        DatasetEditorTable oldEditorTable = getEditorTable();
        DatasetEditor datasetEditor = getDatasetEditor();

        datasetEditorTable = new DatasetEditorTable(this, datasetEditor);
        DatasetEditorStatusBarWidget statusBarWidget = DatasetEditorStatusBarWidget.getInstance(project);
        datasetEditorTable.getSelectionModel().addListSelectionListener(e -> statusBarWidget.update());


        DataGridSettings dataGridSettings = DataGridSettings.getInstance(project);
        DataGridAuditColumnSettings auditColumnSettings = dataGridSettings.getAuditColumnSettings();

        List<TableColumn> hiddenColumns = new ArrayList<>();
        for (DatasetColumnState columnState : datasetEditor.getColumnSetup().getColumnStates()) {

            if (!columnState.isVisible() || !auditColumnSettings.isColumnVisible(columnState.getName())) {
                String columnName = columnState.getName();
                TableColumn tableColumn = datasetEditorTable.getColumnByName(columnName);
                if (tableColumn != null) {
                    hiddenColumns.add(tableColumn);
                }
            }
        }
        for (TableColumn hiddenColumn : hiddenColumns) {
            datasetEditorTable.removeColumn(hiddenColumn);
        }
        return oldEditorTable;
    }

    public void afterRebuild(final DatasetEditorTable oldEditorTable) {
        if (oldEditorTable != null) {
            Dispatch.run(() -> {
                DatasetEditorTable datasetEditorTable = getEditorTable();
                datasetTableScrollPane.setViewportView(datasetEditorTable);
                datasetEditorTable.initTableGutter();
                datasetEditorTable.updateBackground(false);

                Disposer.dispose(oldEditorTable, true);
            });
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @NotNull
    private DBDataset getDataset() {
        return getDatasetEditor().getDataset();
    }

    @NotNull
    public DatasetEditor getDatasetEditor() {
        return datasetEditor.ensure();
    }

    public void showLoadingHint() {
        Dispatch.run(() -> Failsafe.nn(loadingDataPanel).setVisible(true));
    }

    public void hideLoadingHint() {
        Dispatch.run(() -> Failsafe.nn(loadingDataPanel).setVisible(false));
    }

    @NotNull
    public DatasetEditorTable getEditorTable() {
        return Failsafe.nn(datasetEditorTable);
    }

    private ConnectionHandler getConnectionHandler() {
        return getEditorTable().getDataset().getConnection();
    }

    public float getHorizontalScrollProportion() {
        datasetTableScrollPane.getHorizontalScrollBar().getModel();
        return 0;
    }

    /*********************************************************
     *              SearchableDataComponent                  *
     *********************************************************/
    @Override
    public void showSearchHeader() {
        DatasetEditorTable editorTable = getEditorTable();
        editorTable.cancelEditing();
        editorTable.clearSelection();

        DataSearchComponent dataSearchComponent = getSearchComponent();
        dataSearchComponent.initializeFindModel();

        JTextField searchField = dataSearchComponent.getSearchField();
        if (searchPanel.isVisible()) {
            searchField.selectAll();
        } else {
            searchPanel.setVisible(true);    
        }
        Dispatch.run(() -> searchField.requestFocus());
    }

    private DataSearchComponent getSearchComponent() {
        return dataSearchComponent.get();
    }

    @Override
    public void hideSearchHeader() {
        getSearchComponent().resetFindModel();
        searchPanel.setVisible(false);
        DatasetEditorTable editorTable = getEditorTable();

        UserInterface.repaintAndFocus(editorTable);
    }

    @Override
    public void cancelEditActions() {
        getEditorTable().cancelEditing();
    }

    @Override
    public String getSelectedText() {
        DatasetTableCellEditor cellEditor = getEditorTable().getCellEditor();
        if (cellEditor != null) {
            return cellEditor.getTextField().getSelectedText();
        }
        return null;
    }

    @NotNull
    @Override
    public BasicTable<?> getTable() {
        return getEditorTable();
    }

    private void createUIComponents() {
        datasetTableScrollPane = new BasicTableScrollPane();
    }

    private class CancelLoadingAction extends AnAction {
        CancelLoadingAction() {
            super("Cancel", null, Icons.DATA_EDITOR_STOP_LOADING);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getEditorTable().getModel().cancelDataLoad();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(!getEditorTable().getModel().isLoadCancelled());
        }
    }


    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        Object data = super.getData(dataId);
        if (data == null) {
            data = getDatasetEditor().getData(dataId);
        }
        return data;
    }
}
