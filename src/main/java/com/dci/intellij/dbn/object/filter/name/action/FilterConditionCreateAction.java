package com.dci.intellij.dbn.object.filter.name.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.object.filter.name.CompoundFilterCondition;
import com.dci.intellij.dbn.object.filter.name.ObjectNameFilterManager;
import com.dci.intellij.dbn.object.filter.name.SimpleNameFilterCondition;
import com.dci.intellij.dbn.object.filter.name.ui.ObjectNameFilterSettingsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class FilterConditionCreateAction extends AbstractObjectFilterAction {

    public FilterConditionCreateAction(ObjectNameFilterSettingsForm settingsForm) {
        super("Add Filter Condition", Icons.ACTION_ADD, settingsForm);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        Object selection = getSelection();
        ObjectNameFilterManager filterManager = ObjectNameFilterManager.getInstance(project);
        if (selection instanceof CompoundFilterCondition) {
            CompoundFilterCondition condition = (CompoundFilterCondition) selection;
            filterManager.createFilterCondition(condition, settingsForm);
        } else if (selection instanceof SimpleNameFilterCondition) {
            SimpleNameFilterCondition condition = (SimpleNameFilterCondition) selection;
            filterManager.joinFilterCondition(condition, settingsForm);
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        Object selection = getSelection();
        if (selection instanceof CompoundFilterCondition) {
            presentation.setText("Add Condition");
            presentation.setEnabled(true);
            presentation.setIcon(Icons.ACTION_ADD);
        } else if (selection instanceof SimpleNameFilterCondition) {
            presentation.setText("Join Condition");
            presentation.setEnabled(true);
            presentation.setIcon(Icons.ACTION_ADD_SPECIAL);
        } else {
            presentation.setIcon(Icons.ACTION_ADD);
            presentation.setEnabled(false);
        }
    }
}
