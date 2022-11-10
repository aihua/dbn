package com.dci.intellij.dbn.editor.code.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.environment.EnvironmentManager;
import com.dci.intellij.dbn.common.option.ConfirmationOptionHandler;
import com.dci.intellij.dbn.editor.code.SourceCodeEditor;
import com.dci.intellij.dbn.editor.code.SourceCodeManager;
import com.dci.intellij.dbn.editor.code.options.CodeEditorConfirmationSettings;
import com.dci.intellij.dbn.editor.code.options.CodeEditorSettings;
import com.dci.intellij.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dci.intellij.dbn.vfs.VirtualFileStatus.LOADING;
import static com.dci.intellij.dbn.vfs.VirtualFileStatus.MODIFIED;

public class SourceCodeRevertAction extends AbstractCodeEditorAction {
    public SourceCodeRevertAction() {
        super("Revert changes", null, Icons.CODE_EDITOR_RESET);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull SourceCodeEditor fileEditor, @NotNull DBSourceCodeVirtualFile sourceCodeFile) {
        CodeEditorConfirmationSettings confirmationSettings = CodeEditorSettings.getInstance(project).getConfirmationSettings();
        ConfirmationOptionHandler optionHandler = confirmationSettings.getRevertChanges();
        boolean canContinue = optionHandler.resolve(fileEditor.getObject().getQualifiedNameWithType());

        if (canContinue) {
            SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
            sourceCodeManager.loadSourceCode(sourceCodeFile, true);
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project, @Nullable SourceCodeEditor fileEditor, @Nullable DBSourceCodeVirtualFile sourceCodeFile) {
        Presentation presentation = e.getPresentation();
        presentation.setText("Revert Changes");
        if (Failsafe.check(sourceCodeFile)) {
            EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
            boolean readonly = environmentManager.isReadonly(sourceCodeFile);
            presentation.setVisible(!readonly);
            presentation.setEnabled(sourceCodeFile.isNot(LOADING) && sourceCodeFile.is(MODIFIED));
        } else {
            presentation.setEnabled(false);
        }


    }
}
