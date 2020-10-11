package com.dci.intellij.dbn.browser.options.ui;

import com.dci.intellij.dbn.browser.options.BrowserDisplayMode;
import com.dci.intellij.dbn.browser.options.DatabaseBrowserGeneralSettings;
import com.dci.intellij.dbn.browser.options.listener.DisplayModeSettingsListener;
import com.dci.intellij.dbn.browser.options.listener.ObjectDetailSettingsListener;
import com.dci.intellij.dbn.common.event.EventNotifier;
import com.dci.intellij.dbn.common.options.SettingsChangeNotifier;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorUtil;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dci.intellij.dbn.common.ui.ComboBoxUtil.getSelection;
import static com.dci.intellij.dbn.common.ui.ComboBoxUtil.initComboBox;
import static com.dci.intellij.dbn.common.ui.ComboBoxUtil.setSelection;
import static com.dci.intellij.dbn.common.ui.GUIUtil.updateBorderTitleForeground;

public class DatabaseBrowserGeneralSettingsForm extends ConfigurationEditorForm<DatabaseBrowserGeneralSettings> {
    private JPanel mainPanel;
    private JTextField navigationHistorySizeTextField;
    private JCheckBox showObjectDetailsCheckBox;
    private JComboBox<BrowserDisplayMode> browserTypeComboBox;


    public DatabaseBrowserGeneralSettingsForm(DatabaseBrowserGeneralSettings configuration) {
        super(configuration);
        updateBorderTitleForeground(mainPanel);

        initComboBox(browserTypeComboBox,
                BrowserDisplayMode.SIMPLE,
                BrowserDisplayMode.TABBED);

        resetFormChanges();
        registerComponent(mainPanel);
    }

    @NotNull
    @Override
    public JPanel ensureComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        DatabaseBrowserGeneralSettings configuration = getConfiguration();
        ConfigurationEditorUtil.validateIntegerInputValue(navigationHistorySizeTextField, "Navigation history size", true, 0, 1000, "");

        final boolean repaintTree = configuration.isModified();
        
        final BrowserDisplayMode displayMode = getSelection(browserTypeComboBox);
        final boolean displayModeChanged = configuration.getDisplayMode() != displayMode;
        configuration.setDisplayMode(displayMode);


        configuration.getNavigationHistorySize().to(navigationHistorySizeTextField);
        configuration.getShowObjectDetails().to(showObjectDetailsCheckBox);

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            if (displayModeChanged) {
                EventNotifier.notify(project,
                        DisplayModeSettingsListener.TOPIC,
                        (listener) -> listener.displayModeChanged(displayMode));

            } else if (repaintTree) {
                EventNotifier.notify(project,
                        ObjectDetailSettingsListener.TOPIC,
                        (listener) -> listener.displayDetailsChanged());
            }
        });
    }

    @Override
    public void resetFormChanges() {
        DatabaseBrowserGeneralSettings configuration = getConfiguration();
        setSelection(browserTypeComboBox, configuration.getDisplayMode());

        configuration.getNavigationHistorySize().from(navigationHistorySizeTextField);
        configuration.getShowObjectDetails().from(showObjectDetailsCheckBox);
    }

}
