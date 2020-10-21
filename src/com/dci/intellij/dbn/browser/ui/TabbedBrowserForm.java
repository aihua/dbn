package com.dci.intellij.dbn.browser.ui;

import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.environment.options.EnvironmentVisibilitySettings;
import com.dci.intellij.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.ui.tab.TabbedPane;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.connection.ConnectionBundle;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.tabs.TabInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class TabbedBrowserForm extends DatabaseBrowserForm{
    private final TabbedPane connectionTabs;
    private JPanel mainPanel;

    TabbedBrowserForm(@NotNull Project project, @Nullable TabbedBrowserForm previous) {
        super(project);
        connectionTabs = new TabbedPane(this);
        //connectionTabs.setBackground(GUIUtil.getListBackground());
        //mainPanel.add(connectionTabs, BorderLayout.CENTER);
        initTabs(previous);
        ProjectEvents.subscribe(project, this, EnvironmentManagerListener.TOPIC, environmentManagerListener);
    }


    private void initTabs(@Nullable TabbedBrowserForm previous) {
        Project project = ensureProject();

        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        for (ConnectionHandler connectionHandler: connectionBundle.getConnectionHandlers()) {
            ConnectionId connectionId = connectionHandler.getConnectionId();
            SimpleBrowserForm browserForm = previous == null ? null : previous.removeBrowserForm(connectionId);
            if (browserForm == null) {
                browserForm = new SimpleBrowserForm(this, connectionHandler);
            } else {
                browserForm.setParent(this);
            }

            JComponent component = browserForm.getComponent();
            TabInfo tabInfo = new TabInfo(component);
            tabInfo.setText(CommonUtil.nvl(connectionHandler.getName(), "[unnamed connection]"));
            tabInfo.setObject(browserForm);
            //tabInfo.setIcon(connectionHandler.getIcon());
            this.connectionTabs.addTab(tabInfo);

            EnvironmentType environmentType = connectionHandler.getEnvironmentType();
            tabInfo.setTabColor(environmentType.getColor());
        }
        if (this.connectionTabs.getTabCount() == 0) {
            mainPanel.removeAll();
            mainPanel.add(new JBList(new ArrayList()), BorderLayout.CENTER);
        } else {
            if (mainPanel.getComponentCount() > 0) {
                Component component = mainPanel.getComponent(0);
                if (component != this.connectionTabs) {
                    mainPanel.removeAll();
                    mainPanel.add(this.connectionTabs, BorderLayout.CENTER);
                }
            } else {
                mainPanel.add(this.connectionTabs, BorderLayout.CENTER);
            }
        }
    }

    @Nullable
    private SimpleBrowserForm getBrowserForm(ConnectionId connectionId) {
        for (TabInfo tabInfo : getConnectionTabs().getTabs()) {
            SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
            ConnectionHandler connectionHandler = browserForm.getConnectionHandler();
            if (connectionHandler != null && connectionHandler.getConnectionId() == connectionId) {
                return browserForm;
            }
        }
        return null;
    }

    @Nullable
    private SimpleBrowserForm removeBrowserForm(ConnectionId connectionId) {
        TabbedPane connectionTabs = getConnectionTabs();
        for (TabInfo tabInfo : connectionTabs.getTabs()) {
            SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
            ConnectionId tabConnectionId = browserForm.getConnectionId();
            if (tabConnectionId == connectionId) {
                connectionTabs.removeTab(tabInfo, false);
                return browserForm;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    @Nullable
    public DatabaseBrowserTree getBrowserTree() {
        return getActiveBrowserTree();
    }

    @Nullable
    public DatabaseBrowserTree getBrowserTree(ConnectionId connectionId) {
        SimpleBrowserForm browserForm = getBrowserForm(connectionId);
        return browserForm== null ? null : browserForm.getBrowserTree();
    }

    @Nullable
    public DatabaseBrowserTree getActiveBrowserTree() {
        TabInfo tabInfo = getConnectionTabs().getSelectedInfo();
        if (tabInfo != null) {
            SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
            return browserForm.getBrowserTree();
        }
        return null;
    }

    @Override
    public void selectElement(BrowserTreeNode treeNode, boolean focus, boolean scroll) {
        ConnectionId connectionId = treeNode.getConnectionId();
        SimpleBrowserForm browserForm = getBrowserForm(connectionId);
        if (browserForm != null) {
            getConnectionTabs().select(browserForm.getComponent(), focus);
            if (scroll) {
                browserForm.selectElement(treeNode, focus, true);
            }
        }
    }

    @Override
    public void rebuildTree() {
        for (TabInfo tabInfo : getConnectionTabs().getTabs()) {
            SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
            browserForm.rebuildTree();
        }
    }

    @NotNull
    public TabbedPane getConnectionTabs() {
        return Failsafe.nn(connectionTabs);
    }

    /********************************************************
     *                       Listeners                      *
     ********************************************************/
    private final EnvironmentManagerListener environmentManagerListener = new EnvironmentManagerListener() {
        @Override
        public void configurationChanged(Project project) {
            EnvironmentVisibilitySettings visibilitySettings = getEnvironmentSettings(project).getVisibilitySettings();
            for (TabInfo tabInfo : getConnectionTabs().getTabs()) {
                try {
                    SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
                    ConnectionHandler connectionHandler = browserForm.getConnectionHandler();
                    if (connectionHandler != null) {
                        JBColor environmentColor = connectionHandler.getEnvironmentType().getColor();
                        if (visibilitySettings.getConnectionTabs().value()) {
                            tabInfo.setTabColor(environmentColor);
                        } else {
                            tabInfo.setTabColor(null);
                        }
                    }
                } catch (ProcessCanceledException ignore) {}
            }
        }
    };

    void refreshTabInfo(ConnectionId connectionId) {
        for (TabInfo tabInfo : connectionTabs.getTabs()) {
            SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
            ConnectionHandler connectionHandler = browserForm.getConnectionHandler();
            if (connectionHandler != null) {
                if (connectionHandler.getConnectionId() == connectionId) {
                    tabInfo.setText(connectionHandler.getName());
                    break;
                }
            }
        }

    }
}

