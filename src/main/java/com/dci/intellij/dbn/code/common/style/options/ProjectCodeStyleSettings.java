package com.dci.intellij.dbn.code.common.style.options;

import com.dci.intellij.dbn.code.common.style.options.ui.CodeStyleSettingsForm;
import com.dci.intellij.dbn.code.psql.style.PSQLCodeStyle;
import com.dci.intellij.dbn.code.psql.style.options.PSQLCodeStyleSettings;
import com.dci.intellij.dbn.code.sql.style.SQLCodeStyle;
import com.dci.intellij.dbn.code.sql.style.options.SQLCodeStyleSettings;
import com.dci.intellij.dbn.common.options.CompositeProjectConfiguration;
import com.dci.intellij.dbn.common.options.Configuration;
import com.dci.intellij.dbn.options.ProjectSettings;
import com.dci.intellij.dbn.options.ProjectSettingsManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ProjectCodeStyleSettings extends CompositeProjectConfiguration<ProjectSettings, CodeStyleSettingsForm> {
    public ProjectCodeStyleSettings(ProjectSettings parent){
        super(parent);
    }

    public static ProjectCodeStyleSettings getInstance(@NotNull Project project) {
        return ProjectSettingsManager.getSettings(project).getCodeStyleSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.CodeStyleSettings";
    }

    @Override
    public String getDisplayName() {
        return "Code Style";
    }

    @Override
    @NotNull
    public CodeStyleSettingsForm createConfigurationEditor() {
        return new CodeStyleSettingsForm(this);
    }

    public SQLCodeStyleSettings getSQLCodeStyleSettings() {
        return SQLCodeStyle.settings(getProject());
    }

    public PSQLCodeStyleSettings getPSQLCodeStyleSettings() {
        return PSQLCodeStyle.settings(getProject());
    }

    /*********************************************************
    *                     Configuration                      *
    *********************************************************/
    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                getSQLCodeStyleSettings(),
                getPSQLCodeStyleSettings()};
    }
}
