package com.dci.intellij.dbn.execution.script.ui;

import com.dci.intellij.dbn.common.ui.dialog.DBNDialog;
import com.dci.intellij.dbn.execution.script.CmdLineInterface;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Set;

public class CmdLineInterfaceInputDialog extends DBNDialog<CmdLineInterfaceInputForm> {
    private CmdLineInterface cmdLineInterface;
    private Set<String> usedNames;

    public CmdLineInterfaceInputDialog(Project project, @NotNull CmdLineInterface cmdLineInterface, @NotNull Set<String> usedNames) {
        super(project, "Add command-line interface", true);
        this.cmdLineInterface = cmdLineInterface;
        this.usedNames = usedNames;
        setModal(true);
        Action okAction = getOKAction();
        okAction.putValue(Action.NAME, "Save");
        init();
    }

    @NotNull
    @Override
    protected CmdLineInterfaceInputForm createForm() {
        return new CmdLineInterfaceInputForm(this, cmdLineInterface, usedNames);
    }

    @Override
    protected String getDimensionServiceKey() {
        return null;
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction(),
        };
    }

    void setActionEnabled(boolean enabled) {
        getOKAction().setEnabled(enabled);
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}
