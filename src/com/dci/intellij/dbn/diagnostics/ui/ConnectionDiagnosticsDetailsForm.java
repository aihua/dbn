package com.dci.intellij.dbn.diagnostics.ui;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.ui.DBNHeaderForm;
import com.dci.intellij.dbn.common.ui.tab.TabbedPane;
import com.dci.intellij.dbn.common.ui.table.DBNTable;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.diagnostics.ui.model.ConnectivityDiagnosticsTableModel;
import com.dci.intellij.dbn.diagnostics.ui.model.MetadataDiagnosticsTableModel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class ConnectionDiagnosticsDetailsForm extends DBNFormImpl {
    private final DBNTable<MetadataDiagnosticsTableModel> metadataTable;
    private final DBNTable<ConnectivityDiagnosticsTableModel> connectivityTable;

    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel diagnosticsTabsPanel;
    private final TabbedPane diagnosticsTabs;

    public ConnectionDiagnosticsDetailsForm(@NotNull ConnectionDiagnosticsForm parent, ConnectionHandler connection) {
        super(parent);

        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        diagnosticsTabs = new TabbedPane(this);
        diagnosticsTabsPanel.add(diagnosticsTabs, BorderLayout.CENTER);


        MetadataDiagnosticsTableModel metadataTableModel = new MetadataDiagnosticsTableModel(connection);
        metadataTable = new DiagnosticsTable<>(this, metadataTableModel);
        metadataTable.getRowSorter().toggleSortOrder(0);
        addTab(metadataTable, "Metadata Interface");

        ConnectivityDiagnosticsTableModel connectivityTableModel = new ConnectivityDiagnosticsTableModel(connection);
        connectivityTable = new DiagnosticsTable<>(this, connectivityTableModel);
        connectivityTable.getRowSorter().toggleSortOrder(0);
        addTab(connectivityTable, "Database Connectivity");

        diagnosticsTabs.addListener(new TabsListener.Adapter() {
            @Override
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                int selectedIndex = diagnosticsTabs.getTabs().indexOf(newSelection);
                ConnectionDiagnosticsForm parent = Failsafe.nd(parent());
                parent.setTabSelectionIndex(selectedIndex);
            }
        });
   }

    private void addTab(JComponent component, String title) {
        JBScrollPane scrollPane = new JBScrollPane(component);
        TabInfo tabInfo = new TabInfo(scrollPane);
        tabInfo.setText(title);
        tabInfo.setObject(component);
        //tabInfo.setTabColor(GUIUtil.getWindowColor());
        diagnosticsTabs.addTab(tabInfo);
    }

    protected void selectTab(int tabIndex) {
        TabInfo tabInfo = diagnosticsTabs.getTabs().get(tabIndex);
        diagnosticsTabs.select(tabInfo, false);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
