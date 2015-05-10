package com.dci.intellij.dbn.connection.operation.options.ui;

import com.dci.intellij.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dci.intellij.dbn.connection.operation.options.OperationSettings;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class OperationsSettingsForm extends CompositeConfigurationEditorForm<OperationSettings> {
    private JPanel mainPanel;
    private JPanel transactionSettingsPanel;
    private JPanel sessionBrowserSettings;
    private JPanel compilerPanel;

    public OperationsSettingsForm(OperationSettings settings) {
        super(settings);
        transactionSettingsPanel.add(settings.getTransactionManagerSettings().createComponent(), BorderLayout.CENTER);
        sessionBrowserSettings.add(settings.getSessionBrowserSettings().createComponent(), BorderLayout.CENTER);
        compilerPanel.add(settings.getCompilerSettings().createComponent(), BorderLayout.CENTER);
        resetFormChanges();
    }


    public JPanel getComponent() {
        return mainPanel;
    }
}
