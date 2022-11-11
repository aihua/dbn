package com.dci.intellij.dbn.code.common.intention;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.util.Context;
import com.dci.intellij.dbn.connection.ConnectionSelectorOptions;
import com.dci.intellij.dbn.connection.mapping.FileConnectionContextManager;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.intellij.codeInsight.intention.LowPriorityAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dci.intellij.dbn.connection.ConnectionSelectorOptions.Option.SHOW_CREATE_CONNECTION;
import static com.dci.intellij.dbn.connection.ConnectionSelectorOptions.Option.SHOW_VIRTUAL_CONNECTIONS;
import static com.dci.intellij.dbn.connection.ConnectionSelectorOptions.options;

public class SelectConnectionIntentionAction extends GenericIntentionAction implements LowPriorityAction {
    @Override
    @NotNull
    public String getText() {
        return "Select connection";
    }

    @Override
    public Icon getIcon(int flags) {
        return Icons.FILE_CONNECTION_MAPPING;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
        if (psiFile instanceof DBLanguagePsiFile) {
            VirtualFile virtualFile = psiFile.getVirtualFile();
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            return contextManager.isConnectionSelectable(virtualFile);
        }
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
        if (psiFile instanceof DBLanguagePsiFile) {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);

            ConnectionSelectorOptions options = options(
                    SHOW_VIRTUAL_CONNECTIONS,
                    SHOW_CREATE_CONNECTION);

            DataContext dataContext = Context.getDataContext(editor);
            contextManager.promptConnectionSelector(psiFile.getVirtualFile(), dataContext, options, null);
        }
    }


    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    protected Integer getGroupPriority() {
        return 2;
    }
}
