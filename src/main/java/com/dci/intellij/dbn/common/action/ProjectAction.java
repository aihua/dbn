package com.dci.intellij.dbn.common.action;

import com.dci.intellij.dbn.common.util.Commons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.dci.intellij.dbn.common.dispose.Checks.isValid;
import static com.dci.intellij.dbn.common.dispose.Failsafe.guarded;

public abstract class ProjectAction extends BasicAction implements DumbAware {

    public ProjectAction() {}

    public ProjectAction(@Nullable String text) {
        super(text);
    }

    public ProjectAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public final void update(@NotNull AnActionEvent e) {
        guarded(this, a -> {
            Project project = a.resolveProject(e);
            if (isValid(project)) a.update(e, project);
        });
    }

    @Override
    public final void actionPerformed(@NotNull AnActionEvent e) {
        guarded(this, a -> {
            Project project = a.resolveProject(e);
            if (isValid(project)) a.actionPerformed(e, project);
        });
    }

    @Nullable
    private Project resolveProject(@NotNull AnActionEvent e) {
        return Commons.coalesce(
                () -> getProject(),
                () -> Lookups.getProject(e));
    }

    /**
     * fallback when project cannot be loaded from the data context (TODO check why)
     */
    @Nullable
    public Project getProject() {
        return null;
    }

    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
    }

    protected abstract void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project);





}

