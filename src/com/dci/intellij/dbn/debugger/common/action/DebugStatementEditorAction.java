package com.dci.intellij.dbn.debugger.common.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.DumbAwareProjectAction;
import com.dci.intellij.dbn.common.action.Lookup;
import com.dci.intellij.dbn.common.util.Documents;
import com.dci.intellij.dbn.common.util.Editors;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.mapping.FileConnectionMappingManager;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.execution.statement.StatementExecutionManager;
import com.dci.intellij.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.dci.intellij.dbn.language.common.psi.ExecutablePsiElement;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.dci.intellij.dbn.vfs.DBConsoleType;
import com.dci.intellij.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class DebugStatementEditorAction extends DumbAwareProjectAction {

    public DebugStatementEditorAction() {
        super(null);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        Editor editor = Lookup.getEditor(e);
        if (editor != null) {
            VirtualFile virtualFile = Documents.getVirtualFile(editor);
            ExecutablePsiElement executablePsiElement = null;
            if (virtualFile instanceof DBConsoleVirtualFile) {
                DBConsoleVirtualFile consoleVirtualFile = (DBConsoleVirtualFile) virtualFile;
                if (consoleVirtualFile.getType() == DBConsoleType.DEBUG) {
                    PsiFile file = Documents.getFile(editor);
                    if (file != null) {
                        BasePsiElement basePsiElement = PsiUtil.lookupElementAtOffset(file, ElementTypeAttribute.EXECUTABLE, 100);
                        if (basePsiElement instanceof ExecutablePsiElement) {
                            executablePsiElement = (ExecutablePsiElement) basePsiElement;
                        }
                    }
                }
            }

            if (executablePsiElement == null) {
                executablePsiElement = PsiUtil.lookupExecutableAtCaret(editor, true);
            }

            if (executablePsiElement != null && executablePsiElement.is(ElementTypeAttribute.DEBUGGABLE)) {
                FileEditor fileEditor = Editors.getFileEditor(editor);
                if (fileEditor != null) {
                    StatementExecutionManager statementExecutionManager = StatementExecutionManager.getInstance(project);
                    StatementExecutionProcessor executionProcessor = statementExecutionManager.getExecutionProcessor(fileEditor, executablePsiElement, true);
                    if (executionProcessor != null) {
                        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
                        debuggerManager.startStatementDebugger(executionProcessor);
                    }
                }
            }
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setIcon(Icons.STMT_EXECUTION_DEBUG);
        presentation.setText("Debug Statement");
        Editor editor = Lookup.getEditor(e);
        boolean enabled = false;
        boolean visible = false;
        if (editor != null) {
            FileConnectionMappingManager connectionMappingManager = FileConnectionMappingManager.getInstance(project);
            VirtualFile virtualFile = Documents.getVirtualFile(editor);
            if (virtualFile != null) {
                enabled = DatabaseDebuggerManager.isDebugConsole(virtualFile);

                ConnectionHandler connectionHandler = connectionMappingManager.getConnectionHandler(virtualFile);
                if (DatabaseFeature.DEBUGGING.isSupported(connectionHandler)){
                    visible = true;
                    if (!enabled) {
                        PsiFile psiFile = PsiUtil.getPsiFile(project, editor.getDocument());
                        if (psiFile != null) {
                            ExecutablePsiElement executablePsiElement = PsiUtil.lookupExecutableAtCaret(editor, true);
                            if (executablePsiElement != null && executablePsiElement.is(ElementTypeAttribute.DEBUGGABLE)) {
                                enabled = true;
                            }
                        }
                    }
                }
            }
        }
        presentation.setEnabled(enabled);
        presentation.setVisible(visible);

    }
}
