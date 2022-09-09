package com.dci.intellij.dbn.connection.ui;

import com.dci.intellij.dbn.common.database.AuthenticationInfo;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.form.DBNHeaderForm;
import com.dci.intellij.dbn.common.ui.form.DBNHintForm;
import com.dci.intellij.dbn.common.ui.util.ComboBoxes;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.AuthenticationType;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;

import static com.dci.intellij.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dci.intellij.dbn.common.ui.util.ComboBoxes.setSelection;

public class ConnectionAuthenticationForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPasswordField passwordField;
    private JTextField userTextField;
    private JPanel hintPanel;
    private ComboBox<AuthenticationType> authTypeComboBox;
    private JLabel userLabel;
    private JLabel passwordLabel;

    private String cachedUser = "";
    private String cachedPassword = "";

    ConnectionAuthenticationForm(@NotNull ConnectionAuthenticationDialog parentComponent, @Nullable ConnectionHandler connection) {
        super(parentComponent);

        AuthenticationInfo authenticationInfo = parentComponent.getAuthenticationInfo();

        String user = authenticationInfo.getUser();
        if (Strings.isNotEmpty(user)) {
            userTextField.setText(user);
            cachedUser = user;
        }

        String hintText;
        if (connection != null) {
            initComboBox(authTypeComboBox, connection.getDatabaseType().getAuthTypes());
            DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
            headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

            int passwordExpiryTime = connection.getSettings().getDetailSettings().getCredentialExpiryMinutes();
            String expiryTimeText = passwordExpiryTime == 0 ? "0 - no expiry" :
                    passwordExpiryTime == 1 ? "1 minute" : passwordExpiryTime + " minutes";

            hintText = "The system needs your credentials to connect to this database. " +
                            "\nYou can configure how long the credentials stay active on idle connectivity " +
                            "in DBN Settings > Connection > Details (currently set to " + expiryTimeText + ")";

        } else {
            initComboBox(authTypeComboBox, AuthenticationType.values());
            hintText = "The system needs your credentials to connect to this database.";
        }
        setSelection(authTypeComboBox, authenticationInfo.getType());

        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        updateAuthenticationFields();

        userTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                String user = userTextField.getText();
                authenticationInfo.setUser(user);
                parentComponent.updateConnectButton();
            }
        });

        passwordField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                String password = new String(passwordField.getPassword());
                authenticationInfo.setPassword(password);
                parentComponent.updateConnectButton();
            }
        });

        authTypeComboBox.addActionListener(e -> {
            authenticationInfo.setType(ComboBoxes.getSelection(authTypeComboBox));
            updateAuthenticationFields();
            parentComponent.updateConnectButton();
        });
    }

    private void updateAuthenticationFields() {
        AuthenticationType authType = ComboBoxes.getSelection(authTypeComboBox);

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
        if (Strings.isNotEmpty(user)) cachedUser = user;
        if (Strings.isNotEmpty(password)) cachedPassword = password;


        userTextField.setText(showUser ? cachedUser : "");
        passwordField.setText(showPassword ? cachedPassword : "");
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return Strings.isEmpty(userTextField.getText()) ? userTextField : passwordField;
    }
}
