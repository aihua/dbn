package com.dci.intellij.dbn.object.action;

import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.dci.intellij.dbn.object.status.ObjectStatusManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ObjectsStatusRefreshAction extends DumbAwareAction {

    private final ConnectionRef connection;

    public ObjectsStatusRefreshAction(ConnectionHandler connection) {
        super("Refresh Objects Status");
        this.connection = connection.ref();
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ConnectionHandler connection = getConnection();
        Project project = connection.getProject();
        ObjectStatusManager statusManager = ObjectStatusManager.getInstance(project);
        statusManager.refreshObjectsStatus(connection, null);
    }
}
