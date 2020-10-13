package com.dci.intellij.dbn.connection.info.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.database.DatabaseInfo;
import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.ui.Borders;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.ui.DBNHeaderForm;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.DatabaseType;
import com.dci.intellij.dbn.connection.DatabaseUrlType;
import com.dci.intellij.dbn.connection.config.ConnectionDatabaseSettings;
import com.dci.intellij.dbn.connection.info.ConnectionInfo;
import com.dci.intellij.dbn.driver.DriverSource;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

import static com.dci.intellij.dbn.common.ui.GUIUtil.updateBorderTitleForeground;

public class ConnectionInfoForm extends DBNFormImpl{
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel setupPanel;
    private JPanel metaDataPanel;
    private JPanel detailsPanel;
    private JLabel infoDatabaseTypeLabel;
    private JLabel statusMessageLabel;
    private JLabel setupDescriptionLabel;
    private JTextField setupFileTextField;
    private JTextField setupDatabaseTextField;
    private JTextField setupUrlTextField;
    private JTextField setupHostTextField;
    private JTextField setupPortTextField;
    private JTextField setupDriverTextField;
    private JTextField setupNameTextField;
    private JTextField setupDescriptionTextField;
    private JTextField setupDriverLibraryTextField;
    private JTextField infoProductNameTextField;
    private JTextField infoProductVersionTextField;
    private JTextField infoDriverNameTextField;
    private JTextField infoDriverVersionTextField;
    private JTextField infoJdbcTypeTextField;
    private JTextField infoConnectionUrlTextField;
    private JTextField infoUserNameTextField;
    private JLabel setupNameLabel;
    private JLabel setupDriverLibraryLabel;
    private JLabel setupDriverLabel;
    private JLabel setupHostLabel;
    private JLabel setupPortLabel;
    private JLabel setupDatabaseLabel;
    private JLabel setupUrlLabel;
    private JLabel setupFileLabel;
    private JLabel infoDatabaseTypeValueLabel;
    private JLabel infoProductNameLabel;
    private JLabel infoProductVersionLabel;
    private JLabel infoDriverNameLabel;
    private JLabel infoDriverVersionLabel;
    private JLabel infoJdbcTypeLabel;
    private JLabel infoConnectionUrlLabel;
    private JLabel infoUserNameLabel;

    public ConnectionInfoForm(ConnectionInfoDialog parent, ConnectionHandler connectionHandler) {
        super(parent);
        initHeaderPanel(connectionHandler);
        initSetupPanel(connectionHandler);
        initInfoPanel(connectionHandler);
    }

    public ConnectionInfoForm(@NotNull ConnectionInfoDialog parent, ConnectionInfo connectionInfo, String connectionName, EnvironmentType environmentType) {
        super(parent);
        setupPanel.setVisible(false);
        initHeaderPanel(connectionName, environmentType);
        initInfoPanel(connectionInfo);
    }

    private void initHeaderPanel(ConnectionHandler connectionHandler) {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, connectionHandler);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    private void initHeaderPanel(String connectionName, EnvironmentType environmentType) {
        DBNHeaderForm headerForm = new DBNHeaderForm(this);
        headerForm.setTitle(connectionName);
        headerForm.setIcon(Icons.CONNECTION_CONNECTED);
        headerForm.setBackground(environmentType.getColor());
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    private void initInfoPanel(ConnectionHandler connectionHandler) {
        try {
            Connection connection = connectionHandler.getMainConnection();
            ConnectionInfo connectionInfo = new ConnectionInfo(connection.getMetaData());

            initInfoPanel(connectionInfo);
        } catch (SQLException e) {
            infoDatabaseTypeValueLabel.setText(DatabaseType.GENERIC.getName());
            infoDatabaseTypeValueLabel.setIcon(DatabaseType.GENERIC.getIcon());


            initValueField(infoProductNameLabel, infoProductNameTextField, "-");
            initValueField(infoProductVersionLabel, infoProductVersionTextField, "-");
            initValueField(infoDriverNameLabel, infoDriverNameTextField, "-");
            initValueField(infoDriverVersionLabel, infoDriverVersionTextField, "-");
            initValueField(infoJdbcTypeLabel, infoJdbcTypeTextField, "-");
            initValueField(infoConnectionUrlLabel, infoConnectionUrlTextField, "-");
            initValueField(infoUserNameLabel, infoUserNameTextField, "-");
            statusMessageLabel.setText(e.getMessage());
            statusMessageLabel.setIcon(Icons.EXEC_MESSAGES_ERROR);
        }

        updateBorderTitleForeground(detailsPanel);
    }

    private void initInfoPanel(ConnectionInfo connectionInfo) {
        DatabaseType databaseType = connectionInfo.getDatabaseType();
        infoDatabaseTypeValueLabel.setText(databaseType.getName());
        infoDatabaseTypeValueLabel.setIcon(databaseType.getIcon());

        initValueField(infoProductNameLabel, infoProductNameTextField, connectionInfo.getProductName());
        initValueField(infoProductVersionLabel, infoProductVersionTextField, connectionInfo.getProductVersion());
        initValueField(infoDriverNameLabel, infoDriverNameTextField, connectionInfo.getDriverName());
        initValueField(infoDriverVersionLabel, infoDriverVersionTextField, connectionInfo.getDriverVersion());
        initValueField(infoJdbcTypeLabel, infoJdbcTypeTextField, connectionInfo.getDriverJdbcType());
        initValueField(infoConnectionUrlLabel, infoConnectionUrlTextField, connectionInfo.getUrl());
        initValueField(infoUserNameLabel, infoUserNameTextField, connectionInfo.getUserName());

        statusMessageLabel.setText("Connection successful");
        statusMessageLabel.setIcon(Icons.EXEC_MESSAGES_INFO);
    }

    private void initSetupPanel(ConnectionHandler connectionHandler) {
        initValueField(setupNameLabel, setupNameTextField, connectionHandler.getName());

        String description = connectionHandler.getDescription();
        initValueField(setupDescriptionLabel, setupDescriptionTextField, description, !StringUtil.isEmpty(description));

        ConnectionDatabaseSettings databaseSettings = connectionHandler.getSettings().getDatabaseSettings();
        String driverLibrary = databaseSettings.getDriverLibrary();
        initValueField(setupDriverLibraryLabel, setupDriverLibraryTextField, databaseSettings.getDriverSource() == DriverSource.BUILTIN ? "Built-in library" : driverLibrary);
        initValueField(setupDriverLabel, setupDriverTextField, databaseSettings.getDriver(), true);

        DatabaseInfo databaseInfo = databaseSettings.getDatabaseInfo();
        boolean isFileUrlType = databaseInfo.getUrlType() == DatabaseUrlType.FILE;
        initValueField(setupHostLabel, setupHostTextField, databaseInfo.getHost(), !isFileUrlType);
        initValueField(setupPortLabel, setupPortTextField, databaseInfo.getPort(), !isFileUrlType);
        initValueField(setupDatabaseLabel, setupDatabaseTextField, databaseInfo.getDatabase(), !isFileUrlType);
        initValueField(setupUrlLabel, setupUrlTextField, databaseSettings.getConnectionUrl(), true);
        initValueField(setupFileLabel, setupFileTextField, databaseInfo.getMainFile(), isFileUrlType);
        updateBorderTitleForeground(setupPanel);
    }

    private void initValueField(JLabel label, JTextField textField, String value) {
        initValueField(label, textField, value, true);
    }
    private void initValueField(JLabel label, JTextField textField, String value, boolean visible) {
        label.setVisible(visible);
        textField.setVisible(visible);
        textField.setBorder(Borders.EMPTY_BORDER);
        textField.setBackground(UIUtil.getPanelBackground());
        textField.setEditable(false);
        textField.setText(getPresentableText(value));
        textField.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        String text = textField.getText();
        if (text.length() > 0) {
            FontMetrics fontMetrics = textField.getFontMetrics(textField.getFont());
            int width = fontMetrics.charsWidth(text.toCharArray(), 0, text.length()) + 40;
            textField.setMinimumSize(new Dimension(Math.min(width, 600), -1));
        }
    }

    @NotNull
    private String getPresentableText(String value) {
        return StringUtil.isEmpty(value) ? "-" : value;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
