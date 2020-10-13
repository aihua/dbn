package com.dci.intellij.dbn.editor.data.statusbar;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class DatasetEditorStatusBarWidgetFactory implements StatusBarWidgetFactory {
    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.DatasetEditorStatusBarWidgetFactory";
    }

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "DB Table Math";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return !project.isDefault();
    }

    @NotNull
    @Override
    public StatusBarWidget createWidget(@NotNull Project project) {
        return Failsafe.getComponent(project, DatasetEditorStatusBarWidget.class);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        Disposer.dispose(widget);
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }
}
