package com.dci.intellij.dbn.editor.data.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.environment.EnvironmentManager;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.data.DatasetEditor;
import com.dci.intellij.dbn.editor.data.model.DatasetEditorModelRow;
import com.dci.intellij.dbn.editor.data.ui.table.DatasetEditorTable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dci.intellij.dbn.editor.data.model.RecordStatus.DELETED;

public class RecordDeleteAction extends AbstractDataEditorAction {

    public RecordDeleteAction() {
        super("Delete records", Icons.DATA_EDITOR_DELETE_RECORD);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DatasetEditor datasetEditor) {
        datasetEditor.deleteRecords();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DatasetEditor datasetEditor) {
        presentation.setText("Delete Records");
        if (Failsafe.check(datasetEditor) && datasetEditor.getConnection().isConnected()) {
            EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
            boolean isEnvironmentReadonlyData = environmentManager.isReadonly(datasetEditor.getDataset(), DBContentType.DATA);
            presentation.setVisible(!isEnvironmentReadonlyData && !datasetEditor.isReadonlyData());
            presentation.setEnabled(true);
            if (datasetEditor.isInserting() || datasetEditor.isLoading() || datasetEditor.isDirty() || datasetEditor.isReadonly()) {
                presentation.setEnabled(false);
            } else {
                DatasetEditorTable editorTable = datasetEditor.getEditorTable();
                if (editorTable.getSelectedRows() != null && editorTable.getSelectedRows().length > 0) {
                    for (int selectedRow : editorTable.getSelectedRows()) {
                        if (selectedRow < editorTable.getModel().getRowCount()) {
                            DatasetEditorModelRow row = editorTable.getModel().getRowAtIndex(selectedRow);
                            if (row != null && row.isNot(DELETED)) {
                                presentation.setEnabled(true);
                                return;
                            }
                        }
                    }
                }
            }
        } else {
            presentation.setEnabled(false);
        }
    }
}