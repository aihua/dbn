package com.dci.intellij.dbn.common.project;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dci.intellij.dbn.common.dispose.Failsafe.nd;

public interface ProjectSupplier {
    @Nullable
    Project getProject();


    @NotNull
    default Project ensureProject() {
        return nd(getProject());
    }
}
