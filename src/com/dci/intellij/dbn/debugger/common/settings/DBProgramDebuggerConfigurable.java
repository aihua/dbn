package com.dci.intellij.dbn.debugger.common.settings;

import com.dci.intellij.dbn.common.dispose.SafeDisposer;
import com.dci.intellij.dbn.debugger.common.settings.ui.DBProgramDebuggerSettingsForm;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

public class DBProgramDebuggerConfigurable implements Configurable {

    private transient DBProgramDebuggerSettingsForm component;

    @Override
    @Nls
    public String getDisplayName() {
        return "Database";
    }

    public Icon getIcon() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        component = new DBProgramDebuggerSettingsForm();
        return component.getComponent();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }

    @Override
    public void reset() {
    }

    @Override
    public void disposeUIResources() {
        SafeDisposer.dispose(component);
        component = null;
    }
}
