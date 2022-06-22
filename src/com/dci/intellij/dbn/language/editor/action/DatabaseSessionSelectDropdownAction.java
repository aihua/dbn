package com.dci.intellij.dbn.language.editor.action;

import com.dci.intellij.dbn.common.action.Lookups;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.ui.misc.DBNComboBoxAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionType;
import com.dci.intellij.dbn.connection.mapping.FileConnectionContextManager;
import com.dci.intellij.dbn.connection.session.DatabaseSession;
import com.dci.intellij.dbn.connection.session.DatabaseSessionBundle;
import com.dci.intellij.dbn.vfs.DBConsoleType;
import com.dci.intellij.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.util.List;

public class DatabaseSessionSelectDropdownAction extends DBNComboBoxAction implements DumbAware {
    private static final String NAME = "Session";

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent component) {
        Project project = Lookups.getProject(component);
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        VirtualFile virtualFile = Lookups.getVirtualFile(component);
        if (virtualFile != null) {
            ConnectionHandler connection = FileConnectionContextManager.getInstance(project).getConnection(virtualFile);
            if (Failsafe.check(connection) && !connection.isVirtual()) {
                DatabaseSessionBundle sessionBundle = connection.getSessionBundle();

                if (isDebugConsole(virtualFile)) {
                    actionGroup.add(new DatabaseSessionSelectAction(sessionBundle.getDebugSession()));
                } else {
                    actionGroup.add(new DatabaseSessionSelectAction(sessionBundle.getMainSession()));
                    actionGroup.add(new DatabaseSessionSelectAction(sessionBundle.getPoolSession()));
                    List<DatabaseSession> sessions = sessionBundle.getSessions(ConnectionType.SESSION);
                    if (sessions.size() > 0) {
                        //actionGroup.addSeparator();
                        for (DatabaseSession session : sessions){
                            actionGroup.add(new DatabaseSessionSelectAction(session));
                        }
                    }
                    actionGroup.addSeparator();
                    actionGroup.add(new DatabaseSessionCreateAction(connection));
                    actionGroup.add(new DatabaseSessionDisableAction(connection));
                }
            }
        }
        return actionGroup;
    }

    private boolean isDebugConsole(VirtualFile virtualFile) {
        boolean isDebugConsole = false;
        if (virtualFile instanceof DBConsoleVirtualFile) {
            DBConsoleVirtualFile consoleVirtualFile = (DBConsoleVirtualFile) virtualFile;
            if (consoleVirtualFile.getType() == DBConsoleType.DEBUG) {
                isDebugConsole = true;
            }
        }
        return isDebugConsole;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = Lookups.getProject(e);
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        String text = NAME;

        Icon icon = null;
        boolean visible = false;
        boolean enabled = true;

        if (project != null && virtualFile != null) {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            ConnectionHandler connection = contextManager.getConnection(virtualFile);
            visible = connection != null && !connection.isVirtual() && connection.getSettings().getDetailSettings().isEnableSessionManagement();
            if (visible) {
                if (isDebugConsole(virtualFile)) {
                    DatabaseSession debugSession = connection.getSessionBundle().getDebugSession();
                    text = debugSession.getName();
                    icon = debugSession.getIcon();
                    enabled = false;
                } else {
                    DatabaseSession session = contextManager.getDatabaseSession(virtualFile);
                    if (session != null) {
                        text = session.getName();
                        icon = session.getIcon();
                        enabled = true;
/*
                    // TODO allow selecting "hot" session?
                    DatabaseSession databaseSession = contextManager.getDatabaseSession(virtualFile);
                    if (databaseSession != null) {
                        DBNConnection connection = connection.getConnectionPool().getSessionConnection(databaseSession.getId());
                        enabled = connection == null || !connection.hasDataChanges();

                    } else {
                        enabled = true;
                    }
*/

                    }
                }
            }
        }

        Presentation presentation = e.getPresentation();
        presentation.setText(text, false);
        presentation.setDescription("Select database session");
        presentation.setIcon(icon);
        presentation.setVisible(visible);
        presentation.setEnabled(enabled);
    }

    @Override
    protected boolean shouldShowDisabledActions() {
        return true;
    }
}
