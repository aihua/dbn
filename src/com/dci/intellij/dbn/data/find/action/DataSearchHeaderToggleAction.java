package com.dci.intellij.dbn.data.find.action;

import com.dci.intellij.dbn.common.ui.DBNCheckboxAction;
import com.dci.intellij.dbn.data.find.DataSearchComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class DataSearchHeaderToggleAction extends DBNCheckboxAction implements DumbAware {
    private final DataSearchComponent searchComponent;

    protected DataSearchHeaderToggleAction(DataSearchComponent searchComponent, String text) {
        super(text);
        this.searchComponent = searchComponent;
    }

    public DataSearchComponent getEditorSearchComponent() {
        return searchComponent;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @Override
    public JComponent createCustomComponent(@NotNull Presentation presentation) {
        final JComponent customComponent = super.createCustomComponent(presentation);
        if (customComponent instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) customComponent;
            checkBox.setFocusable(false);
            checkBox.setOpaque(false);
        }
        return customComponent;
    }
}
