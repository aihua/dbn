package com.dci.intellij.dbn.code.psql.style.options.ui;

import com.dci.intellij.dbn.code.common.style.options.DBLCodeStyleSettings;
import com.dci.intellij.dbn.code.psql.style.options.PSQLCodeStyleSettings;
import com.dci.intellij.dbn.common.options.ui.CompositeConfigurationEditorForm;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class PSQLCodeStyleSettingsEditorForm extends CompositeConfigurationEditorForm<DBLCodeStyleSettings> {
    private JPanel mainPanel;
    private JPanel casePanel;
    private JPanel previewPanel;
    private JPanel attributesPanel;

    public PSQLCodeStyleSettingsEditorForm(PSQLCodeStyleSettings settings) {
        super(settings);
        casePanel.add(settings.getCaseSettings().createComponent(), BorderLayout.CENTER);
        attributesPanel.add(settings.getFormattingSettings().createComponent(), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}