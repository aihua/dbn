package com.dci.intellij.dbn.connection.config.ui;

import com.dci.intellij.dbn.common.database.DatabaseInfo;
import com.dci.intellij.dbn.common.ui.Presentable;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.misc.DBNComboBox;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.common.util.Safe;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.DatabaseType;
import com.dci.intellij.dbn.connection.DatabaseUrlPattern;
import com.dci.intellij.dbn.connection.DatabaseUrlType;
import com.dci.intellij.dbn.connection.config.ConnectionDatabaseSettings;
import com.dci.intellij.dbn.connection.config.file.DatabaseFileBundle;
import com.dci.intellij.dbn.connection.config.file.ui.DatabaseFileSettingsForm;
import com.dci.intellij.dbn.connection.config.tns.TnsNames;
import com.dci.intellij.dbn.connection.config.tns.TnsNamesParser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;

import static com.dci.intellij.dbn.common.ui.util.ComboBoxes.*;
import static com.dci.intellij.dbn.common.ui.util.TextFields.onTextChange;
import static com.dci.intellij.dbn.common.util.Commons.coalesce;
import static com.dci.intellij.dbn.common.util.Commons.nvl;
import static com.dci.intellij.dbn.connection.DatabaseUrlType.CUSTOM;
import static com.dci.intellij.dbn.connection.DatabaseUrlType.FILE;

public class ConnectionUrlSettingsForm extends DBNFormBase {
    private JLabel urlTypeLabel;
    private JLabel hostLabelField;
    private JLabel portLabelField;
    private JLabel databaseLabel;
    private JLabel tnsFolderLabel;
    private JLabel tnsProfileLabel;
    private JLabel databaseFilesLabel;
    private JLabel urlLabel;
    private JPanel databaseFilesPanel;
    private ComboBox<DatabaseUrlType> urlTypeComboBox;
    private DBNComboBox<Presentable> tnsProfileComboBox;
    private JTextField hostTextField;
    private JTextField portTextField;
    private JTextField databaseTextField;
    private JTextField urlTextField;
    private TextFieldWithBrowseButton tnsFolderTextField;
    private JPanel mainPanel;

    private final DatabaseFileSettingsForm databaseFileSettingsForm;
    private final Map<DatabaseType, DatabaseInfo> history = new HashMap<>();


    public ConnectionUrlSettingsForm(ConnectionDatabaseSettingsForm parent, ConnectionDatabaseSettings configuration) {
        super(parent);

        databaseFileSettingsForm = new DatabaseFileSettingsForm(this, configuration.getDatabaseInfo().getFileBundle());
        databaseFilesPanel.add(databaseFileSettingsForm.getComponent(), BorderLayout.CENTER);
        urlTypeComboBox.addActionListener(e -> updateFieldVisibility());

        tnsFolderTextField.addBrowseFolderListener(
                "Select Wallet Directory",
                "Folder must contain tnsnames.ora",
                null, new FileChooserDescriptor(false, true, true, true, false, false));
        onTextChange(tnsFolderTextField, e -> handleTnsFolderChanged(tnsFolderTextField.getText()));

        onTextChange(hostTextField, e -> updateUrlField());
        onTextChange(portTextField, e -> updateUrlField());
        onTextChange(databaseTextField, e -> updateUrlField());
        onTextChange(tnsFolderTextField, e -> updateUrlField());
        tnsProfileComboBox.addActionListener(e -> updateUrlField());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private DatabaseType getDatabaseType() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        return parent.getSelectedDatabaseType();
    }

    public String getVendor() {
        return Objects.toString(getDatabaseType()).toLowerCase();
    }

    public String getHost() {
        return hostTextField.getText();
    }

    public String getPort() {
        return portTextField.getText();
    }

    public String getDatabase() {
        return databaseTextField.getText();
    }

    public String getTnsFolder() {
        return tnsFolderTextField.getText();
    }

    public String getTnsProfile() {
        return Safe.call(tnsProfileComboBox.getSelectedValue(), v -> v.getName(), null);
    }

    public String getUrl() {
        return urlTextField.getText();
    }

    public DatabaseFileBundle getFileBundle() {
        return databaseFileSettingsForm.getFileBundle();
    }

    public DatabaseUrlType getUrlType() {
        return getSelection(urlTypeComboBox);
    }

    private void updateUrlField() {
        DatabaseUrlType urlType = getUrlType();
        if (urlType == CUSTOM) return;

        DatabaseType databaseType = getDatabaseType();
        DatabaseUrlPattern urlPattern = nvl(databaseType.getUrlPattern(urlType), DatabaseUrlPattern.GENERIC);
        String url = urlPattern.buildUrl(
                getVendor(),
                getHost(),
                getPort(),
                getDatabase(),
                getMainFilePath() ,
                getTnsFolder(),
                getTnsProfile());
        urlTextField.setText(url);
    }

    private String getMainFilePath() {
        return databaseFileSettingsForm.getFileBundle().getMainFilePath();
    }

    private void handleTnsFolderChanged(@NotNull String text) {
        tnsProfileComboBox.setValues(Collections.emptyList());
        File tnsFolder = new File(text);
        if (!tnsFolder.isDirectory()) return;

        File tnsFile = new File(tnsFolder, "tnsnames.ora");
        if (!tnsFile.exists()) return;

        List<String> tnsEntries = getTnsEntries(tnsFile);
        tnsProfileComboBox.setValues(Presentable.basic(tnsEntries));
    }

    private List<String> getTnsEntries(File tnsnamesOraFile) {
        try {
            TnsNames tnsNames = TnsNamesParser.get(tnsnamesOraFile);
            return tnsNames.getProfileNames();
        } catch (Exception e) {
            //ErrorHandler.logErrorStack("Error occurred while reading tnsnames.ora file for database: " + adbInstance.getDbName(), e);
        }
        return Collections.emptyList();
    }

    public void updateFieldVisibility() {
        DatabaseUrlType urlType = getUrlType();

        boolean tnsVisible = urlType == DatabaseUrlType.TNS;
        boolean flsVisible = urlType == FILE;
        boolean hpdVisible = urlType.isOneOf(
                DatabaseUrlType.SID,
                DatabaseUrlType.SERVICE,
                DatabaseUrlType.DATABASE);

        urlTextField.setEnabled(urlType == CUSTOM);

        // tns folder
        tnsFolderTextField.setVisible(tnsVisible);
        tnsFolderLabel.setVisible(tnsVisible);
        tnsProfileComboBox.setVisible(tnsVisible);
        tnsProfileLabel.setVisible(tnsVisible);

        // classic service name or sid
        databaseLabel.setVisible(hpdVisible);
        databaseTextField.setVisible(hpdVisible);
        hostLabelField.setVisible(hpdVisible);
        hostTextField.setVisible(hpdVisible);
        portLabelField.setVisible(hpdVisible);
        portTextField.setVisible(hpdVisible);

        // file based url
        databaseFilesLabel.setVisible(flsVisible);
        databaseFilesPanel.setVisible(flsVisible);

        updateUrlField();
    }

    void handleDatabaseTypeChange(DatabaseType oldDatabaseType, DatabaseType newDatabaseType) {
        DatabaseInfo previousInfo = loadDatabaseInfo();
        history.put(oldDatabaseType, previousInfo);

        DatabaseInfo histInfo = history.get(newDatabaseType);
        if (histInfo == null) {
            String previousUrl = previousInfo.getUrl();
            DatabaseUrlType previousUrlType = previousInfo.getUrlType();

            DatabaseUrlPattern urlPattern = coalesce(
                    () -> newDatabaseType.resolveUrlPattern(previousUrl),
                    () -> newDatabaseType.getUrlPattern(previousUrlType),
                    () -> newDatabaseType.getDefaultUrlPattern());

            histInfo = urlPattern.getDefaultInfo();
            if (Strings.isNotEmptyOrSpaces(previousUrl)) {
                histInfo.setUrl(previousUrl);
                histInfo.initializeDetails(urlPattern);
            }

        }

        applyDatabaseInfo(histInfo);
        updateFieldVisibility();
    }

    void resetFormChanges() {
        ConnectionDatabaseSettings configuration = getDatabaseSettings();
        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        applyDatabaseInfo(databaseInfo);

    }

    private DatabaseInfo loadDatabaseInfo() {
        DatabaseInfo databaseInfo = new DatabaseInfo();
        databaseInfo.setHost(getHost());
        databaseInfo.setPort(getPort());
        databaseInfo.setDatabase(getDatabase());
        databaseInfo.setFileBundle(getFileBundle().clone());
        databaseInfo.setTnsFolder(getTnsFolder());
        databaseInfo.setTnsProfile(getTnsProfile());
        databaseInfo.setUrlType(getUrlType());
        databaseInfo.setUrl(getUrl());
        return databaseInfo;
    }

    private void applyDatabaseInfo(DatabaseInfo databaseInfo) {
        databaseFileSettingsForm.setFileBundle(databaseInfo.getFileBundle());
        hostTextField.setText(databaseInfo.getHost());
        portTextField.setText(databaseInfo.getPort());
        databaseTextField.setText(databaseInfo.getDatabase());
        tnsFolderTextField.setText(databaseInfo.getTnsFolder());

        String tnsProfile = databaseInfo.getTnsProfile();
        if (Strings.isNotEmpty(tnsProfile)) {
            Presentable presentable = Presentable.basic(tnsProfile);
            tnsProfileComboBox.setSelectedValue(presentable);
        }


        DatabaseType databaseType = getDatabaseType();
        DatabaseUrlType[] urlTypes = databaseType.getUrlTypes();
        initComboBox(urlTypeComboBox, urlTypes);
        setSelection(urlTypeComboBox, databaseInfo.getUrlType());
        urlTypeLabel.setVisible(urlTypes.length > 1);
        urlTypeComboBox.setVisible(urlTypes.length > 1);
        urlTextField.setText(databaseInfo.getUrl());
    }

    @NotNull
    private ConnectionDatabaseSettings getDatabaseSettings() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        ConnectionDatabaseSettings configuration = parent.getConfiguration();
        return configuration;
    }

    boolean settingsChanged() {
        ConnectionDatabaseSettings configuration = getDatabaseSettings();

        DatabaseInfo databaseInfo = configuration.getDatabaseInfo();
        DatabaseUrlType urlType = getUrlType();
        return
            !Commons.match(databaseInfo.getHost(), getHost()) ||
            !Commons.match(databaseInfo.getPort(), getPort()) ||
            !Commons.match(databaseInfo.getDatabase(), getDatabase()) ||
            !Commons.match(databaseInfo.getTnsFolder(), getTnsFolder()) ||
            !Commons.match(databaseInfo.getTnsProfile(), getTnsProfile()) ||
            !Commons.match(databaseInfo.getUrl(), getUrl()) ||
            !Commons.match(databaseInfo.getUrlType(), urlType) ||
            !Commons.match(databaseInfo.getFileBundle(), urlType == FILE ? getFileBundle() : null);

    }
}
