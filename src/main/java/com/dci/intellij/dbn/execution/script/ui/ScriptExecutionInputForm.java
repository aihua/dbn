package com.dci.intellij.dbn.execution.script.ui;

import com.dci.intellij.dbn.common.routine.Consumer;
import com.dci.intellij.dbn.common.text.TextContent;
import com.dci.intellij.dbn.common.ui.PresentableFactory;
import com.dci.intellij.dbn.common.ui.ValueSelectorOption;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.form.DBNHeaderForm;
import com.dci.intellij.dbn.common.ui.form.DBNHintForm;
import com.dci.intellij.dbn.common.ui.misc.DBNComboBox;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.dci.intellij.dbn.connection.DatabaseType;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.dci.intellij.dbn.execution.common.ui.ExecutionTimeoutForm;
import com.dci.intellij.dbn.execution.script.CmdLineInterface;
import com.dci.intellij.dbn.execution.script.ScriptExecutionInput;
import com.dci.intellij.dbn.execution.script.ScriptExecutionManager;
import com.dci.intellij.dbn.vfs.DBVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.dci.intellij.dbn.common.text.TextContent.plain;

public class ScriptExecutionInputForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;
    private DBNComboBox<CmdLineInterface> cmdLineExecutableComboBox;
    private JCheckBox clearOutputCheckBox;
    private JPanel hintPanel;
    private JPanel executionTimeoutPanel;

    private final DBNHeaderForm headerForm;
    private final ExecutionTimeoutForm executionTimeoutForm;

    ScriptExecutionInputForm(@NotNull ScriptExecutionInputDialog parent, @NotNull ScriptExecutionInput executionInput) {
        super(parent);

        VirtualFile sourceFile = executionInput.getSourceFile();
        String headerTitle = sourceFile.isInLocalFileSystem() ? sourceFile.getPath() : sourceFile.getName();
        Icon headerIcon = sourceFile.getFileType().getIcon();
        if (sourceFile instanceof DBVirtualFile) {
            DBVirtualFile databaseVirtualFile = (DBVirtualFile) sourceFile;
            headerIcon = databaseVirtualFile.getIcon();
        }

        headerForm = new DBNHeaderForm(this, headerTitle, headerIcon);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        TextContent hintText = plain(
                "Script execution uses the Command-Line Interface executable supplied with your database client. " +
                "Make sure it is available in the \"PATH\" environment variable or provide the path to the executable.");

        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        Project project = ensureProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        connectionComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
        connectionComboBox.setEnabled(sourceFile.isInLocalFileSystem());
        connectionComboBox.setValues(connectionManager.getConnections());

        schemaComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);

        cmdLineExecutableComboBox.set(ValueSelectorOption.HIDE_ICON, true);
        cmdLineExecutableComboBox.setValueFactory(new PresentableFactory<CmdLineInterface>("New Cmd-Line Interface...") {
            @Override
            public void create(Consumer<CmdLineInterface> consumer) {
                ConnectionHandler connection = connectionComboBox.getSelectedValue();
                if (connection != null) {
                    ScriptExecutionManager scriptExecutionManager = ScriptExecutionManager.getInstance(project);
                    scriptExecutionManager.createCmdLineInterface(connection.getDatabaseType(), null, consumer);
                }
            }
        });

        clearOutputCheckBox.setSelected(executionInput.isClearOutput());
        executionTimeoutForm = new ExecutionTimeoutForm(this, executionInput, DBDebuggerType.NONE) {
            @Override
            protected void handleChange(boolean hasError) {
                updateButtons();
            }
        };
        executionTimeoutPanel.add(executionTimeoutForm.getComponent());

        updateControls(executionInput);
        clearOutputCheckBox.addActionListener(e -> {
            boolean selected = clearOutputCheckBox.isSelected();
            executionInput.setClearOutput(selected);
        });

        connectionComboBox.addListener((oldValue, newValue) -> {
            executionInput.setTargetConnection(newValue);
            updateControls(executionInput);
        });

        schemaComboBox.addListener((oldValue, newValue) -> {
            executionInput.setTargetSchemaId(newValue);
            updateButtons();
        });

        cmdLineExecutableComboBox.addListener((oldValue, newValue) -> {
            executionInput.setCmdLineInterface(newValue);
            updateButtons();
        });
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return connectionComboBox.isEnabled() ? connectionComboBox : schemaComboBox;
    }

    private void updateControls(ScriptExecutionInput executionInput) {
        ConnectionHandler connection = executionInput.getConnection();
        SchemaId schema = executionInput.getSchemaId();
        CmdLineInterface cmdLineInterface;
        if (connection != null && !connection.isVirtual()) {
            schema = Commons.nvln(schema, connection.getDefaultSchema());
            connectionComboBox.setSelectedValue(connection);
            schemaComboBox.setValues(connection.getSchemaIds());
            schemaComboBox.setSelectedValue(schema);
            schemaComboBox.setEnabled(true);
            headerForm.setBackground(connection.getEnvironmentType().getColor());

            DatabaseType databaseType = connection.getDatabaseType();
            ScriptExecutionManager scriptExecutionManager = ScriptExecutionManager.getInstance(ensureProject());
            List<CmdLineInterface> interfaces = scriptExecutionManager.getAvailableInterfaces(databaseType);
            cmdLineExecutableComboBox.clearValues();
            cmdLineExecutableComboBox.addValues(interfaces);
            cmdLineExecutableComboBox.setEnabled(true);

            cmdLineInterface = scriptExecutionManager.getRecentInterface(databaseType);
            if (cmdLineInterface != null) {
                cmdLineExecutableComboBox.setSelectedValue(cmdLineInterface);

            }

            executionInput.setTargetConnection(connection);
            executionInput.setTargetSchemaId(schema);
            executionInput.setCmdLineInterface(cmdLineInterface);
        } else {
            schemaComboBox.setEnabled(false);
            cmdLineExecutableComboBox.setEnabled(false);
        }
        updateButtons();
    }

    private void updateButtons() {
        ScriptExecutionInputDialog parentComponent = ensureParentComponent();
        parentComponent.setActionEnabled(
                connectionComboBox.getSelectedValue() != null &&
                schemaComboBox.getSelectedValue() != null &&
                cmdLineExecutableComboBox.getSelectedValue() != null &&
                !executionTimeoutForm.hasErrors());
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
