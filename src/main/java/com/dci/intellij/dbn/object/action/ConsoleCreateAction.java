package com.dci.intellij.dbn.object.action;

import com.dci.intellij.dbn.common.action.ProjectAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.dci.intellij.dbn.connection.console.DatabaseConsoleManager;
import com.dci.intellij.dbn.vfs.DBConsoleType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ConsoleCreateAction extends ProjectAction {
    private final DBConsoleType consoleType;
    private final ConnectionRef connection;

    ConsoleCreateAction(ConnectionHandler connection, DBConsoleType consoleType) {
        super("New " + consoleType.getName() + "...");
        this.connection = connection.ref();
        this.consoleType = consoleType;

    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        DatabaseConsoleManager consoleManager = DatabaseConsoleManager.getInstance(project);
        consoleManager.showCreateConsoleDialog(connection.get(), consoleType);
    }
}