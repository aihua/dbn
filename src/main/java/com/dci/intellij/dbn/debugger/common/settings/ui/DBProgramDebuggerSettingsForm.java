package com.dci.intellij.dbn.debugger.common.settings.ui;

import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DBProgramDebuggerSettingsForm extends DBNFormBase {
    private JPanel mainPanel;

    public DBProgramDebuggerSettingsForm() {
        super(null);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
