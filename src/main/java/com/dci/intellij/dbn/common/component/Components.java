package com.dci.intellij.dbn.common.component;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class Components {
    private Components() {}


    @NotNull
    public static <T extends ProjectComponent> T projectService(@NotNull Project project, @NotNull Class<T> interfaceClass) {
        return ServiceManager.getService(Failsafe.nd(project), interfaceClass);
    }

    @NotNull
    public static <T extends ApplicationComponent> T applicationService(@NotNull Class<T> interfaceClass) {
        return ServiceManager.getService(interfaceClass);
    }
}
