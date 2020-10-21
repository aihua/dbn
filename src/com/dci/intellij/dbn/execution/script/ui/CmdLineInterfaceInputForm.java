package com.dci.intellij.dbn.execution.script.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.ui.DBNHintForm;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.DatabaseType;
import com.dci.intellij.dbn.execution.script.CmdLineInterface;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Set;

public class CmdLineInterfaceInputForm extends DBNFormImpl{
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JLabel databaseTypeLabel;
    private JLabel nameInUseLabel;
    private JPanel executablePanel;
    private JLabel executableLabel;
    private JPanel databaseTypePanel;
    private JPanel hintPanel;

    public CmdLineInterfaceInputForm(@NotNull CmdLineInterfaceInputDialog parent, @NotNull CmdLineInterface cmdLineInterface, @NotNull Set<String> usedNames) {
        super(parent);
        DatabaseType databaseType = cmdLineInterface.getDatabaseType();
        databaseTypeLabel.setText(databaseType.name());
        databaseTypeLabel.setIcon(databaseType.getIcon());
        databaseTypePanel.setBorder(UIUtil.getTextFieldBorder());

        String executablePath = cmdLineInterface.getExecutablePath();
        executableLabel.setText(executablePath);
        executablePanel.setBorder(UIUtil.getTextFieldBorder());

        nameTextField.setText(CmdLineInterface.getDefault(databaseType).getName());
        nameInUseLabel.setIcon(Icons.COMMON_ERROR);

        cmdLineInterface.setDatabaseType(databaseType);
        cmdLineInterface.setExecutablePath(executablePath);
        nameTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updateComponents(cmdLineInterface, usedNames);
            }
        });

        String hintText =
                "Please provide a name for storing Command-Line interface executable.\n" +
                "Command-Line interfaces can be configured in DBN Settings > Execution Engine > Script Execution.";
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        updateComponents(cmdLineInterface, usedNames);
    }

    public CmdLineInterfaceInputDialog getParentDialog() {
        return (CmdLineInterfaceInputDialog) ensureParent();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return nameTextField;
    }

    private void updateComponents(CmdLineInterface cmdLineInterface, Set<String> usedNames) {
        String name = nameTextField.getText();
        cmdLineInterface.setName(name);
        boolean isNameUsed = usedNames.contains(name);
        nameInUseLabel.setVisible(isNameUsed);

        CmdLineInterfaceInputDialog parentComponent = getParentDialog();
        parentComponent.setActionEnabled(!isNameUsed && StringUtil.isNotEmpty(nameTextField.getText()));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
