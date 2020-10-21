package com.dci.intellij.dbn.browser.model;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.ui.tree.TreeEventType;
import com.dci.intellij.dbn.connection.ConnectionBundle;
import com.dci.intellij.dbn.connection.ConnectionCache;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionHandlerStatusListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleBrowserTreeModel extends BrowserTreeModel {
    public SimpleBrowserTreeModel() {
        this(Failsafe.DUMMY_PROJECT, null);
    }

    public SimpleBrowserTreeModel(@NotNull Project project, @Nullable ConnectionBundle connectionBundle) {
        super(new SimpleBrowserTreeRoot(project, connectionBundle));
        ProjectEvents.subscribe(project, this, ConnectionHandlerStatusListener.TOPIC, connectionHandlerStatusListener);
    }

    @Override
    public boolean contains(BrowserTreeNode node) {
        return true;
    }

    private final ConnectionHandlerStatusListener connectionHandlerStatusListener = (connectionId) -> {
        ConnectionHandler connectionHandler = ConnectionCache.findConnectionHandler(connectionId);
        if (connectionHandler != null) {
            notifyListeners(connectionHandler.getObjectBundle(), TreeEventType.NODES_CHANGED);
        }
    };
}
