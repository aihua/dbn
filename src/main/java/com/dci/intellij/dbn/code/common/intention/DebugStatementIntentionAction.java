package com.dci.intellij.dbn.code.common.intention;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.util.Context;
import com.dci.intellij.dbn.common.util.Editors;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.connection.mapping.FileConnectionContextManager;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.execution.statement.StatementExecutionManager;
import com.dci.intellij.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.psi.ExecutablePsiElement;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dci.intellij.dbn.common.dispose.Checks.isNotValid;
import static com.dci.intellij.dbn.common.util.Files.isDbLanguageFile;
import static com.dci.intellij.dbn.common.util.Files.isDbLanguagePsiFile;

public class DebugStatementIntentionAction extends GenericIntentionAction implements HighPriorityAction {
    @Override
    @NotNull
    public String getText() {
        return "Debug statement";
    }


    @Override
    public Icon getIcon(int flags) {
        return Icons.STMT_EXECUTION_DEBUG;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
        if (isNotValid(psiFile)) return false;
        if (!isDbLanguagePsiFile(psiFile)) return false;

        VirtualFile file = psiFile.getVirtualFile();
        if (isNotValid(file)) return false;
        if (!isDbLanguageFile(file)) return false;

        ExecutablePsiElement executable = PsiUtil.lookupExecutableAtCaret(editor, true);
        if (isNotValid(executable)) return false;

        FileEditor fileEditor = Editors.getFileEditor(editor);
        if (isNotValid(fileEditor)) return false;

        return executable.is(ElementTypeAttribute.DEBUGGABLE);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
        if (isNotValid(project)) return;
        if (isNotValid(editor)) return;
        if (isNotValid(psiFile)) return;
        if (!isDbLanguagePsiFile(psiFile)) return;

        ExecutablePsiElement executable = PsiUtil.lookupExecutableAtCaret(editor, true);
        if (isNotValid(executable)) return;

        FileEditor fileEditor = Editors.getFileEditor(editor);
        if (isNotValid(fileEditor)) return;

        DBLanguagePsiFile databasePsiFile = (DBLanguagePsiFile) psiFile;
        DataContext dataContext = Context.getDataContext(editor);

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        contextManager.selectConnectionAndSchema(
                databasePsiFile.getVirtualFile(),
                dataContext,
                () -> ConnectionAction.invoke("", false, databasePsiFile,
                        action -> {
                            StatementExecutionManager executionManager = StatementExecutionManager.getInstance(project);
                            StatementExecutionProcessor executionProcessor = executionManager.getExecutionProcessor(fileEditor, executable, true);
                            if (executionProcessor != null) {
                                DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
                                debuggerManager.startStatementDebugger(executionProcessor);
                            }
                        }));
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    protected Integer getGroupPriority() {
        return 1;
    }
}
