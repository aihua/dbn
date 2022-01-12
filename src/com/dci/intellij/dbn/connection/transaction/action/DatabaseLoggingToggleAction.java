package com.dci.intellij.dbn.connection.transaction.action;

import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.action.AbstractConnectionToggleAction;
import com.dci.intellij.dbn.database.DatabaseCompatibilityInterface;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

public class DatabaseLoggingToggleAction extends AbstractConnectionToggleAction {

    public DatabaseLoggingToggleAction(ConnectionHandler connectionHandler) {
        super("Database Logging", connectionHandler);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return getConnectionHandler().isLoggingEnabled();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        getConnectionHandler().setLoggingEnabled(state);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        ConnectionHandler connectionHandler = getConnectionHandler();
        DatabaseCompatibilityInterface compatibilityInterface = connectionHandler.getInterfaceProvider().getCompatibilityInterface();
        boolean supportsLogging = DatabaseFeature.DATABASE_LOGGING.isSupported(connectionHandler);
        Presentation presentation = e.getPresentation();
        presentation.setVisible(supportsLogging);
        String databaseLogName = compatibilityInterface.getDatabaseLogName();
        if (Strings.isNotEmpty(databaseLogName)) {
            presentation.setText("Database Logging (" + databaseLogName + ")");
        }
    }
}
