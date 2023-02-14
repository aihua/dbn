package com.dci.intellij.dbn.connection.mapping;

import com.dci.intellij.dbn.common.editor.EditorNotificationProvider;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.util.Editors;
import com.dci.intellij.dbn.connection.mapping.ui.FileConnectionContextNotificationPanel;
import com.dci.intellij.dbn.language.psql.PSQLFileType;
import com.dci.intellij.dbn.language.sql.SQLFileType;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileConnectionContextNotificationProvider extends EditorNotificationProvider<FileConnectionContextNotificationPanel> {
    private static final Key<FileConnectionContextNotificationPanel> KEY = Key.create("DBNavigator.FileConnectionMappingNotificationPanel");
    public FileConnectionContextNotificationProvider() {
        ProjectEvents.subscribe(FileConnectionContextListener.TOPIC, mappingListener);
    }

    @Deprecated
    public FileConnectionContextNotificationProvider(@NotNull Project project) {
        super(project);
        ProjectEvents.subscribe(project, this, FileConnectionContextListener.TOPIC, mappingListener);
    }

    @NotNull
    @Override
    public Key<FileConnectionContextNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public FileConnectionContextNotificationPanel createComponent(@NotNull VirtualFile virtualFile, @NotNull FileEditor fileEditor, @NotNull Project project) {

        FileType fileType = virtualFile.getFileType();
        if (fileType == SQLFileType.INSTANCE || fileType == PSQLFileType.INSTANCE) return null;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        FileConnectionContext mapping = contextManager.getMapping(virtualFile);
        if (mapping == null) return null;

        return new FileConnectionContextNotificationPanel(project, virtualFile, mapping);
    }

    public static final FileConnectionContextListener mappingListener = new FileConnectionContextListener() {
        @Override
        public void mappingChanged(Project project, VirtualFile file) {
            if (file instanceof VirtualFileWindow) {
                VirtualFileWindow fileWindow = (VirtualFileWindow) file;
                file = fileWindow.getDelegate();
            }
            EditorNotifications notifications = Editors.getNotifications(project);;
            notifications.updateNotifications(file);
        }
    };
}
