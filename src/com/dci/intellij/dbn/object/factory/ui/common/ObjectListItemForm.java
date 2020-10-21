package com.dci.intellij.dbn.object.factory.ui.common;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.util.ActionUtil;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class ObjectListItemForm extends DBNFormImpl {
    private JPanel mainPanel;
    private JPanel removeActionPanel;
    private JPanel objectDetailsComponent;

    private final ObjectFactoryInputForm<?> inputForm;

    ObjectListItemForm(@NotNull ObjectListForm<?> parent, ObjectFactoryInputForm<?> inputForm) {
        super(parent);
        this.inputForm = inputForm;
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar(
                "DBNavigator.ObjectFactory.AddElement", true,
                new RemoveObjectAction());
        removeActionPanel.add(actionToolbar.getComponent(), BorderLayout.NORTH);

    }

    @NotNull
    public ObjectListForm<?> getParentForm() {
        return ensureParent();
    }

    @NotNull
    @Override
    public JPanel getMainComponent(){
        return mainPanel;
    }

    private void createUIComponents() {
        objectDetailsComponent = (JPanel) inputForm.getComponent();
    }

    public class RemoveObjectAction extends AnAction {
        RemoveObjectAction() {
            super("Remove " + getParentForm().getObjectType().getName(), null, Icons.ACTION_CLOSE);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getParentForm().removeObjectPanel(ObjectListItemForm.this);
        }
    }

    ObjectFactoryInputForm<?> getObjectDetailsPanel() {
        return inputForm;
    }
}
