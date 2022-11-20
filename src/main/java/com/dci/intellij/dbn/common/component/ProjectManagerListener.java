package com.dci.intellij.dbn.common.component;

import com.dci.intellij.dbn.common.compatibility.Compatibility;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.VetoableProjectManagerListener;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface ProjectManagerListener {
    Project getProject();

    default boolean canCloseProject() {
        return true;
    }

    default void projectOpened() {}

    default void projectClosed() {}

    static void register(ProjectComponent projectComponent) {
        if (projectComponent instanceof ProjectManagerListener) {

            ProjectManagerListener listener = (ProjectManagerListener) projectComponent;
            VetoableProjectManagerListener projectManagerListener = new VetoableProjectManagerListener() {
                @Override
                public boolean canClose(@NotNull Project project) {
                    return !isSupported(project) || listener.canCloseProject();
                }

                @Override
                @Compatibility
                public boolean canCloseProject(@NotNull Project project) {
                    return !isSupported(project) || listener.canCloseProject();
                }

                @Override
                public void projectOpened(@NotNull Project project) {
                    if (isSupported(project)) listener.projectOpened();
                }

                @Override
                public void projectClosed(@NotNull Project project) {
                    if (isSupported(project)) listener.projectClosed();
                }

                private boolean isSupported(@NotNull Project project) {
                    return !project.isDefault() && Objects.equals(project, listener.getProject());
                }

                @Override
                public String toString() {
                    return projectComponent + "#" + ProjectManagerListener.class.getSimpleName();
                }
            };

            ProjectManager projectManager = ProjectManager.getInstance();

            Project project = projectComponent.getProject();
            projectManager.addProjectManagerListener(project, projectManagerListener);
            //projectManager.addProjectManagerListener(projectManagerListener);
        }
    }
}
