package com.dci.intellij.dbn.execution.method.result.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.execution.method.result.MethodExecutionResult;
import com.dci.intellij.dbn.vfs.DatabaseFileSystem;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class MethodEditAction extends AbstractMethodExecutionResultAction {
    public MethodEditAction() {
        super("Edit Method", Icons.OBEJCT_EDIT_SOURCE);
    }

    @Override
    protected void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull MethodExecutionResult executionResult) {

        DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
        databaseFileSystem.connectAndOpenEditor(executionResult.getMethod(), null, true, true);
    }
}