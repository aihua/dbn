package com.dci.intellij.dbn.diagnostics.action;

import com.dci.intellij.dbn.common.action.DataKeys;
import com.dci.intellij.dbn.common.action.DumbAwareContextAction;
import com.dci.intellij.dbn.diagnostics.ParserDiagnosticsManager;
import com.dci.intellij.dbn.diagnostics.ui.ParserDiagnosticsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class AbstractParserDiagnosticsAction extends DumbAwareContextAction<ParserDiagnosticsForm> {
    public AbstractParserDiagnosticsAction(String text, Icon icon) {
        super(text, null, icon);
    }

    @Override
    protected final ParserDiagnosticsForm getTarget(@NotNull AnActionEvent e) {
        return e.getData(DataKeys.PARSER_DIAGNOSTICS_FORM);
    }


    @NotNull
    protected ParserDiagnosticsManager getManager(@NotNull Project project) {
        return ParserDiagnosticsManager.get(project);
    }
}
