package com.dci.intellij.dbn.common.action;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.util.Context;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;

public class Lookups {
    protected Lookups(){};

    @Nullable
    public static Project getProject(AnActionEvent e) {
        return e.getData(PlatformDataKeys.PROJECT);
    }

    @NotNull
    public static Project ensureProject(AnActionEvent e) {
        return Failsafe.nn(e.getData(PlatformDataKeys.PROJECT));
    }

    @Nullable
    public static VirtualFile getVirtualFile(@NotNull AnActionEvent e) {
        return e.getData(PlatformDataKeys.VIRTUAL_FILE);
    }

    @Nullable
    public static VirtualFile getVirtualFile(@NotNull Component component) {
        DataContext dataContext = Context.getDataContext(component);
        return getVirtualFile(dataContext);
    }

    @Nullable
    public static VirtualFile getVirtualFile(@NotNull DataContext dataContext) {
        return PlatformDataKeys.VIRTUAL_FILE.getData(dataContext);
    }

    @Nullable
    public static Editor getEditor(@NotNull AnActionEvent e) {
        return e.getData(PlatformDataKeys.EDITOR);
    }

    @Nullable
    public static FileEditor getFileEditor(@NotNull AnActionEvent e) {
        return e.getData(PlatformDataKeys.FILE_EDITOR);
    }

    public static Project getProject(Component component){
        DataContext dataContext = Context.getDataContext(component);
        return PlatformDataKeys.PROJECT.getData(dataContext);
    }

    @Nullable
    public static Object getData(String dataId, DataProvider... dataProviders) {
        for (DataProvider dataProvider : dataProviders) {
            if (dataProvider != null) {
                Object data = dataProvider.getData(dataId);
                if (data != null) {
                    return data;
                }
            }
        }
        return null;
    }
}
