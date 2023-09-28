package com.dci.intellij.dbn.database.interfaces;

import com.dci.intellij.dbn.common.Priority;
import com.dci.intellij.dbn.common.cache.CacheKey;
import com.dci.intellij.dbn.common.thread.ThreadInfo;
import com.dci.intellij.dbn.common.thread.ThreadMonitor;
import com.dci.intellij.dbn.connection.*;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterface.Callable;
import com.dci.intellij.dbn.database.interfaces.queue.InterfaceTaskRequest;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;

import java.sql.SQLException;

import static com.dci.intellij.dbn.database.interfaces.DatabaseInterface.ConnectionCallable;
import static com.dci.intellij.dbn.database.interfaces.DatabaseInterface.ConnectionRunnable;

@UtilityClass
public final class DatabaseInterfaceInvoker {

    /**
     * Database Interface invocation against a pool connection
     * Schedules the task and returns immediately
     */
    public static void schedule(Priority priority, String title, String text, Project project, ConnectionId connectionId, ConnectionRunnable runnable) throws SQLException {
        InterfaceTaskRequest request = InterfaceTaskRequest.create(priority, title, text, project, connectionId, null);
        ConnectionHandler connection = request.getConnection();
        DatabaseInterfaceQueue interfaceQueue = connection.getInterfaceQueue();

        interfaceQueue.scheduleAndForget(request,
                () -> ConnectionContext.surround(request,
                        () -> PooledConnection.run(request, runnable)));
    }


    /**
     * Database Interface invocation against a pool connection
     * Schedules the task and waits for the execution
     *
     * @param priority     the priority of the task
     * @param project
     * @param connectionId the connection to be invoked against
     * @param runnable     the task to be executed
     * @throws SQLException if jdbc call fails
     */
    public static void execute(Priority priority, Project project, ConnectionId connectionId, ConnectionRunnable runnable) throws SQLException {
        execute(priority, null, null, project, connectionId, runnable);
    }

    public static void execute(Priority priority, String title, String text, Project project, ConnectionId connectionId, ConnectionRunnable runnable) throws SQLException {
        execute(priority, title, text, project, connectionId, null, runnable);
    }

    /**
     * @deprecated use {@link #execute(Priority, String, String, Project, ConnectionId, ConnectionRunnable)}
     */
    public static void execute(Priority priority, String title, String text, Project project, ConnectionId connectionId, @Deprecated SchemaId schemaId, ConnectionRunnable runnable) throws SQLException {
        InterfaceTaskRequest request = InterfaceTaskRequest.create(priority, title, text, project, connectionId, schemaId);
        ConnectionHandler connection = request.getConnection();
        DatabaseInterfaceQueue interfaceQueue = connection.getInterfaceQueue();

        ThreadInfo threadInfo = ThreadInfo.copy();
        interfaceQueue.scheduleAndWait(request,
                () -> ConnectionContext.surround(request,
                    () -> ThreadMonitor.surround(project, threadInfo, null,
                        () -> PooledConnection.run(request, runnable))));  }


    /**
     * Database Interface invocation against a pool connection
     * Schedules the task, waits for the execution and returns result
     *
     * @param <T>          type of the entity returned by the invocation
     * @param priority     the priority of the task
     * @param project      the project
     * @param connectionId the connection to be invoked against
     * @param callable     the task to be executed
     * @throws SQLException if jdbc call fails
     */
    public static <T> T load(Priority priority, Project project, ConnectionId connectionId, ConnectionCallable<T> callable) throws SQLException {
        return load(priority, null, null, project, connectionId, callable);
    }

    public static <T> T load(Priority priority, String title, String text, Project project, ConnectionId connectionId, ConnectionCallable<T> callable) throws SQLException {
        InterfaceTaskRequest request = InterfaceTaskRequest.create(priority, title, text, project, connectionId, null);
        ConnectionHandler connection = request.getConnection();
        DatabaseInterfaceQueue interfaceQueue = connection.getInterfaceQueue();

        ThreadInfo threadInfo = ThreadInfo.copy();
        return interfaceQueue.scheduleAndReturn(request,
                () -> ConnectionContext.surround(request,
                    () -> ThreadMonitor.surround(project, threadInfo, null, null,
                        () -> PooledConnection.call(request, callable))));
    }

    public static <T> T cached(CacheKey<T> key, Callable<T> loader) throws SQLException {
        return ConnectionHandler.local().getMetaDataCache().get(key, () -> loader.call());
    }
}
