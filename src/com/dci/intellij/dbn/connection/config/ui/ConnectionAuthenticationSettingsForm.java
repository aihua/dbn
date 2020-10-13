package com.dci.intellij.dbn.connection.config.ui;

import com.dci.intellij.dbn.common.database.AuthenticationInfo;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.AuthenticationType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionListener;

import static com.dci.intellij.dbn.common.ui.ComboBoxUtil.getSelection;
import static com.dci.intellij.dbn.common.ui.ComboBoxUtil.initComboBox;
import static com.dci.intellij.dbn.common.ui.ComboBoxUtil.setSelection;

public class ConnectionAuthenticationSettingsForm extends DBNFormImpl {
    private JComboBox<AuthenticationType> authTypeComboBox;
    private JTextField userTextField;
    private JPasswordField passwordField;
    private JPanel mainPanel;
    private JLabel userLabel;
    private JLabel passwordLabel;

    private String cachedUser = "";
    private String cachedPassword = "";

    private final ActionListener actionListener = e -> updateAuthenticationFields();

    ConnectionAuthenticationSettingsForm(@NotNull ConnectionDatabaseSettingsForm parentComponent) {
        super(parentComponent);
        initComboBox(authTypeComboBox, AuthenticationType.values());
        authTypeComboBox.addActionListener(actionListener);
    }

    private void updateAuthenticationFields() {
        AuthenticationType authType = getSelection(authTypeComboBox);

        boolean showUser = authType.isOneOf(
                AuthenticationType.USER,
                AuthenticationType.USER_PASSWORD);
        boolean showPassword = authType == AuthenticationType.USER_PASSWORD;


        userLabel.setVisible(showUser);
        userTextField.setVisible(showUser);

        passwordLabel.setVisible(showPassword);
        passwordField.setVisible(showPassword);
        //passwordField.setBackground(showPasswordField ? UIUtil.getTextFieldBackground() : UIUtil.getPanelBackground());

        String user = userTextField.getText();
        String password = new String(passwordField.getPassword());
        if (StringUtil.isNotEmpty(user)) cachedUser = user;
        if (StringUtil.isNotEmpty(password)) cachedPassword = password;


        userTextField.setText(showUser ? cachedUser : "");
        passwordField.setText(showPassword ? cachedPassword : "");
    }

    public JTextField getUserTextField() {
        return userTextField;
    }

    public void applyFormChanges(AuthenticationInfo authenticationInfo){
        authenticationInfo.setType(getSelection(authTypeComboBox));
        authenticationInfo.setUser(userTextField.getText());
        authenticationInfo.setPassword(new String(passwordField.getPassword()));
    }

    public void resetFormChanges(AuthenticationInfo authenticationInfo) {
        String user = authenticationInfo.getUser();
        String password = authenticationInfo.getPassword();
        if (StringUtil.isNotEmpty(user)) cachedUser = user;
        if (StringUtil.isNotEmpty(password)) cachedPassword = password;

        userTextField.setText(authenticationInfo.getUser());
        passwordField.setText(authenticationInfo.getPassword());
        setSelection(authTypeComboBox, authenticationInfo.getType());
        updateAuthenticationFields();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
