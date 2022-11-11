package com.dci.intellij.dbn.connection.transaction.action;

import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.action.AbstractConnectionToggleAction;
import com.dci.intellij.dbn.connection.transaction.DatabaseTransactionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class AutoCommitToggleAction extends AbstractConnectionToggleAction {

    public AutoCommitToggleAction(ConnectionHandler connection) {
        super("Auto-Commit", connection);

    }
    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return getConnection().isAutoCommit();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        ConnectionHandler connection = getConnection();
        DatabaseTransactionManager transactionManager = DatabaseTransactionManager.getInstance(connection.getProject());
        transactionManager.toggleAutoCommit(connection);
    }
}
