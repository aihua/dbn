package com.dci.intellij.dbn.connection.mapping;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.ProjectAction;
import com.dci.intellij.dbn.common.file.VirtualFileRef;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.ref.WeakRef;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.connection.action.AbstractConnectionAction;
import com.dci.intellij.dbn.connection.session.DatabaseSession;
import com.dci.intellij.dbn.connection.session.DatabaseSessionManager;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.action.AnObjectAction;
import com.dci.intellij.dbn.options.ConfigId;
import com.dci.intellij.dbn.options.ProjectSettingsManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ConnectionContextActions {
    static class ConnectionSelectAction extends AbstractConnectionAction {
        private final VirtualFileRef file;
        private final Runnable callback;
        private final boolean promptSchemaSelection;

        ConnectionSelectAction(ConnectionHandler connection, VirtualFile file, boolean promptSchemaSelection, Runnable callback) {
            super(connection.getName(), null, connection.getIcon(), connection);
            this.file = VirtualFileRef.of(file);
            this.callback = callback;
            this.promptSchemaSelection = promptSchemaSelection;
        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull ConnectionHandler connection) {
            VirtualFile file = this.file.get();
            if (file != null) {
                FileConnectionContextManager manager = getContextManager(getProject());
                manager.setConnection(file, connection);
                if (promptSchemaSelection) {
                    manager.promptSchemaSelector(file, e.getDataContext(), callback);
                } else {
                    SchemaId schemaId = manager.getDatabaseSchema(file);
                    if (schemaId == null) {
                        SchemaId defaultSchema = connection.getDefaultSchema();
                        manager.setDatabaseSchema(file, defaultSchema);
                    }
                    if (callback != null) {
                        callback.run();
                    }
                }

            }
        }

        public boolean isSelected() {
            VirtualFile file = this.file.get();
            if (file != null) {
                FileConnectionContextManager manager = getContextManager(getProject());
                ConnectionHandler connection = manager.getConnection(file);
                return connection != null && Objects.equals(connection.getConnectionId(), getConnectionId());
            }
            return false;
        }
    }

    static class ConnectionSetupAction extends ProjectAction {
        private final ProjectRef project;

        ConnectionSetupAction(Project project) {
            super("Setup New Connection", null, Icons.CONNECTION_NEW);
            this.project = ProjectRef.of(project);
        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
            ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(project);
            settingsManager.openProjectSettings(ConfigId.CONNECTIONS);
        }

        @Nullable
        @Override
        public Project getProject() {
            return project.get();
        }
    }

    static class SchemaSelectAction extends AnObjectAction<DBSchema> {
        private final WeakRef<VirtualFile> file;
        private final Runnable callback;

        SchemaSelectAction(VirtualFile file, DBSchema schema, Runnable callback) {
            super(schema);
            this.file = WeakRef.of(file);
            this.callback = callback;
        }

        @Override
        protected void actionPerformed(
                @NotNull AnActionEvent e,
                @NotNull Project project,
                @NotNull DBSchema object) {

            VirtualFile file = this.file.get();
            if (file != null) {
                FileConnectionContextManager manager = getContextManager(getProject());
                manager.setDatabaseSchema(file, getSchemaId());
                if (callback != null) {
                    callback.run();
                }
            }
        }

        @Nullable
        public SchemaId getSchemaId() {
            return SchemaId.from(getTarget());
        }

        public boolean isSelected() {
            VirtualFile file = this.file.get();
            if (file != null) {
                FileConnectionContextManager manager = getContextManager(getProject());
                SchemaId schemaId = manager.getDatabaseSchema(file);
                return schemaId != null && schemaId.equals(getSchemaId());
            }
            return false;
        }
    }

    static class SessionSelectAction extends AnAction {
        private final VirtualFileRef file;
        private final DatabaseSession session;
        private final Runnable callback;

        SessionSelectAction(VirtualFile file, DatabaseSession session, Runnable callback) {
            super(session.getName(), null, session.getIcon());
            this.file = VirtualFileRef.of(file);
            this.session = session;
            this.callback = callback;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            VirtualFile file = this.file.get();
            if (file != null && session != null) {
                FileConnectionContextManager manager = getContextManager(getProject());
                manager.setDatabaseSession(file, session);
                if (callback != null) {
                    callback.run();
                }
            }
        }

        public boolean isSelected() {
            VirtualFile file = this.file.get();
            if (file != null) {
                FileConnectionContextManager manager = getContextManager(getProject());
                DatabaseSession fileSession = manager.getDatabaseSession(file);
                return fileSession != null && fileSession.equals(session);
            }
            return false;
        }

        @NotNull
        private Project getProject() {
            return session.getConnection().getProject();
        }
    }

    static class SessionCreateAction extends DumbAwareAction {
        private final VirtualFileRef file;
        private final ConnectionRef connection;

        SessionCreateAction(VirtualFile file, ConnectionHandler connection) {
            super("New Session...");
            this.file = VirtualFileRef.of(file);
            this.connection = connection.ref();
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            VirtualFile file = this.file.get();
            if (file != null) {
                ConnectionHandler connection = this.connection.ensure();
                Project project = connection.getProject();
                DatabaseSessionManager sessionManager = DatabaseSessionManager.getInstance(project);
                sessionManager.showCreateSessionDialog(
                        connection,
                        (session) -> {
                            if (session != null) {
                                FileConnectionContextManager manager = getContextManager(project);
                                manager.setDatabaseSession(file, session);
                            }
                        });
            }
        }
    }

    @NotNull
    private static FileConnectionContextManager getContextManager(Project project) {
        return FileConnectionContextManager.getInstance(project);
    }
}
