package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.browser.DatabaseBrowserManager;
import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.browser.model.BrowserTreeNodeBase;
import com.dci.intellij.dbn.browser.ui.DatabaseBrowserTree;
import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.list.FilteredList;
import com.dci.intellij.dbn.common.options.SettingsChangeNotifier;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.connection.config.ConnectionBundleSettings;
import com.dci.intellij.dbn.connection.config.ConnectionSettings;
import com.dci.intellij.dbn.connection.config.ConnectionSettingsListener;
import com.dci.intellij.dbn.object.common.DBObjectBundle;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

public class ConnectionBundle extends BrowserTreeNodeBase implements BrowserTreeNode, StatefulDisposable {
    private final ProjectRef project;
    private final List<ConnectionHandler> virtualConnections = new ArrayList<>();
    private FilteredList<ConnectionHandler> connections = FilteredList.stateful(c -> c.isEnabled());

    public ConnectionBundle(Project project) {
        this.project = ProjectRef.of(project);
        virtualConnections.add(new VirtualConnectionHandler(
                ConnectionId.VIRTUAL_ORACLE_CONNECTION,
                "Virtual - Oracle 10.1",
                DatabaseType.ORACLE,
                10.1,
                project));

        virtualConnections.add(new VirtualConnectionHandler(
                ConnectionId.VIRTUAL_MYSQL_CONNECTION,
                "Virtual - MySQL 5.0",
                DatabaseType.MYSQL,
                5.0,
                project));

        virtualConnections.add(new VirtualConnectionHandler(
                ConnectionId.VIRTUAL_POSTGRES_CONNECTION,
                "Virtual - PostgreSQL 9.3.4",
                DatabaseType.POSTGRES,
                9.3,
                project));

        virtualConnections.add(new VirtualConnectionHandler(
                ConnectionId.VIRTUAL_SQLITE_CONNECTION,
                "Virtual - SQLite 3.10.2",
                DatabaseType.SQLITE,
                3.10,
                project));

        virtualConnections.add(new VirtualConnectionHandler(
                ConnectionId.VIRTUAL_ISO92_SQL_CONNECTION,
                "Virtual - ISO-92 SQL",
                DatabaseType.GENERIC,
                92,
                project));
    }

    public List<ConnectionHandler> getVirtualConnections() {
        return virtualConnections;
    }

    public ConnectionHandler getVirtualConnection(ConnectionId id) {
        for (ConnectionHandler virtualConnection : virtualConnections) {
            if (virtualConnection.getConnectionId() == id) {
                return virtualConnection;
            }
        }
        return null;
    }

    public void applySettings(ConnectionBundleSettings configuration) {
        FilteredList<ConnectionHandler> newConnectionHandlers = FilteredList.stateful(c -> c.isEnabled());
        List<ConnectionHandler> oldConnectionHandlers = new ArrayList<>(this.connections.getBase());
        List<ConnectionSettings> connectionSettings = configuration.getConnections();
        boolean listChanged = false;
        for (ConnectionSettings connectionSetting : connectionSettings) {
            ConnectionId connectionId = connectionSetting.getConnectionId();
            ConnectionHandler connection = getConnection(oldConnectionHandlers, connectionId);
            if (connection == null) {
                connection = new ConnectionHandlerImpl(this, connectionSetting);
                newConnectionHandlers.add(connection);
                Disposer.register(this, connection);
                listChanged = true;
            } else {
                listChanged = listChanged || connection.isEnabled() != connectionSetting.isActive();
                connection.setSettings(connectionSetting);
                newConnectionHandlers.add(connection);
                oldConnectionHandlers.remove(connection);
            }
        }
        this.connections = newConnectionHandlers;



        listChanged = listChanged || oldConnectionHandlers.size() > 0;
        if (listChanged) {
            Project project = configuration.getProject();
            SettingsChangeNotifier.register(() -> {
                ProjectEvents.notify(project,
                        ConnectionSettingsListener.TOPIC,
                        (listener) -> listener.connectionsChanged());

                ConnectionManager connectionManager = ConnectionManager.getInstance(project);
                connectionManager.disposeConnections(oldConnectionHandlers);
            });
        }
    }

    ConnectionHandler getConnection(List<ConnectionHandler> list, ConnectionId connectionId) {
        for (ConnectionHandler connectionHandler : list) {
            if (connectionHandler.getConnectionId() == connectionId) {
                return connectionHandler;
            }
        }
        return null;
    }

    @Override
    public Icon getIcon(int flags) {
        return Icons.PROJECT;
    }

    @Override
    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    public void addConnection(ConnectionHandler connectionHandler) {
        connections.add(connectionHandler);
        Disposer.register(this, connectionHandler);
    }

    public void setConnections(List<ConnectionHandler> connections) {
        this.connections = FilteredList.stateful(c -> c != null && c.isEnabled(), connections);
    }

    public boolean containsConnection(ConnectionHandler connectionHandler) {
        return connections.contains(connectionHandler);
    }

    public ConnectionHandler getConnection(ConnectionId id) {
        for (ConnectionHandler connectionHandler : connections.getBase()){
            if (connectionHandler.getConnectionId() == id) return connectionHandler;
        }
        return null;
    }

    public List<ConnectionHandler> getConnections() {
        return connections;
    }

    public List<ConnectionHandler> getAllConnections() {
        return connections.getBase();
    }

    /*********************************************************
    *                    NavigationItem                      *
    *********************************************************/
    @Override
    public void navigate(boolean requestFocus) {}

    @Override
    public boolean canNavigate() {
        return true;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    /*********************************************************
    *                  ItemPresentation                      *
    *********************************************************/
    @NotNull
    @Override
    public String getName() {
        return Commons.nvl(getPresentableText(), "Connection Bundle");
    }

    @Override
    public String getLocationString() {
        return null;
    }

    @Override
    public ItemPresentation getPresentation() {
        return this;
    }

    @Override
    public Icon getIcon(boolean open) {
        return getIcon(0);
    }

    /*********************************************************
    *                       TreeElement                     *
    *********************************************************/
    @Override
    public boolean isTreeStructureLoaded() {
        return true;
    }

    @Override
    public void initTreeElement() {}

    @Override
    public boolean canExpand() {
        return true;
    }

    @Override
    @Nullable
    public BrowserTreeNode getParent() {
        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(getProject());
        DatabaseBrowserTree activeBrowserTree = browserManager.getActiveBrowserTree();
        return browserManager.isTabbedMode() ? null : activeBrowserTree == null ? null : activeBrowserTree.getModel().getRoot();
    }

    @Override
    public List<? extends BrowserTreeNode> getChildren() {
        return null;  //should never be used
    }

    @Override
    public void refreshTreeChildren(@NotNull DBObjectType... objectTypes) {
        for (ConnectionHandler connectionHandler : connections) {
            connectionHandler.getObjectBundle().refreshTreeChildren(objectTypes);
        }
    }

    @Override
    public void rebuildTreeChildren() {
        for (ConnectionHandler connectionHandler : connections) {
            connectionHandler.getObjectBundle().rebuildTreeChildren();
        }
    }

    @Override
    public BrowserTreeNode getChildAt(int index) {
        return connections.get(index).getObjectBundle();
    }

    @Override
    public int getChildCount() {
        return connections.size();
    }

    @Override
    public boolean isLeaf() {
        return connections.size() == 0;
    }

    @Override
    public int getIndex(BrowserTreeNode child) {
        DBObjectBundle objectBundle = (DBObjectBundle) child;
        return connections.indexOf(objectBundle.getConnectionHandler());
    }

    @Override
    public int getTreeDepth() {
        return 1;
    }

    @Override
    public String getPresentableText() {
        return "Database connections";
    }

    @Override
    public String getPresentableTextDetails() {
        int size = connections.size();
        return size == 0 ? "(no connections)" : "(" + size + ')';
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return null;
    }

   /*********************************************************
    *                    ToolTipProvider                    *
    *********************************************************/
    @Override
    public String getToolTip() {
        return "";
    }


    public boolean isEmpty() {
        return connections.getBase().isEmpty();
    }


    @Override
    protected void disposeInner() {
        nullify();
    }
}
