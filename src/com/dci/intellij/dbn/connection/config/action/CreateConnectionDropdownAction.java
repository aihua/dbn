package com.dci.intellij.dbn.connection.config.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.DataKeys;
import com.dci.intellij.dbn.common.action.GroupPopupAction;
import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.connection.DatabaseType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class CreateConnectionDropdownAction extends GroupPopupAction {
    private AnAction[] actions = new AnAction[] {
            new ConnectionCreateAction(DatabaseType.ORACLE),
            new ConnectionCreateAction(DatabaseType.MYSQL),
            new ConnectionCreateAction(DatabaseType.POSTGRES),
            new ConnectionCreateAction(DatabaseType.SQLITE),
            new ConnectionCreateAction(null),
            Actions.SEPARATOR,
            new TnsNamesImportAction()
    };

    public CreateConnectionDropdownAction() {
        super("New Connection", null, Icons.ACTION_ADD_MORE);
    }

    public CreateConnectionDropdownAction(String name, @Nullable String groupTitle, @Nullable Icon icon) {
        super(name, groupTitle, icon);
    }

    @Override
    public DataProvider getDataProvider(AnActionEvent e) {
        return e.getData((DataKeys.CONNECTION_BUNDLE_SETTINGS));
    }

    @Override
    protected AnAction[] getActions(AnActionEvent e) {
        return actions;
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        DataProvider dataProvider = getDataProvider(e);
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(dataProvider != null);
        presentation.setText("New Connection");
    }
}
