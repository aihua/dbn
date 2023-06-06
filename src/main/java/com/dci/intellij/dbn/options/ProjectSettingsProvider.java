package com.dci.intellij.dbn.options;

import com.dci.intellij.dbn.common.compatibility.Workaround;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.util.Unsafe;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectSettingsProvider extends ConfigurableProvider{
    public static final String PROJECT_CONFIGURABLE = "com.intellij.projectConfigurable";
    private final ProjectRef project;

    public ProjectSettingsProvider(Project project) {
        this.project = ProjectRef.of(project);
    }


    /**
     * ConfigurableEP is logging wrapped ProcessCancelledException when provider is
     * initialized in background and cancelled (e.g. on all-actions invocation)
     * (initialize the provider upfront)
     */
    @Workaround // https://youtrack.jetbrains.com/issue/IDEA-313711
    public static void init(Project project) {
        Unsafe.silent(() -> {
            ConfigurableEP<?>[] extensions = Configurable.PROJECT_CONFIGURABLE.getExtensions(project);
            for (ConfigurableEP<?> extension : extensions) {
                if (ProjectSettingsProvider.class.getName().equals(extension.providerClass)) {
                    ConfigurableWrapper.wrapConfigurable(extension);
                    break;
                }
            }
        });
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
