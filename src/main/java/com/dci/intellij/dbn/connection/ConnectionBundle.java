package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.browser.DatabaseBrowserManager;
import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.browser.model.BrowserTreeNodeBase;
import com.dci.intellij.dbn.browser.ui.DatabaseBrowserTree;
import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Disposed;
import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.list.FilteredList;
import com.dci.intellij.dbn.common.options.SettingsChangeNotifier;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.common.util.Lists;
import com.dci.intellij.dbn.connection.config.ConnectionBundleSettings;
import com.dci.intellij.dbn.connection.config.ConnectionConfigListener;
import com.dci.intellij.dbn.connection.config.ConnectionSettings;
import com.dci.intellij.dbn.object.common.DBObjectBundle;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

import static com.intellij.util.containers.ContainerUtil.createConcurrentWeakKeyWeakValueMap;

public class ConnectionBundle extends BrowserTreeNodeBase implements BrowserTreeNode, StatefulDisposable {
    private final ProjectRef project;
    private final Map<ConnectionId, ConnectionHandler> virtualConnections = new LinkedHashMap<>();
    private FilteredList<ConnectionHandler> connections = FilteredList.stateful(c -> c.isEnabled());
    private Map<ConnectionId, ConnectionHandler> index = createConcurrentWeakKeyWeakValueMap();

    public ConnectionBundle(Project project) {
        this.project = ProjectRef.of(project);
        virtualConnections.put(
                ConnectionId.VIRTUAL_ORACLE,
                new VirtualConnectionHandler(
                        ConnectionId.VIRTUAL_ORACLE,
                        "Virtual - Oracle 10.1",
                        DatabaseType.ORACLE,
                        10.1,
                        this));

        virtualConnections.put(
                ConnectionId.VIRTUAL_MYSQL,
                new VirtualConnectionHandler(
                        ConnectionId.VIRTUAL_MYSQL,
                        "Virtual - MySQL 5.0",
                        DatabaseType.MYSQL,
                        5.0,
                        this));

        virtualConnections.put(ConnectionId.VIRTUAL_POSTGRES,
                new VirtualConnectionHandler(
                        ConnectionId.VIRTUAL_POSTGRES,
                        "Virtual - PostgreSQL 9.3.4",
                        DatabaseType.POSTGRES,
                        9.3,
                        this));

        virtualConnections.put(ConnectionId.VIRTUAL_SQLITE,
                new VirtualConnectionHandler(
                        ConnectionId.VIRTUAL_SQLITE,
                        "Virtual - SQLite 3.10.2",
                        DatabaseType.SQLITE,
                        3.10,
                        this));

        virtualConnections.put(
                ConnectionId.VIRTUAL_ISO92_SQL,
                new VirtualConnectionHandler(
                        ConnectionId.VIRTUAL_ISO92_SQL,
                        "Virtual - ISO-92 SQL",
                        DatabaseType.GENERIC,
                        92,
                        this));
        rebuildIndex();
    }

    public Collection<ConnectionHandler> getVirtualConnections() {
        return virtualConnections.values();
    }

    public ConnectionHandler getVirtualConnection(ConnectionId id) {
        return virtualConnections.get(id);
    }

    public void applySettings(ConnectionBundleSettings configuration) {
        FilteredList<ConnectionHandler> newConnections = FilteredList.stateful(c -> c.isEnabled());
        List<ConnectionHandler> oldConnections = new ArrayList<>(this.connections.getBase());
        List<ConnectionSettings> connectionSettings = configuration.getConnections();
        boolean listChanged = false;
        for (ConnectionSettings connectionSetting : connectionSettings) {
            ConnectionId connectionId = connectionSetting.getConnectionId();
            ConnectionHandler connection = Lists.first(oldConnections, c -> c.getConnectionId() == connectionId);;
            if (connection == null) {
                connection = new ConnectionHandlerImpl(getProject(), connectionSetting);
                newConnections.add(connection);
                Disposer.register(this, connection);
                listChanged = true;
            } else {
                listChanged = listChanged || connection.isEnabled() != connectionSetting.isActive();
                connection.setSettings(connectionSetting);
                newConnections.add(connection);
                oldConnections.remove(connection);
            }
        }
        this.connections = newConnections;

        listChanged = listChanged || oldConnections.size() > 0;
        if (listChanged) {
            Project project = configuration.getProject();
            SettingsChangeNotifier.register(() -> {
                ProjectEvents.notify(project,
                        ConnectionConfigListener.TOPIC,
                        listener -> listener.connectionsChanged());

                for (ConnectionHandler connection : oldConnections) {
                    ConnectionId connectionId = connection.getConnectionId();
                    ProjectEvents.notify(project,
                        ConnectionConfigListener.TOPIC,
                        listener -> listener.connectionRemoved(connectionId));
                }

                ConnectionManager connectionManager = ConnectionManager.getInstance(project);
                connectionManager.disposeConnections(oldConnections);


            });
        }

        rebuildIndex();
    }

    private void rebuildIndex() {
        Map<ConnectionId, ConnectionHandler> index = new HashMap<>();
        for (ConnectionHandler connection : connections.getBase()) {
            index.put(connection.getConnectionId(), connection);
        }
        this.index = index;
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

    @Nullable
    public ConnectionHandler getConnection(@Nullable ConnectionId id) {
        if (id == null) return null;

        ConnectionHandler connection = index.get(id);
        if (connection != null) return connection;

        return virtualConnections.get(id);
    }

    public List<ConnectionHandler> getConnections() {
        return connections;
    }

    public List<ConnectionHandler> getAllConnections() {
        return connections.getBase();
    }

    public int size() {
        return connections.size();
    }

    public int fullSize() {
        return getAllConnections().size();
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
        for (ConnectionHandler connection : connections) {
            connection.getObjectBundle().refreshTreeChildren(objectTypes);
        }
    }

    @Override
    public void rebuildTreeChildren() {
        for (ConnectionHandler connection : connections) {
            connection.getObjectBundle().rebuildTreeChildren();
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
        return connections.indexOf(objectBundle.getConnection());
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
        index = Disposed.map();
        nullify();
    }
}
