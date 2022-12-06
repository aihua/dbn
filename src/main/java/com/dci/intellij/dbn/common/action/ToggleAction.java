package com.dci.intellij.dbn.common.action;

import com.dci.intellij.dbn.common.compatibility.Compatibility;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class ToggleAction
        extends com.intellij.openapi.actionSystem.ToggleAction
        implements BackgroundUpdatedAction {

    public ToggleAction() {
    }

    public ToggleAction(@Nullable String text) {
        super(text);
    }

    public ToggleAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }


    @NotNull
    @Compatibility
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    @Compatibility
    public boolean isUpdateInBackground() {
        return true;
    }
}
