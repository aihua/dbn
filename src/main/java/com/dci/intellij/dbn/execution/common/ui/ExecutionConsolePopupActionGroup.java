package com.dci.intellij.dbn.execution.common.ui;

import com.dci.intellij.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.dci.intellij.dbn.execution.statement.result.ui.RenameExecutionResultDialog;
import com.dci.intellij.dbn.execution.statement.result.ui.StatementExecutionResultForm;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.TabLabel;
import org.jetbrains.annotations.NotNull;

public class ExecutionConsolePopupActionGroup extends DefaultActionGroup {
    private final WeakRef<ExecutionConsoleForm> executionConsoleForm;

    public ExecutionConsolePopupActionGroup(ExecutionConsoleForm executionConsoleForm) {
        this.executionConsoleForm = WeakRef.of(executionConsoleForm);
        add(rename);
        addSeparator();
        add(close);
        add(closeAll);
        add(closeAllButThis);
    }

    public ExecutionConsoleForm getExecutionConsoleForm() {
        return executionConsoleForm.ensure();
    }

    private static TabInfo getTabInfo(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Object o = dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT.getName());
        if (o instanceof TabLabel) {
            TabLabel tabLabel = (TabLabel) o;
            return tabLabel.getInfo();
        }
        return null;
    }

    private final AnAction rename = new AnAction("Rename Result...") {
        @Override
        public void update(@NotNull AnActionEvent e) {
            TabInfo tabInfo = getTabInfo(e);
            boolean visible = false;
            if (tabInfo != null) {
                Object object = tabInfo.getObject();
                visible = object instanceof StatementExecutionResultForm;
            }
            e.getPresentation().setVisible(visible);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            TabInfo tabInfo = getTabInfo(e);
            if (tabInfo != null) {
                Object object = tabInfo.getObject();
                if (object instanceof StatementExecutionResultForm) {
                    StatementExecutionResultForm resultForm = (StatementExecutionResultForm) object;
                    StatementExecutionCursorResult executionResult = resultForm.getExecutionResult();
                    RenameExecutionResultDialog dialog = new RenameExecutionResultDialog(executionResult);
                    dialog.show();
                    tabInfo.setText(executionResult.getName());
                }
            }
        }
    };

    private final AnAction close = new AnAction("Close") {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            TabInfo tabInfo = getTabInfo(e);
            if (tabInfo != null) {
                getExecutionConsoleForm().removeTab(tabInfo);
            }
        }
    };

    private final AnAction closeAll = new AnAction("Close All") {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getExecutionConsoleForm().removeAllTabs();
        }
    };

    private final AnAction closeAllButThis = new AnAction("Close All But This") {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            TabInfo tabInfo = getTabInfo(e);
            if (tabInfo != null) {
                getExecutionConsoleForm().removeAllExceptTab(tabInfo);
            }
        }
    };
}
