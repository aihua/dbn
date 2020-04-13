package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.util.EventUtil;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.impl.ProjectLifecycleListener;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConnectionCache implements ApplicationComponent{
    private static final Map<ConnectionId, ConnectionHandler> CACHE = new THashMap<>();

    @Nullable
    public static ConnectionHandler findConnectionHandler(ConnectionId connectionId) {
        ConnectionHandler connectionHandler = CACHE.get(connectionId);
        ProjectManager projectManager = ProjectManager.getInstance();
        if (connectionHandler == null && projectManager != null) {
            synchronized (ConnectionCache.class) {
                connectionHandler = CACHE.get(connectionId);
                if (connectionHandler == null) {
                    for (Project project : projectManager.getOpenProjects()) {
                        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
                        connectionHandler = connectionManager.getConnectionHandler(connectionId);
                        if (Failsafe.check(connectionHandler)) {
                            CACHE.put(connectionId, connectionHandler);
                            return connectionHandler;
                        }
                    }
                }
            }
        }
        return Failsafe.check(connectionHandler) ? connectionHandler : null;
    }


    @Override
    public void initComponent() {
        EventUtil.subscribe(null, ProjectLifecycleListener.TOPIC, projectLifecycleListener);
    }

    @Override
    public void disposeComponent() { }

    @NotNull
    @Override
    public String getComponentName() {
        return "DBNavigator.ConnectionCache";
    }

    /*********************************************************
     *              ProjectLifecycleListener                 *
     *********************************************************/
    private final ProjectLifecycleListener projectLifecycleListener = new ProjectLifecycleListener() {

        @Override
        public void beforeProjectLoaded(@NotNull Project project) {
        }

        @Override
        public void projectComponentsInitialized(@NotNull Project project) {
            if (!project.isDefault()) {
                ConnectionManager connectionManager = ConnectionManager.getInstance(project);
                List<ConnectionHandler> connectionHandlers = connectionManager.getConnectionHandlers();
                for (ConnectionHandler connectionHandler : connectionHandlers) {
                    CACHE.put(connectionHandler.getConnectionId(), connectionHandler);
                }
            }
        }

        @Override
        public void afterProjectClosed(@NotNull Project project) {
            if (!project.isDefault()) {
                Iterator<ConnectionId> connectionIds = CACHE.keySet().iterator();
                while (connectionIds.hasNext()) {
                    ConnectionId connectionId = connectionIds.next();
                    ConnectionHandler connectionHandler = CACHE.get(connectionId);
                    if (connectionHandler.isDisposed() || connectionHandler.getProject() == project) {
                        connectionIds.remove();
                    }
                }
            }
        }
    };
}
