package com.dci.intellij.dbn.options;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.component.ApplicationComponentBase;
import com.dci.intellij.dbn.common.component.PersistentState;
import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.project.Projects;
import com.dci.intellij.dbn.common.util.InternalApi;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dci.intellij.dbn.common.component.Components.applicationService;

@State(
    name = "DBNavigator.DefaultProject.Settings",
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DefaultProjectSettingsManager extends ApplicationComponentBase implements PersistentState {

    private final Latent<ProjectSettings> defaultProjectSettings = Latent.basic(() -> createProjectSettings());

    @NotNull
    private ProjectSettings createProjectSettings() {
        Project defaultProject = Projects.getDefaultProject();
        return new ProjectSettings(defaultProject);
    }

    private DefaultProjectSettingsManager() {
        super("DBNavigator.Application.TemplateProjectSettings");
    }

    public static DefaultProjectSettingsManager getInstance() {
        return applicationService(DefaultProjectSettingsManager.class);
    }

    @NotNull
    public ProjectSettings getDefaultProjectSettings() {
        return defaultProjectSettings.get();
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        ProjectSettings projectSettings = getDefaultProjectSettings();
        Element element = new Element("state");
        projectSettings.writeConfiguration(element);
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        ProjectSettings projectSettings = getDefaultProjectSettings();
        projectSettings.readConfiguration(element);
    }
}
