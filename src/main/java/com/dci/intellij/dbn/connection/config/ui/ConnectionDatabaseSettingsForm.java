package com.dci.intellij.dbn.connection.config.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.database.AuthenticationInfo;
import com.dci.intellij.dbn.common.database.DatabaseInfo;
import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.options.ConfigurationHandle;
import com.dci.intellij.dbn.common.options.SettingsChangeNotifier;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorUtil;
import com.dci.intellij.dbn.common.ui.form.DBNHintForm;
import com.dci.intellij.dbn.common.ui.util.UserInterface;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.connection.*;
import com.dci.intellij.dbn.connection.config.*;
import com.dci.intellij.dbn.connection.config.file.DatabaseFileBundle;
import com.dci.intellij.dbn.driver.DriverSource;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Objects;

import static com.dci.intellij.dbn.common.ui.util.ComboBoxes.*;
import static java.awt.event.KeyEvent.VK_UNDEFINED;

@SuppressWarnings("unused")
public class ConnectionDatabaseSettingsForm extends ConfigurationEditorForm<ConnectionDatabaseSettings> {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JTextField descriptionTextField;
    private JComboBox<DatabaseType> databaseTypeComboBox;
    private JPanel driverLibraryPanel;
    private JLabel databaseTypeLabel;
    private JPanel authenticationPanel;
    private JPanel urlPanel;
    private JPanel databaseTypeHintPanel;

    private final ConnectionUrlSettingsForm urlSettingsForm;
    private final ConnectionDriverSettingsForm driverSettingsForm;
    private final ConnectionAuthenticationSettingsForm authSettingsForm;

    private DatabaseType selectedDatabaseType;

    public ConnectionDatabaseSettingsForm(ConnectionDatabaseSettings configuration) {
        super(configuration);

        ConnectionConfigType configType = configuration.getConfigType();

        selectedDatabaseType = configuration.getDatabaseType();
        if (configType == ConnectionConfigType.CUSTOM) {
            initComboBox(databaseTypeComboBox,
                    DatabaseType.ORACLE,
                    DatabaseType.MYSQL,
                    DatabaseType.POSTGRES,
                    DatabaseType.SQLITE,
                    DatabaseType.GENERIC);
        } else {
            databaseTypeLabel.setText(selectedDatabaseType.getName());
            databaseTypeLabel.setIcon(selectedDatabaseType.getIcon());
            databaseTypeLabel.setDisplayedMnemonic(VK_UNDEFINED);
            initComboBox(databaseTypeComboBox, selectedDatabaseType);
            setSelection(databaseTypeComboBox, selectedDatabaseType);
            databaseTypeComboBox.setEnabled(false);
            databaseTypeComboBox.setVisible(false);
        }

        urlSettingsForm = new ConnectionUrlSettingsForm(this, configuration);
        authSettingsForm = new ConnectionAuthenticationSettingsForm(this);
        driverSettingsForm = new ConnectionDriverSettingsForm(this);
		boolean externalLibrary = configuration.getDriverSource() == DriverSource.EXTERNAL;

        urlPanel.add(urlSettingsForm.getComponent(), BorderLayout.CENTER);
        authenticationPanel.add(authSettingsForm.getComponent(), BorderLayout.CENTER);
        driverLibraryPanel.add(driverSettingsForm.getComponent(), BorderLayout.CENTER);

        resetFormChanges();
        registerComponent(mainPanel);

        DatabaseType databaseType = configuration.getDatabaseType();
        AuthenticationType[] authTypes = databaseType.getAuthTypes();

        urlSettingsForm.updateFieldVisibility();
        authenticationPanel.setVisible(databaseType.supportsAuthentication());


        if (configType == ConnectionConfigType.CUSTOM) {
            databaseTypeComboBox.addActionListener(e -> databaseTypeChanged());
            driverSettingsForm.getDriverComboBox().addActionListener(e -> updateNativeSupportDatabaseHint());
            updateNativeSupportDatabaseHint();
        }
    }

    protected void databaseTypeChanged() {
        DatabaseType oldDatabaseType = selectedDatabaseType;
        DatabaseType newDatabaseType = getSelection(databaseTypeComboBox);
        selectedDatabaseType = newDatabaseType;

        AuthenticationType[] authTypes = newDatabaseType.getAuthTypes();

        urlSettingsForm.handleDatabaseTypeChange(oldDatabaseType, newDatabaseType);
        authenticationPanel.setVisible(newDatabaseType.supportsAuthentication());
        driverSettingsForm.updateDriverFields();

        updateNativeSupportDatabaseHint();
    }

    void notifyPresentationChanges() {
        ConnectionDatabaseSettings configuration = getConfiguration();
        String name = nameTextField.getText();
        ConnectivityStatus connectivityStatus = configuration.getConnectivityStatus();
        ConnectionSettings connectionSettings = configuration.getParent();
        ConnectionSettingsForm connectionSettingsForm = connectionSettings.getSettingsEditor();

        Icon icon = connectionSettings.isNew() ? Icons.CONNECTION_NEW :
                connectionSettingsForm != null && !connectionSettingsForm.isConnectionActive() ? Icons.CONNECTION_DISABLED :
                        connectivityStatus == ConnectivityStatus.VALID ? Icons.CONNECTION_CONNECTED :
                        connectivityStatus == ConnectivityStatus.INVALID ? Icons.CONNECTION_INVALID : Icons.CONNECTION_INACTIVE;

        EnvironmentType environmentType = connectionSettings.getDetailSettings().getEnvironmentType();
        JBColor color = environmentType.getColor();
        ConnectionId connectionId = configuration.getConnectionId();
        DatabaseType databaseType = configuration.getDatabaseType();

        ProjectEvents.notify(
                configuration.getProject(),
                ConnectionPresentationChangeListener.TOPIC,
                (listener) -> listener.presentationChanged(name, icon, color, connectionId, databaseType));
    }

    //protected abstract ConnectionDatabaseSettings createConfig(ConnectionSettings configuration);

    @Override
    protected DocumentListener createDocumentListener() {
        return new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                ConnectionDatabaseSettings configuration = getConfiguration();
                configuration.setModified(true);

                Document document = e.getDocument();

                if (document == driverSettingsForm.getDriverLibraryTextField().getTextField().getDocument()) {
                    driverSettingsForm.updateDriverFields();
                }

                if (document == nameTextField.getDocument()) {
                    ConnectionBundleSettings connectionBundleSettings = configuration.getParent().getParent();
                    ConnectionBundleSettingsForm settingsEditor = connectionBundleSettings.getSettingsEditor();
                    if (settingsEditor != null) {
                        JList<?> connectionList = settingsEditor.getList();
                        UserInterface.repaint(connectionList);
                        notifyPresentationChanges();
                    }
                }
            }
        };
    }


    @Override
    protected ActionListener createActionListener() {
        return e -> {
            Object source = e.getSource();
            ConnectionDatabaseSettings configuration = getConfiguration();
            configuration.setModified(true);
            if (source == nameTextField) {
                ConnectionBundleSettings connectionBundleSettings = configuration.getParent().getParent();
                ConnectionBundleSettingsForm settingsEditor = connectionBundleSettings.getSettingsEditor();

                if (settingsEditor != null) {
                    JList<?> connectionList = settingsEditor.getList();
                    UserInterface.repaint(connectionList);
                    notifyPresentationChanges();
                }
            }
        };
    }

	public String getConnectionName() {
        return nameTextField.getText();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges(final ConnectionDatabaseSettings configuration) throws ConfigurationException {
        DatabaseType databaseType = getSelectedDatabaseType();
        DriverOption driverOption = driverSettingsForm.getDriverOption();
        DatabaseUrlType urlType = Commons.nvl(urlSettingsForm.getUrlType(), DatabaseUrlType.CUSTOM);

        configuration.setDatabaseType(databaseType);
        configuration.setName(nameTextField.getText());
        configuration.setDescription(descriptionTextField.getText());
        configuration.setDriverLibrary(driverSettingsForm.getDriverLibrary());
        configuration.setDriver(driverOption == null ? null : driverOption.getName());
        configuration.setUrlPattern(DatabaseUrlPattern.get(databaseType, urlType));

        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        databaseInfo.reset();

        databaseInfo.setUrlType(urlType);
        databaseInfo.setUrl(urlSettingsForm.getUrl());

        if (urlType == DatabaseUrlType.TNS) {
        	databaseInfo.setTnsFolder(urlSettingsForm.getTnsFolder());
        	databaseInfo.setTnsProfile(urlSettingsForm.getTnsProfile());
        } else if (urlType == DatabaseUrlType.FILE){
            DatabaseFileBundle fileBundle = urlSettingsForm.getFileBundle();
            fileBundle.validate();
            databaseInfo.setFileBundle(fileBundle);
        } else if (urlType != DatabaseUrlType.CUSTOM){
            databaseInfo.setHost(urlSettingsForm.getHost());
            databaseInfo.setPort(urlSettingsForm.getPort());
            databaseInfo.setDatabase(urlSettingsForm.getDatabase());
        }

        AuthenticationInfo authenticationInfo = configuration.getAuthenticationInfo();
        String oldUserName = authenticationInfo.getUser();
        String oldPassword = authenticationInfo.getPassword();
        authSettingsForm.applyFormChanges(authenticationInfo);
        if (!ConfigurationHandle.isTransitory()) {
            authenticationInfo.updateKeyChain(oldUserName, oldPassword);
        }

        configuration.setDriverSource(driverSettingsForm.getDriverSource());
        configuration.updateSignature();
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ConfigurationEditorUtil.validateStringValue(nameTextField, "Name", true);
        ConnectionDatabaseSettings configuration = getConfiguration();

        DatabaseType selectedDatabaseType = getSelectedDatabaseType();
        DatabaseType driverDatabaseType = driverSettingsForm.getDriverDatabaseType();
        if (driverDatabaseType != null && driverDatabaseType != selectedDatabaseType) {
            if (selectedDatabaseType == DatabaseType.GENERIC) {
                // TODO hint there is dedicated support for the database type resolved from driver
            } else {
                throw new ConfigurationException("The provided driver library is not a valid " + selectedDatabaseType.getName() + " driver library.");
            }
        }

        boolean nameChanged = !Objects.equals(nameTextField.getText(), configuration.getName());

        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        boolean settingsChanged = urlSettingsForm.settingsChanged() ||
                //!connectionConfig.getProperties().equals(propertiesEditorForm.getProperties()) ||
                !Commons.match(configuration.getDatabaseType(), selectedDatabaseType) ||
                !Commons.match(configuration.getDriverLibrary(), driverSettingsForm.getDriverLibrary()) ||
                !Commons.match(configuration.getAuthenticationInfo().getUser(), authSettingsForm.getUser());


        applyFormChanges(configuration);

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            ConnectionId connectionId = configuration.getConnectionId();
            if (nameChanged) {
                ProjectEvents.notify(project,
                        ConnectionConfigListener.TOPIC,
                        listener -> listener.connectionNameChanged(connectionId));
            }

            if (settingsChanged) {
                ProjectEvents.notify(project,
                        ConnectionConfigListener.TOPIC,
                        listener -> listener.connectionChanged(connectionId));
            }
        });
    }

    @NotNull
    DatabaseType getSelectedDatabaseType() {
        ConnectionDatabaseSettings configuration = getConfiguration();;
        return Commons.nvl(getSelection(databaseTypeComboBox), configuration.getDatabaseType());
    }

    @Override
    public void resetFormChanges() {
        ConnectionDatabaseSettings configuration = getConfiguration();
        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        DatabaseType databaseType = configuration.getDatabaseType();

        nameTextField.setText(configuration.getDisplayName());
        descriptionTextField.setText(configuration.getDescription());
        setSelection(databaseTypeComboBox, databaseType);

        urlSettingsForm.resetFormChanges();
        authSettingsForm.resetFormChanges();
        driverSettingsForm.resetFormChanges();
    }

    private void updateNativeSupportDatabaseHint() {
        DatabaseType selectedDatabaseType = getSelectedDatabaseType();
        DatabaseType driverDatabaseType = driverSettingsForm.getDriverDatabaseType();
        if (selectedDatabaseType == DatabaseType.GENERIC && driverDatabaseType != null && driverDatabaseType != selectedDatabaseType) {
            String databaseTypeName = driverDatabaseType.getName();
            DBNHintForm hintForm = new DBNHintForm(this,
                    "Your database type was identified as \"" + databaseTypeName +"\".\n" +
                    "Use specific connection type instead of \"Generic\", " +
                    "to enable dedicated support for this database", null, true,
                    "Change to " + databaseTypeName,
                    () -> setSelection(databaseTypeComboBox, driverDatabaseType));
            databaseTypeHintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);
        } else {
            databaseTypeHintPanel.removeAll();
        }
    }
}

