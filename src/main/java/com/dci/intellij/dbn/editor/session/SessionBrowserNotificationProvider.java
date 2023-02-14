package com.dci.intellij.dbn.editor.session;

import com.dci.intellij.dbn.common.compatibility.LegacyEditorNotificationsProvider;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.util.Editors;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.editor.session.ui.SessionBrowserErrorNotificationPanel;
import com.dci.intellij.dbn.vfs.file.DBSessionBrowserVirtualFile;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SessionBrowserNotificationProvider extends LegacyEditorNotificationsProvider<SessionBrowserErrorNotificationPanel> {
    private static final Key<SessionBrowserErrorNotificationPanel> KEY = Key.create("DBNavigator.SessionBrowserErrorNotificationPanel");

    public SessionBrowserNotificationProvider() {
        ProjectEvents.subscribe(SessionBrowserLoadListener.TOPIC, sessionBrowserLoadListener());
    }

    @Deprecated
    public SessionBrowserNotificationProvider(@NotNull Project project) {
        super(project);
        ProjectEvents.subscribe(project, this, SessionBrowserLoadListener.TOPIC, sessionBrowserLoadListener());

    }

    @NotNull
    private static SessionBrowserLoadListener sessionBrowserLoadListener() {
        return virtualFile -> {
            if (virtualFile instanceof DBSessionBrowserVirtualFile) {
                DBSessionBrowserVirtualFile databaseFile = (DBSessionBrowserVirtualFile) virtualFile;
                Project project = databaseFile.getProject();
                EditorNotifications notifications = Editors.getNotifications(project);;
                notifications.updateNotifications(virtualFile);
            }
        };
    }

    @NotNull
    @Override
    public Key<SessionBrowserErrorNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public SessionBrowserErrorNotificationPanel createComponent(@NotNull VirtualFile virtualFile, @NotNull FileEditor fileEditor, @NotNull Project project) {
        if (!(virtualFile instanceof DBSessionBrowserVirtualFile)) return null;
        if (!(fileEditor instanceof SessionBrowser)) return null;

        SessionBrowser sessionBrowser = (SessionBrowser) fileEditor;
        ConnectionHandler connection = sessionBrowser.getConnection();
        String error = sessionBrowser.getModelError();
        if (Strings.isEmpty(error)) return null;

        return createPanel(connection, error);
    }

    private static SessionBrowserErrorNotificationPanel createPanel(ConnectionHandler connection, String sourceLoadError) {
        return new SessionBrowserErrorNotificationPanel(connection, sourceLoadError);
    }
}
