package com.dci.intellij.dbn.development.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.action.AbstractConnectionAction;
import com.dci.intellij.dbn.diagnostics.Diagnostics;
import com.dci.intellij.dbn.object.common.DBObjectRecursiveLoaderVisitor;
import com.dci.intellij.dbn.object.common.list.DBObjectListContainer;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DevLoadAllObjectsAction extends AbstractConnectionAction {
    public DevLoadAllObjectsAction(ConnectionHandler connectionHandler) {
        super("Load All Objects", null, Icons.DATA_EDITOR_RELOAD_DATA, connectionHandler);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull ConnectionHandler connectionHandler) {
        Progress.prompt(
                project,
                connectionHandler.getMetaLoadTitle(), true,
                (progress) -> {
                    DBObjectListContainer objectListContainer = connectionHandler.getObjectBundle().getObjectListContainer();
                    objectListContainer.visitLists(DBObjectRecursiveLoaderVisitor.INSTANCE, false);
                });
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable ConnectionHandler connectionHandler) {
        presentation.setVisible(Diagnostics.isDeveloperMode());
    }
}
