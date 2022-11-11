package com.dci.intellij.dbn.editor.code.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.environment.EnvironmentManager;
import com.dci.intellij.dbn.common.option.ConfirmationOptionHandler;
import com.dci.intellij.dbn.editor.DBContentType;
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

import static com.dci.intellij.dbn.vfs.VirtualFileStatus.MODIFIED;
import static com.dci.intellij.dbn.vfs.VirtualFileStatus.SAVING;

public class SourceCodeSaveAction extends AbstractCodeEditorAction {
    public SourceCodeSaveAction() {
        super("", null, Icons.CODE_EDITOR_SAVE_TO_DATABASE);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull SourceCodeEditor fileEditor, @NotNull DBSourceCodeVirtualFile sourceCodeFile) {
        CodeEditorSettings editorSettings = CodeEditorSettings.getInstance(project);
        CodeEditorConfirmationSettings confirmationSettings = editorSettings.getConfirmationSettings();
        ConfirmationOptionHandler optionHandler = confirmationSettings.getSaveChanges();
        boolean canContinue = optionHandler.resolve(fileEditor.getObject().getQualifiedNameWithType());
        if (canContinue) {
            SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
            sourceCodeManager.saveSourceCode(sourceCodeFile, fileEditor, null);
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project, @Nullable SourceCodeEditor fileEditor, @Nullable DBSourceCodeVirtualFile sourceCodeFile) {
        Presentation presentation = e.getPresentation();
        if (Failsafe.check(sourceCodeFile)) {
            EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
            boolean readonly = environmentManager.isReadonly(sourceCodeFile);
            presentation.setVisible(!readonly);
            DBContentType contentType = sourceCodeFile.getContentType();
            String text =
                    contentType == DBContentType.CODE_SPEC ? "Save spec" :
                    contentType == DBContentType.CODE_BODY ? "Save body" : "Save";

            presentation.setEnabled(sourceCodeFile.is(MODIFIED) && sourceCodeFile.isNot(SAVING));
            presentation.setText(text);
        } else {
            presentation.setEnabled(false);
        }
    }
}
