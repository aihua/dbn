package com.dci.intellij.dbn.connection.mapping;

import com.dci.intellij.dbn.common.editor.EditorNotificationProvider;
import com.dci.intellij.dbn.common.event.ProjectEvents;
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
    public FileConnectionContextNotificationPanel createNotificationPanel(@NotNull VirtualFile virtualFile, @NotNull FileEditor fileEditor, @NotNull Project project) {
        FileConnectionContextNotificationPanel notificationPanel = null;

        FileType fileType = virtualFile.getFileType();
        if (fileType != SQLFileType.INSTANCE && fileType != PSQLFileType.INSTANCE) {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            FileConnectionContext connectionMapping = contextManager.getMapping(virtualFile);
            if (connectionMapping != null) {
                notificationPanel = new FileConnectionContextNotificationPanel(project, virtualFile, connectionMapping);
            }
        }
        return notificationPanel;
    }

    public static final FileConnectionContextListener mappingListener = new FileConnectionContextListener() {
        @Override
        public void mappingChanged(Project project, VirtualFile file) {
            if (file instanceof VirtualFileWindow) {
                VirtualFileWindow fileWindow = (VirtualFileWindow) file;
                file = fileWindow.getDelegate();
            }
            EditorNotifications notifications = EditorNotifications.getInstance(project);
            notifications.updateNotifications(file);
        }
    };
}
