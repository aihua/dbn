package com.dci.intellij.dbn.common.component;

import com.dci.intellij.dbn.common.thread.ThreadMonitor;
import com.dci.intellij.dbn.common.thread.ThreadProperty;
import com.dci.intellij.dbn.common.util.Unsafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PersistentState extends PersistentStateComponent<Element> {
    @Nullable
    default Project getProject() {
        return null;
    }

    @Override
    @Nullable
    default Element getState() {
        return ThreadMonitor.surround(
                getProject(),
                ThreadProperty.COMPONENT_STATE,
                () -> Unsafe.warned(null,
                        () -> getComponentState()));
    }

    @Override
    default void loadState(@NotNull Element state) {
        ThreadMonitor.surround(
                getProject(),
                ThreadProperty.COMPONENT_STATE,
                () -> Unsafe.warned(
                        () -> loadComponentState(state)));
    }

    Element getComponentState();

    void loadComponentState(@NotNull Element state);
}
