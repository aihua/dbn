package com.dci.intellij.dbn.diagnostics.ui;

import com.dci.intellij.dbn.common.dispose.DisposableContainers;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.util.UserInterface;
import com.dci.intellij.dbn.connection.ConnectionBundle;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.dci.intellij.dbn.connection.config.ConnectionConfigListener;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

import static com.dci.intellij.dbn.common.ui.util.Borderless.markBorderless;

public class ConnectionDiagnosticsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel detailsPanel;
    private JList<ConnectionHandler> connectionsList;
    private int tabSelectionIndex;

    private final Map<ConnectionId, ConnectionDiagnosticsDetailsForm> resourceMonitorForms = DisposableContainers.map(this);

    public ConnectionDiagnosticsForm(@NotNull Project project) {
        super(null, project);
        connectionsList.addListSelectionListener(e -> {
            ConnectionHandler connection = connectionsList.getSelectedValue();
            showDetailsForm(connection);
        });
        connectionsList.setCellRenderer(new ConnectionListCellRenderer());

        ListModel<ConnectionHandler> model = createModel();
        connectionsList.setModel(model);
        connectionsList.setSelectedIndex(0);
        markBorderless(connectionsList);

        ProjectEvents.subscribe(project, this,
                ConnectionConfigListener.TOPIC,
                ConnectionConfigListener.whenSetupChanged(() -> rebuildModel()));
    }

    private void rebuildModel() {
        ListModel<ConnectionHandler> model = createModel();
        connectionsList.setModel(model);
    }

    @NotNull
    private ListModel<ConnectionHandler> createModel() {
        DefaultListModel<ConnectionHandler> model = new DefaultListModel<>();
        ConnectionManager connectionManager = ConnectionManager.getInstance(ensureProject());
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        List<ConnectionHandler> connections = connectionBundle.getConnections();
        for (ConnectionHandler connection : connections) {
            model.addElement(connection);
        }
        return model;
    }

    private void showDetailsForm(ConnectionHandler connection) {
        detailsPanel.removeAll();
        if (connection != null) {
            ConnectionId connectionId = connection.getConnectionId();
            ConnectionDiagnosticsDetailsForm detailForm = resourceMonitorForms.get(connectionId);
            if (detailForm == null) {
                detailForm = new ConnectionDiagnosticsDetailsForm(this, connection);
                resourceMonitorForms.put(connectionId, detailForm);
            }
            detailsPanel.add(detailForm.getComponent(), BorderLayout.CENTER);
            detailForm.selectTab(tabSelectionIndex);
        }

        UserInterface.repaint(detailsPanel);
    }

    public void setTabSelectionIndex(int tabSelectionIndex) {
        this.tabSelectionIndex = tabSelectionIndex;
    }

    private static class ConnectionListCellRenderer extends ColoredListCellRenderer<ConnectionHandler> {

        @Override
        protected void customizeCellRenderer(@NotNull JList list, ConnectionHandler value, int index, boolean selected, boolean hasFocus) {
            setIcon(value.getIcon());
/*            if (!selected) {
                JBColor color = Commons.nvl(value.getEnvironmentType().getColor(), JBColor.WHITE);
                setBackground(Colors.softer(color, 30));
            }*/
            append(value.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }


    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
