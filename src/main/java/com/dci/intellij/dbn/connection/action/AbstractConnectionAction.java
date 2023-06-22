package com.dci.intellij.dbn.connection.action;

import com.dci.intellij.dbn.common.action.ContextAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class AbstractConnectionAction extends ContextAction<ConnectionHandler> {
    private final ConnectionRef connection;

    public AbstractConnectionAction(String text, @NotNull ConnectionHandler connection) {
        this(text, null, connection);

    }
    public AbstractConnectionAction(String text, Icon icon, @NotNull ConnectionHandler connection) {
        this(text, null, icon, connection);
    }
    public AbstractConnectionAction(String text, String description, Icon icon, @NotNull ConnectionHandler connection) {
        super(text, description, icon);
        this.connection = connection.ref();
    }

    public ConnectionId getConnectionId() {
        return connection.getConnectionId();
    }

    @Nullable
    public ConnectionHandler getConnection() {
        return ConnectionRef.get(connection);
    }

    @Override
    protected ConnectionHandler getTarget(@NotNull AnActionEvent e) {
        return connection.get();
    }

    @NotNull
    @Override
    public Project getProject() {
        return connection.ensure().getProject();
    }
}

