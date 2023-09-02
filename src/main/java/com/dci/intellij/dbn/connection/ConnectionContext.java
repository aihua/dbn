package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterface;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterface.Callable;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterface.Runnable;
import com.intellij.openapi.project.Project;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Objects;

@Data
public class ConnectionContext {
    private ProjectRef project;
    private final ConnectionId connectionId;
    private final SchemaId schemaId;

    public ConnectionContext(Project project, ConnectionId connectionId, SchemaId schemaId) {
        this.project = ProjectRef.of(project);
        this.connectionId = connectionId;
        this.schemaId = schemaId;
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return ConnectionHandler.ensure(connectionId);
    }

    @Nullable
    public Project getProject() {
        return ProjectRef.get(project);
    }

    public static <T> T surround(ConnectionContext context, Callable<T> callable) throws SQLException {
        return Local.surround(context, callable);
    }

    public static void surround(ConnectionContext context, Runnable runnable) throws SQLException {
        Local.surround(context, runnable);
    }

    public static ConnectionContext local() {
        return Local.get();
    }

    /**
     * Thread local connection context
     */
    private static class Local {
        private static final ThreadLocal<ConnectionContext> LOCAL = new ThreadLocal<>();

        private Local() {}

        static ConnectionContext get() {
            ConnectionContext context = LOCAL.get();
            if (context == null) throw new IllegalStateException("Connection context not initialized");
            return context;
        }

        static <T> T surround(ConnectionContext context, Callable<T> callable) throws SQLException {
            boolean initialized = init(context);
            try {
                return callable.call();
            } finally {
                release(initialized);
            }
        }

        static void surround(ConnectionContext context, DatabaseInterface.Runnable runnable) throws SQLException {
            boolean initialized = init(context);
            try {
                runnable.run();
            } finally {
                release(initialized);
            }
        }

        static boolean init(ConnectionContext context) {
            ConnectionContext localContext = LOCAL.get();
            if (localContext == null) {
                LOCAL.set(context);
                return true;
            }

            ConnectionId connectionId = context.getConnectionId();
            ConnectionId localConnectionId = localContext.getConnectionId();
            if (!Objects.equals(connectionId, localConnectionId)) {
                throw new IllegalStateException("Context already initialized for another connection");
            }

            return false;
        }

        static void release(boolean initialized) {
            if (!initialized) return;
            LOCAL.remove();
        }
    }
}
