package com.dci.intellij.dbn.execution.script.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.util.DocumentUtil;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.execution.script.ScriptExecutionManager;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import static com.dci.intellij.dbn.common.util.ActionUtil.*;

public class ExecuteScriptEditorAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = ensureProject(e);
        Editor editor = getEditor(e);
        if (editor != null) {
            FileDocumentManager.getInstance().saveDocument(editor.getDocument());
            ScriptExecutionManager scriptExecutionManager = ScriptExecutionManager.getInstance(project);
            VirtualFile virtualFile = DocumentUtil.getVirtualFile(editor);
            scriptExecutionManager.executeScript(virtualFile);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(isEnabled(e));
        presentation.setIcon(Icons.EXECUTE_SQL_SCRIPT);
        presentation.setText("Execute SQL Script");
        presentation.setVisible(isVisible(e));
    }

    private static boolean isVisible(AnActionEvent e) {
        VirtualFile virtualFile = getVirtualFile(e);
        return !DatabaseDebuggerManager.isDebugConsole(virtualFile);
    }

    private static boolean isEnabled(AnActionEvent e) {
        Project project = getProject(e);
        Editor editor = getEditor(e);
        if (project == null || editor == null) {
            return false;
        } else {
            PsiFile psiFile = PsiUtil.getPsiFile(project, editor.getDocument());
            return psiFile instanceof DBLanguagePsiFile;
        }
    }
}
