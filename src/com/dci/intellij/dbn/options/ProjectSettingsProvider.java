package com.dci.intellij.dbn.options;

import com.dci.intellij.dbn.common.project.ProjectRef;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectSettingsProvider extends ConfigurableProvider{
    private final ProjectRef project;

    public ProjectSettingsProvider(Project project) {
        this.project = ProjectRef.of(project);
    }

    @Nullable
    @Override
    public Configurable createConfigurable() {
        Project project = getProject();
        ProjectSettings projectSettings = project.isDefault() ?
                DefaultProjectSettingsManager.getInstance().getDefaultProjectSettings() :
                ProjectSettingsManager.getInstance(project).getProjectSettings();

        return projectSettings.clone();
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }
}
