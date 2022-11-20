package com.dci.intellij.dbn.execution.explain.result.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.execution.explain.result.ExplainPlanResult;
import com.dci.intellij.dbn.execution.explain.result.ui.ExplainPlanResultForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dci.intellij.dbn.common.dispose.Checks.isValid;

public class ExplainPlanResultExpandAllAction extends AbstractExplainPlanResultAction {
    public ExplainPlanResultExpandAllAction() {
        super("Expand All", Icons.ACTION_EXPAND_ALL);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull ExplainPlanResult explainPlanResult) {
        ExplainPlanResultForm resultForm = explainPlanResult.getForm();
        if (isValid(resultForm)) {
            resultForm.expandAllNodes();
        }
    }
}
