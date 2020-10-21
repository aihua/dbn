package com.dci.intellij.dbn.common.environment.options.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.environment.EnvironmentTypeBundle;
import com.dci.intellij.dbn.common.environment.options.EnvironmentSettings;
import com.dci.intellij.dbn.common.environment.options.EnvironmentVisibilitySettings;
import com.dci.intellij.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.options.SettingsChangeNotifier;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

import static com.dci.intellij.dbn.common.ui.GUIUtil.updateBorderTitleForeground;

public class EnvironmentSettingsForm extends ConfigurationEditorForm<EnvironmentSettings> {
    private JPanel mainPanel;
    private JCheckBox connectionTabsCheckBox;
    private JCheckBox objectEditorTabsCheckBox;
    private JCheckBox scriptEditorTabsCheckBox;
    private JCheckBox dialogHeadersCheckBox;
    private JCheckBox executionResultTabsCheckBox;
    private JPanel environmentTypesPanel;
    private JPanel environmentApplicabilityPanel;
    private JPanel environmentTypesTablePanel;
    private EnvironmentTypesEditorTable environmentTypesTable;

    public EnvironmentSettingsForm(EnvironmentSettings settings) {
        super(settings);
        environmentTypesTable = new EnvironmentTypesEditorTable(this, settings.getEnvironmentTypes());

        updateBorderTitleForeground(environmentTypesPanel);
        updateBorderTitleForeground(environmentApplicabilityPanel);

        EnvironmentVisibilitySettings visibilitySettings = settings.getVisibilitySettings();
        visibilitySettings.getConnectionTabs().from(connectionTabsCheckBox);
        visibilitySettings.getObjectEditorTabs().from(objectEditorTabsCheckBox);
        visibilitySettings.getScriptEditorTabs().from(scriptEditorTabsCheckBox);
        visibilitySettings.getDialogHeaders().from(dialogHeadersCheckBox);
        visibilitySettings.getExecutionResultTabs().from(executionResultTabsCheckBox);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(environmentTypesTable);
        decorator.setAddAction(anActionButton -> environmentTypesTable.insertRow());
        decorator.setRemoveAction(anActionButton -> environmentTypesTable.removeRow());
        decorator.setMoveUpAction(anActionButton -> environmentTypesTable.moveRowUp());
        decorator.setMoveDownAction(anActionButton -> environmentTypesTable.moveRowDown());
        decorator.addExtraAction(new AnActionButton("Revert Changes", Icons.ACTION_REVERT_CHANGES) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                TableCellEditor cellEditor = environmentTypesTable.getCellEditor();
                if (cellEditor != null) {
                    cellEditor.cancelCellEditing();
                }
                environmentTypesTable.setEnvironmentTypes(EnvironmentTypeBundle.DEFAULT);
            }

        });
        decorator.setPreferredSize(new Dimension(-1, 400));
        JPanel panel = decorator.createPanel();
        environmentTypesTablePanel.add(panel, BorderLayout.CENTER);
        environmentTypesTable.getParent().setBackground(environmentTypesTable.getBackground());
        registerComponents(mainPanel);
    }
    
    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
    
    @Override
    public void applyFormChanges() throws ConfigurationException {
        EnvironmentSettings configuration = getConfiguration();
        EnvironmentTypesTableModel model = environmentTypesTable.getModel();
        model.validate();
        EnvironmentTypeBundle environmentTypeBundle = model.getEnvironmentTypes();
        boolean settingsChanged = configuration.setEnvironmentTypes(environmentTypeBundle);

        EnvironmentVisibilitySettings visibilitySettings = configuration.getVisibilitySettings();
        boolean visibilityChanged =
            visibilitySettings.getConnectionTabs().to(connectionTabsCheckBox) ||
            visibilitySettings.getObjectEditorTabs().to(objectEditorTabsCheckBox) ||
            visibilitySettings.getScriptEditorTabs().to(scriptEditorTabsCheckBox)||
            visibilitySettings.getDialogHeaders().to(dialogHeadersCheckBox)||
            visibilitySettings.getExecutionResultTabs().to(executionResultTabsCheckBox);

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            if (settingsChanged || visibilityChanged) {
                ProjectEvents.notify(project,
                        EnvironmentManagerListener.TOPIC,
                        (listener) -> listener.configurationChanged(project));
            }
        });
    }

    @Override
    public void resetFormChanges() {
        EnvironmentSettings settings = getConfiguration();
        environmentTypesTable.getModel().setEnvironmentTypes(settings.getEnvironmentTypes());

        EnvironmentVisibilitySettings visibilitySettings = settings.getVisibilitySettings();
        visibilitySettings.getConnectionTabs().from(connectionTabsCheckBox);
        visibilitySettings.getObjectEditorTabs().from(objectEditorTabsCheckBox);
        visibilitySettings.getScriptEditorTabs().from(scriptEditorTabsCheckBox);
        visibilitySettings.getDialogHeaders().from(dialogHeadersCheckBox);
        visibilitySettings.getExecutionResultTabs().from(executionResultTabsCheckBox);
    }
}
