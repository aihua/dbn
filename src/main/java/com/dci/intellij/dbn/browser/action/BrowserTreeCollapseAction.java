package com.dci.intellij.dbn.browser.action;

import com.dci.intellij.dbn.browser.DatabaseBrowserManager;
import com.dci.intellij.dbn.browser.ui.DatabaseBrowserTree;
import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.DumbAwareProjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class BrowserTreeCollapseAction extends DumbAwareProjectAction {
    public BrowserTreeCollapseAction() {
        super("Collapse all", null, Icons.ACTION_COLLAPSE_ALL);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText("Collapse All");
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
        DatabaseBrowserTree activeBrowserTree = browserManager.getActiveBrowserTree();
        if (activeBrowserTree != null) {
            activeBrowserTree.collapseAll();
        }
    }

    @Override
    public boolean isDumbAware() {
        return false;
    }
}
