package com.dci.intellij.dbn.editor.data.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Checks;
import com.dci.intellij.dbn.editor.data.DatasetEditor;
import com.dci.intellij.dbn.editor.data.options.DataEditorSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecordsFetchNextAction extends AbstractDataEditorAction {

    public RecordsFetchNextAction() {
        super("Fetch next records", Icons.DATA_EDITOR_FETCH_NEXT_RECORDS);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DatasetEditor datasetEditor) {
        DataEditorSettings settings = DataEditorSettings.getInstance(datasetEditor.getProject());
        datasetEditor.fetchNextRecords(settings.getGeneralSettings().getFetchBlockSize().value());
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DatasetEditor datasetEditor) {
        presentation.setText("Fetch Next Records");

        DataEditorSettings settings = DataEditorSettings.getInstance(project);
        presentation.setText("Fetch Next " + settings.getGeneralSettings().getFetchBlockSize().value() + " Records");

        boolean enabled =
                Checks.isValid(datasetEditor) &&
                        datasetEditor.isLoaded() &&
                        datasetEditor.getConnection().isConnected() &&
                        !datasetEditor.isInserting() &&
                        !datasetEditor.isLoading() &&
                        !datasetEditor.isDirty() &&
                        !datasetEditor.getEditorTable().getModel().isResultSetExhausted();
        presentation.setEnabled(enabled);
    }
}