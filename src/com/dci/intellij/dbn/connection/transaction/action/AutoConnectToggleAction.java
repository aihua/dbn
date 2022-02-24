package com.dci.intellij.dbn.connection.transaction.action;

import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.action.AbstractConnectionToggleAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class AutoConnectToggleAction extends AbstractConnectionToggleAction {

    public AutoConnectToggleAction(ConnectionHandler connection) {
        super("Connect Automatically", connection);

    }
    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return getConnection().getSettings().getDetailSettings().isConnectAutomatically();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        getConnection().getSettings().getDetailSettings().setConnectAutomatically(state);
    }
}
