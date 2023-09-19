package com.dci.intellij.dbn.plugin;

import com.dci.intellij.dbn.common.file.FileTypeService;
import com.dci.intellij.dbn.common.project.Projects;
import com.dci.intellij.dbn.debugger.ExecutionConfigManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.dci.intellij.dbn.DatabaseNavigator.DBN_PLUGIN_ID;

public class DBNPluginStateListener implements PluginStateListener {
    @Override
    public void install(@NotNull IdeaPluginDescriptor descriptor) {
    }

    @Override
    public void uninstall(@NotNull IdeaPluginDescriptor descriptor) {
        if (!Objects.equals(descriptor.getPluginId(), DBN_PLUGIN_ID)) return;

        // bye bye...
        FileTypeService fileTypeService = FileTypeService.getInstance();
        fileTypeService.restoreFileAssociations();

        Project[] projects = Projects.getOpenProjects();
        for (Project project : projects) {
            ExecutionConfigManager executionConfigManager = ExecutionConfigManager.getInstance(project);
            executionConfigManager.removeRunConfigurations();
        }

        PluginConflictManager conflictManager = PluginConflictManager.getInstance();
        conflictManager.setConflictPrompted(false);
        conflictManager.setFileTypesClaimed(false);
    }
}
