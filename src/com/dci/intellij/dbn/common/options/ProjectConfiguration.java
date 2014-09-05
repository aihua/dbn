package com.dci.intellij.dbn.common.options;

import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.options.GlobalProjectSettings;
import com.intellij.openapi.project.Project;

public abstract class ProjectConfiguration<T extends ConfigurationEditorForm> extends Configuration<T> {
    private Project project;

    public ProjectConfiguration(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }
}
