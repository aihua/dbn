package com.dci.intellij.dbn.object.filter.name.action;

import com.dci.intellij.dbn.common.action.DumbAwareProjectAction;
import com.dci.intellij.dbn.object.filter.name.ui.ObjectNameFilterSettingsForm;

import javax.swing.*;
import javax.swing.tree.TreePath;

public abstract class AbstractObjectFilterAction extends DumbAwareProjectAction {
    ObjectNameFilterSettingsForm settingsForm;

    AbstractObjectFilterAction(String text, Icon icon, ObjectNameFilterSettingsForm settingsForm) {
        super(text, null, icon);
        this.settingsForm = settingsForm;
    }

    protected Object getSelection() {
        TreePath selectionPath = getFiltersTree().getSelectionPath();
        return selectionPath == null ? null : selectionPath.getLastPathComponent();
    }

    JTree getFiltersTree() {
        return settingsForm.getFiltersTree();
    }
}
