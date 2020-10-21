package com.dci.intellij.dbn.debugger.common.config.ui;

import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.debugger.common.config.DBRunConfig;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;

public abstract class DBProgramRunConfigurationEditorForm<T extends DBRunConfig> extends DBNFormImpl {

    public DBProgramRunConfigurationEditorForm(Project project) {
        super(null, project);
    }

    public abstract void readConfiguration(T configuration);

    public abstract void writeConfiguration(T configuration) throws ConfigurationException;
}
