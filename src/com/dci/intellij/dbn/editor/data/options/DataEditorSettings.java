package com.dci.intellij.dbn.editor.data.options;

import com.dci.intellij.dbn.common.options.CompositeProjectConfiguration;
import com.dci.intellij.dbn.common.options.Configuration;
import com.dci.intellij.dbn.editor.data.options.ui.DataEditorSettingsForm;
import com.dci.intellij.dbn.options.ConfigId;
import com.dci.intellij.dbn.options.ProjectSettings;
import com.dci.intellij.dbn.options.ProjectSettingsManager;
import com.dci.intellij.dbn.options.TopLevelConfig;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class DataEditorSettings extends CompositeProjectConfiguration<ProjectSettings, DataEditorSettingsForm> implements TopLevelConfig {
    private final @Getter DataEditorPopupSettings popupSettings                       = new DataEditorPopupSettings(this);
    private final @Getter DataEditorValueListPopupSettings valueListPopupSettings     = new DataEditorValueListPopupSettings(this);
    private final @Getter DataEditorFilterSettings filterSettings                     = new DataEditorFilterSettings(this);
    private final @Getter DataEditorGeneralSettings generalSettings                   = new DataEditorGeneralSettings(this);
    private final @Getter DataEditorQualifiedEditorSettings qualifiedEditorSettings   = new DataEditorQualifiedEditorSettings(this);
    private final @Getter DataEditorRecordNavigationSettings recordNavigationSettings = new DataEditorRecordNavigationSettings(this);

    public DataEditorSettings(ProjectSettings parent) {
        super(parent);
    }

    public static DataEditorSettings getInstance(@NotNull Project project) {
        return ProjectSettingsManager.getSettings(project).getDataEditorSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.DataEditorSettings";
    }

    @Override
    public String getDisplayName() {
        return "Data Editor";
    }

    @Override
    public String getHelpTopic() {
        return "dataEditor";
    }

    @Override
    public ConfigId getConfigId() {
        return ConfigId.DATA_EDITOR;
    }

    @NotNull
    @Override
    public DataEditorSettings getOriginalSettings() {
        return getInstance(getProject());
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @Override
    @NotNull
    public DataEditorSettingsForm createConfigurationEditor() {
        return new DataEditorSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "dataset-editor-settings";
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                popupSettings,
                valueListPopupSettings,
                generalSettings,
                filterSettings,
                qualifiedEditorSettings,
                recordNavigationSettings};
    }
}
