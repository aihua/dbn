package com.dci.intellij.dbn.browser.action;

import com.dci.intellij.dbn.browser.DatabaseBrowserManager;
import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.DumbAwareProjectAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.config.ui.ConnectionFilterSettingsDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ConnectionFilterSettingsOpenAction extends DumbAwareProjectAction {
    ConnectionFilterSettingsOpenAction() {
        super("Object Filter Settings...", null, Icons.DATASET_FILTER);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText("Object Filter Settings...");

        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
        ConnectionHandler activeConnection = browserManager.getActiveConnection();
        presentation.setEnabled(activeConnection != null);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
        ConnectionHandler activeConnection = browserManager.getActiveConnection();
        if (activeConnection != null) {
            ConnectionFilterSettingsDialog filterSettingsDialog = new ConnectionFilterSettingsDialog(activeConnection);
            filterSettingsDialog.show();
        }
    }
}
