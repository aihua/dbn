package com.dci.intellij.dbn.diagnostics.action;

import com.dci.intellij.dbn.common.ui.DBNComboBoxAction;
import com.dci.intellij.dbn.diagnostics.data.ParserDiagnosticsFilter;
import com.dci.intellij.dbn.diagnostics.data.StateTransition;
import com.dci.intellij.dbn.diagnostics.ui.ParserDiagnosticsForm;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public class ParserDiagnosticsStateFilterAction extends DBNComboBoxAction implements DumbAware {
    private final ParserDiagnosticsForm form;

    public ParserDiagnosticsStateFilterAction(ParserDiagnosticsForm form) {
        this.form = form;
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent component) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new SelectFilterValueAction(null));
        actionGroup.addSeparator();
        for (StateTransition.Category transitionCategory : StateTransition.Category.values()) {
            actionGroup.add(new SelectFilterValueAction(transitionCategory));
        }
        return actionGroup;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        ParserDiagnosticsFilter resultFilter = getResultFilter();
        StateTransition.Category stateCategory = resultFilter.getStateCategory();
        presentation.setText(stateCategory == null ? "state" : stateCategory.name(), false);
    }

    private ParserDiagnosticsFilter getResultFilter() {
        return form.getManager().getResultFilter();
    }

    private class SelectFilterValueAction extends AnAction {
        private final StateTransition.Category stateCategory;

        public SelectFilterValueAction(StateTransition.Category transitionCategory) {
            super(transitionCategory == null ? "No Filter" : transitionCategory.name());
            this.stateCategory = transitionCategory;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getResultFilter().setStateCategory(stateCategory);
            form.refreshResult();
        }
    }
 }