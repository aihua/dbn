package com.dci.intellij.dbn.common.options;

import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.project.ProjectStateManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class BasicProjectConfiguration<P extends ProjectConfiguration, E extends ConfigurationEditorForm>
        extends BasicConfiguration<P, E>
        implements ProjectConfiguration<P, E> {

    private ProjectRef project;

    public BasicProjectConfiguration(@NotNull P parent) {
        super(parent);
        Disposer.register(parent, this);
    }

    public BasicProjectConfiguration(@NotNull Project project) {
        super(null);
        this.project = ProjectRef.of(project);
        ProjectStateManager.registerDisposable(project, this);
    }

    @NotNull
    @Override
    public Project getProject() {
        if (project != null) {
            return project.ensure();
        }

        return getParent().ensureProject();
    }
}
