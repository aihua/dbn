package com.dci.intellij.dbn.data.export.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.form.DBNHeaderForm;
import com.dci.intellij.dbn.common.ui.util.TextFields;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.dci.intellij.dbn.connection.config.ui.CharsetOption;
import com.dci.intellij.dbn.data.export.DataExportFormat;
import com.dci.intellij.dbn.data.export.DataExportInstructions;
import com.dci.intellij.dbn.data.export.DataExportManager;
import com.dci.intellij.dbn.data.export.ExportQuotePair;
import com.dci.intellij.dbn.data.export.processor.DataExportProcessor;
import com.dci.intellij.dbn.object.DBTable;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.Charset;

import static com.dci.intellij.dbn.common.message.MessageCallback.when;
import static com.dci.intellij.dbn.common.ui.util.ComboBoxes.*;
import static com.dci.intellij.dbn.common.ui.util.TextFields.addDocumentListener;
import static com.dci.intellij.dbn.common.ui.util.TextFields.isEmptyText;
import static com.dci.intellij.dbn.data.export.processor.DataExportFeature.*;

public class ExportDataForm extends DBNFormBase {
    private static final FileChooserDescriptor DIRECTORY_FILE_DESCRIPTOR = new FileChooserDescriptor(false, true, false, false, false, false);

    private JPanel mainPanel;
    private JRadioButton scopeGlobalRadioButton;
    private JRadioButton scopeSelectionRadioButton;
    private JRadioButton formatSQLRadioButton;
    private JRadioButton formatHTMLRadioButton;
    private JRadioButton formatXMLRadioButton;
    private JRadioButton formatJiraRadioButton;
    private JRadioButton formatExcelRadioButton;
    private JRadioButton formatExcelXRadioButton;
    private JRadioButton formatCSVRadioButton;
    private JRadioButton formatCustomRadioButton;
    private JRadioButton destinationClipboardRadioButton;
    private JRadioButton destinationFileRadioButton;

    private JTextField fileNameTextField;
    private JTextField beginQuoteTextField;
    private JTextField endQuoteTextField;
    private JTextField valueSeparatorTextField;
    private TextFieldWithBrowseButton fileLocationTextField;
    private JCheckBox createHeaderCheckBox;
    private JCheckBox friendlyHeadersCheckBox;
    private JCheckBox quoteValuesCheckBox;
    private JCheckBox quoteAllValuesCheckBox;

    private JComboBox<CharsetOption> encodingComboBox;
    private JPanel headerPanel;
    private JPanel scopePanel;
    private JPanel formatPanel;
    private JPanel destinationPanel;
    private JPanel optionsPanel;

    private JLabel encodingLabel;

    private final DataExportInstructions instructions;
    private final ConnectionRef connection;
    private final DBObjectRef<?> sourceObject;

    ExportDataForm(ExportDataDialog parentComponent, DataExportInstructions instructions, boolean hasSelection, @NotNull ConnectionHandler connection, @Nullable DBObject sourceObject) {
        super(parentComponent);
        this.connection = connection.ref();
        this.sourceObject = DBObjectRef.of(sourceObject);
        this.instructions = instructions;

        initComboBox(encodingComboBox, CharsetOption.ALL);
        setSelection(encodingComboBox, CharsetOption.get(instructions.getCharset()));

        ActionListener actionListener = e -> enableDisableFields();
        scopeGlobalRadioButton.addActionListener(actionListener);
        scopeSelectionRadioButton.addActionListener(actionListener);
        formatSQLRadioButton.addActionListener(actionListener);
        formatHTMLRadioButton.addActionListener(actionListener);
        formatXMLRadioButton.addActionListener(actionListener);
        formatJiraRadioButton.addActionListener(actionListener);
        formatExcelRadioButton.addActionListener(actionListener);
        formatExcelXRadioButton.addActionListener(actionListener);
        formatCSVRadioButton.addActionListener(actionListener);
        formatCustomRadioButton.addActionListener(actionListener);
        destinationClipboardRadioButton.addActionListener(actionListener);
        destinationFileRadioButton.addActionListener(actionListener);
        createHeaderCheckBox.addActionListener(actionListener);
        friendlyHeadersCheckBox.addActionListener(actionListener);
        quoteValuesCheckBox.addActionListener(actionListener);
        quoteAllValuesCheckBox.addActionListener(actionListener);
        addDocumentListener(beginQuoteTextField, createQuoteChangeListener());

        scopeSelectionRadioButton.setEnabled(hasSelection);
        scopeSelectionRadioButton.setSelected(hasSelection);
        scopeGlobalRadioButton.setSelected(!hasSelection);

        formatSQLRadioButton.setEnabled(sourceObject instanceof DBTable);

        DataExportFormat format = instructions.getFormat();
        if (formatSQLRadioButton.isEnabled()) {
            formatSQLRadioButton.setSelected(format == DataExportFormat.SQL);
        }

        formatExcelRadioButton.setSelected(format == DataExportFormat.EXCEL);
        formatExcelXRadioButton.setSelected(format == DataExportFormat.EXCELX);
        formatHTMLRadioButton.setSelected(format == DataExportFormat.HTML);
        formatXMLRadioButton.setSelected(format == DataExportFormat.XML);
        formatJiraRadioButton.setSelected(format == DataExportFormat.JIRA);
        formatCSVRadioButton.setSelected(format == DataExportFormat.CSV);
        formatCustomRadioButton.setSelected(format == DataExportFormat.CUSTOM);

        TextFields.limitTextLength(beginQuoteTextField, 1);
        TextFields.limitTextLength(endQuoteTextField, 1);
        beginQuoteTextField.setText(instructions.getBeginQuote());
        endQuoteTextField.setText(instructions.getEndQuote());
        valueSeparatorTextField.setText(instructions.getValueSeparator());
        quoteValuesCheckBox.setSelected(instructions.isQuoteValuesContainingSeparator());
        quoteAllValuesCheckBox.setSelected(instructions.isQuoteAllValues());
        createHeaderCheckBox.setSelected(instructions.isCreateHeader());
        friendlyHeadersCheckBox.setSelected(instructions.isFriendlyHeaders());

        DataExportInstructions.Destination destination = instructions.getDestination();
        if (destinationClipboardRadioButton.isEnabled()) {
            destinationClipboardRadioButton.setSelected(destination == DataExportInstructions.Destination.CLIPBOARD);
            destinationFileRadioButton.setSelected(destination == DataExportInstructions.Destination.FILE);
        } else {
            destinationFileRadioButton.setSelected(true);
        }

        //fileNameTextField.setText(instructions.getFileName());
        fileLocationTextField.setText(instructions.getFileLocation());

        Project project = connection.getProject();
        fileLocationTextField.addBrowseFolderListener(
                "Select Directory",
                "Select destination directory for the exported file", project, DIRECTORY_FILE_DESCRIPTOR);
        
        enableDisableFields();

        String headerTitle;
        Icon headerIcon;
        Color headerBackground = Colors.getPanelBackground();
        if (getEnvironmentSettings(project).getVisibilitySettings().getDialogHeaders().value()) {
            headerBackground = connection.getEnvironmentType().getColor();
        }
        if (sourceObject != null) {
            headerTitle = sourceObject instanceof DBSchemaObject ? sourceObject.getQualifiedName() : sourceObject.getName();
            headerIcon = sourceObject.getIcon();
        } else {
            headerIcon = Icons.DBO_TABLE;
            headerTitle = instructions.getBaseName();
        }
        DBNHeaderForm headerComponent = new DBNHeaderForm(this, headerTitle, headerIcon, headerBackground);
        headerPanel.add(headerComponent.getComponent());
    }

    @NotNull
    private DocumentAdapter createQuoteChangeListener() {
        return new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent documentEvent) {
                String beginQuote = beginQuoteTextField.getText();
                String endQuote = ExportQuotePair.endQuoteOf(beginQuote);
                endQuoteTextField.setText(endQuote);
            }
        };
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    DataExportInstructions getExportInstructions() {
        instructions.setScope(scopeSelectionRadioButton.isSelected() ?
                DataExportInstructions.Scope.SELECTION  :
                DataExportInstructions.Scope.GLOBAL);
        instructions.setCreateHeader(createHeaderCheckBox.isSelected());
        instructions.setFriendlyHeaders(friendlyHeadersCheckBox.isSelected());
        instructions.setQuoteValuesContainingSeparator(quoteValuesCheckBox.isSelected());
        instructions.setQuoteAllValues(quoteAllValuesCheckBox.isSelected());
        instructions.setBeginQuote(beginQuoteTextField.getText().trim());
        instructions.setEndQuote(endQuoteTextField.getText().trim());
        instructions.setValueSeparator(valueSeparatorTextField.getText().trim());
        if (destinationFileRadioButton.isSelected()) {
            instructions.setFileName(fileNameTextField.getText());
            instructions.setFileLocation(fileLocationTextField.getText());
        }

        instructions.setDestination(destinationClipboardRadioButton.isSelected() ?
                DataExportInstructions.Destination.CLIPBOARD :
                DataExportInstructions.Destination.FILE);

        instructions.setFormat(getFormat());

        CharsetOption charsetOption = getSelection(encodingComboBox);
        Charset charset = charsetOption == null ? Charset.defaultCharset() : charsetOption.getCharset();
        instructions.setCharset(charset);
        return instructions;
    }

    private DataExportFormat getFormat() {
        return
            formatSQLRadioButton.isSelected() ? DataExportFormat.SQL :
            formatHTMLRadioButton.isSelected() ? DataExportFormat.HTML :
            formatXMLRadioButton.isSelected() ? DataExportFormat.XML :
            formatJiraRadioButton.isSelected() ? DataExportFormat.JIRA :
            formatExcelRadioButton.isSelected() ? DataExportFormat.EXCEL :
            formatExcelXRadioButton.isSelected() ? DataExportFormat.EXCELX :
            formatCSVRadioButton.isSelected() ? DataExportFormat.CSV :
            formatCustomRadioButton.isSelected() ? DataExportFormat.CUSTOM : null;
    }

    void validateEntries(@NotNull Runnable callback) {
        boolean validOpenQuote = !isEmptyText(beginQuoteTextField);
        boolean validCloseQuote = !isEmptyText(endQuoteTextField);
        boolean validValueSeparator = !isEmptyText(valueSeparatorTextField);
        boolean validFileName = !isEmptyText(fileNameTextField);
        boolean validFileLocation = !isEmptyText(fileLocationTextField.getTextField());
        StringBuilder buffer = new StringBuilder();
        if (valueSeparatorTextField.isEnabled()) {
            if (!validValueSeparator)  buffer.append("Value separator");
        }
        if (beginQuoteTextField.isEnabled() || endQuoteTextField.isEnabled()) {
            if (!validOpenQuote || !validCloseQuote)  buffer.append("Quotes");
        }
        if (fileNameTextField.isEnabled()) {
            if (!validFileName)  {
                if (buffer.length() > 0) buffer.append(", ");
                buffer.append("File Name");
            }
            if (!validFileLocation) {
                if (buffer.length() > 0) buffer.append(", ");
                buffer.append("File Location");
            }
        }

        Project project = getProject();
        if (buffer.length() > 0) {
            buffer.insert(0, "Please provide values for the following fields: ");
            Messages.showErrorDialog(project, "Required input", buffer.toString());
            return;
        }

        if (destinationFileRadioButton.isSelected()) {
            File file = getExportInstructions().getFile();
            if (file.exists()) {
                Messages.showQuestionDialog(project, "File exists",
                        "File " + file.getPath() + " already exists. Overwrite?",
                        Messages.OPTIONS_YES_NO, 0,
                        option -> when(option == 0, callback));

                return;
            }
        }

        callback.run();
    }

    private void enableDisableFields() {
        DataExportProcessor processor = DataExportManager.getExportProcessor(getExportInstructions().getFormat());

        boolean supportsCreateHeader      = HEADER_CREATION.isSupported(processor);
        boolean supportsFriendlyHeader    = FRIENDLY_HEADER.isSupported(processor) && supportsCreateHeader;
        boolean supportsValueQuoting      = VALUE_QUOTING.isSupported(processor);
        boolean supportsExportToClipboard = EXPORT_TO_CLIPBOARD.isSupported(processor);
        boolean supportsFileEncoding      = FILE_ENCODING.isSupported(processor);

        destinationClipboardRadioButton.setEnabled(supportsExportToClipboard);
        quoteValuesCheckBox.setEnabled(supportsValueQuoting);
        quoteAllValuesCheckBox.setEnabled(supportsValueQuoting);
        createHeaderCheckBox.setEnabled(supportsCreateHeader);
        friendlyHeadersCheckBox.setEnabled(supportsFriendlyHeader && createHeaderCheckBox.isSelected());

        if (!destinationClipboardRadioButton.isEnabled() && destinationClipboardRadioButton.isSelected()) {
            destinationFileRadioButton.setSelected(true);
        }

        if (!friendlyHeadersCheckBox.isEnabled()) {
            friendlyHeadersCheckBox.setSelected(false);
        }

        boolean quotingEnabled = (quoteValuesCheckBox.isEnabled() || quoteAllValuesCheckBox.isEnabled()) && quoteValuesCheckBox.isSelected();
        beginQuoteTextField.setEnabled(quotingEnabled);
        endQuoteTextField.setEnabled(quotingEnabled);
        valueSeparatorTextField.setEnabled(formatCustomRadioButton.isSelected());
        fileNameTextField.setEnabled(destinationFileRadioButton.isSelected());
        fileLocationTextField.setEnabled(destinationFileRadioButton.isSelected());
        encodingComboBox.setEnabled(destinationFileRadioButton.isSelected() && supportsFileEncoding);

        String fileNameBase = sourceObject == null ? instructions.getBaseName() : sourceObject.getObjectName();
        if (fileNameBase != null && processor != null) {
            String fileName = fileNameBase + "." + processor.getFileExtension();
            fileNameTextField.setText(fileName);
        }
    }
}
