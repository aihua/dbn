package com.dci.intellij.dbn.connection.operation.options.ui;

import com.dci.intellij.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dci.intellij.dbn.connection.operation.options.OperationSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class OperationsSettingsForm extends CompositeConfigurationEditorForm<OperationSettings> {
    private JPanel mainPanel;
    private JPanel transactionSettingsPanel;
    private JPanel sessionBrowserSettings;
    private JPanel compilerPanel;
    private JPanel debuggerPanel;

    public OperationsSettingsForm(OperationSettings settings) {
        super(settings);
        transactionSettingsPanel.add(settings.getTransactionManagerSettings().createComponent(), BorderLayout.CENTER);
        sessionBrowserSettings.add(settings.getSessionBrowserSettings().createComponent(), BorderLayout.CENTER);
        compilerPanel.add(settings.getCompilerSettings().createComponent(), BorderLayout.CENTER);
        debuggerPanel.add(settings.getDebuggerSettings().createComponent(), BorderLayout.CENTER);
        resetFormChanges();
    }


    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
