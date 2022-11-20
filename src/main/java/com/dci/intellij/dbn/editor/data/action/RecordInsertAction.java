package com.dci.intellij.dbn.editor.data.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Checks;
import com.dci.intellij.dbn.common.environment.EnvironmentManager;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.data.DatasetEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecordInsertAction extends AbstractDataEditorAction {

    public RecordInsertAction() {
        super("Insert record", Icons.DATA_EDITOR_INSERT_RECORD);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DatasetEditor datasetEditor) {
        datasetEditor.insertRecord();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DatasetEditor datasetEditor) {
        presentation.setText("Insert record");
        if (Checks.isValid(datasetEditor)) {
            EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
            boolean isEnvironmentReadonlyData = environmentManager.isReadonly(datasetEditor.getDataset(), DBContentType.DATA);
            presentation.setVisible(!isEnvironmentReadonlyData && !datasetEditor.isReadonlyData());
            presentation.setEnabled(
                    datasetEditor.getConnection().isConnected() &&
                    !datasetEditor.isReadonly() &&
                    !datasetEditor.isInserting() &&
                    !datasetEditor.isLoading() &&
                    !datasetEditor.isDirty());

        } else {
            presentation.setEnabled(false);
        }
    }
}