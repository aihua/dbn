package com.dci.intellij.dbn.execution.statement.action;

import org.jetbrains.annotations.NotNull;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.util.ActionUtil;
import com.dci.intellij.dbn.common.util.EditorUtil;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.execution.statement.StatementExecutionManager;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

public class ExecuteStatementEditorAction extends AnAction {
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = ActionUtil.getProject(e);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (project != null && editor != null) {
            FileEditor fileEditor = EditorUtil.getFileEditor(editor);
            if (fileEditor != null) {
                StatementExecutionManager.getInstance(project).executeStatementAtCursor(fileEditor);
            }
        }
    }

    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(isEnabled(e));
        presentation.setIcon(Icons.STMT_EXECUTION_RUN);
        presentation.setText("Execute Statement");
        presentation.setVisible(isVisible(e));
    }

    private static boolean isEnabled(AnActionEvent e) {
        Project project = ActionUtil.getProject(e);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (project == null || editor == null) {
            return false;
        } else {
            PsiFile psiFile = PsiUtil.getPsiFile(project, editor.getDocument());
            return psiFile instanceof DBLanguagePsiFile;
        }
    }

    public static boolean isVisible(AnActionEvent e) {
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        return !DatabaseDebuggerManager.isDebugConsole(virtualFile);
    }
}
