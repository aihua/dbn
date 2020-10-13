package com.dci.intellij.dbn.editor.data.options.ui;

import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.data.record.navigation.RecordNavigationTarget;
import com.dci.intellij.dbn.editor.data.options.DataEditorRecordNavigationSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dci.intellij.dbn.common.ui.ComboBoxUtil.getSelection;
import static com.dci.intellij.dbn.common.ui.ComboBoxUtil.initComboBox;
import static com.dci.intellij.dbn.common.ui.ComboBoxUtil.setSelection;
import static com.dci.intellij.dbn.common.ui.GUIUtil.updateBorderTitleForeground;

public class DataEditorRecordNavigationSettingsForm extends ConfigurationEditorForm<DataEditorRecordNavigationSettings> {
    private JPanel mainPanel;
    private JComboBox<RecordNavigationTarget> navigationTargetComboBox;


    public DataEditorRecordNavigationSettingsForm(DataEditorRecordNavigationSettings configuration) {
        super(configuration);
        updateBorderTitleForeground(mainPanel);
        initComboBox(navigationTargetComboBox,
                RecordNavigationTarget.EDITOR,
                RecordNavigationTarget.VIEWER,
                RecordNavigationTarget.ASK);

        resetFormChanges();
        registerComponent(mainPanel);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        DataEditorRecordNavigationSettings configuration = getConfiguration();

        RecordNavigationTarget navigationTarget = getSelection(navigationTargetComboBox);
        configuration.setNavigationTarget(navigationTarget);
    }

    @Override
    public void resetFormChanges() {
        DataEditorRecordNavigationSettings configuration = getConfiguration();
        RecordNavigationTarget navigationTarget = configuration.getNavigationTarget();
        setSelection(navigationTargetComboBox, navigationTarget);
    }
}
