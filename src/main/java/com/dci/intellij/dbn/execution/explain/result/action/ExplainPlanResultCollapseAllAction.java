package com.dci.intellij.dbn.execution.explain.result.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.execution.explain.result.ExplainPlanResult;
import com.dci.intellij.dbn.execution.explain.result.ui.ExplainPlanResultForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ExplainPlanResultCollapseAllAction extends AbstractExplainPlanResultAction {
    public ExplainPlanResultCollapseAllAction() {
        super("Collapse All", Icons.ACTION_COLLAPSE_ALL);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull ExplainPlanResult explainPlanResult) {
        ExplainPlanResultForm resultForm = explainPlanResult.getForm();
        if (Failsafe.check(resultForm)) {
            resultForm.collapseAllNodes();
        }
    }
}
