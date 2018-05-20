package com.dci.intellij.dbn.editor.data.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.editor.data.DatasetEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dci.intellij.dbn.common.util.ActionUtil.getFileEditor;

public class LockUnlockDataEditing extends ToggleAction implements DumbAware {

    public LockUnlockDataEditing() {
        super("Lock / Unlock Editing", null, Icons.DATA_EDITOR_LOCK_EDITING);
    }

    public boolean isSelected(AnActionEvent e) {
        DatasetEditor datasetEditor = getDatasetEditor(e);
        return datasetEditor != null && datasetEditor.isReadonly();
    }

    public void setSelected(AnActionEvent e, boolean selected) {
        DatasetEditor datasetEditor = getDatasetEditor(e);
        if (datasetEditor != null) datasetEditor.setReadonly(selected);
    }

    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        DatasetEditor datasetEditor = getDatasetEditor(e);
        Presentation presentation = e.getPresentation();
        Project project = e.getProject();
        if (project == null || datasetEditor == null) {
            presentation.setEnabled(false);
        } else {
            boolean isEnvironmentReadonlyData = datasetEditor.getDataset().getEnvironmentType().isReadonlyData();
            presentation.setVisible(!datasetEditor.isReadonlyData() && !isEnvironmentReadonlyData);
            presentation.setText(isSelected(e) ? "Unlock Editing" : "Lock Editing");
            boolean enabled = !datasetEditor.isInserting();
            presentation.setEnabled(enabled);
        }
    }

    private static DatasetEditor getDatasetEditor(AnActionEvent e) {
        FileEditor fileEditor = getFileEditor(e);
        return fileEditor instanceof DatasetEditor ? (DatasetEditor) fileEditor : null;
    }
}