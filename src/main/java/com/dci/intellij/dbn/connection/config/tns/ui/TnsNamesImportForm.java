package com.dci.intellij.dbn.connection.config.tns.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.config.tns.TnsNames;
import com.dci.intellij.dbn.connection.config.tns.TnsNamesParser;
import com.dci.intellij.dbn.connection.config.tns.TnsProfile;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.List;

import static com.dci.intellij.dbn.common.ui.util.TextFields.onTextChange;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;

public class TnsNamesImportForm extends DBNFormBase {
    private TextFieldWithBrowseButton tnsNamesFileTextField;
    private JTextField filterTextField;
    private JBScrollPane tnsNamesScrollPanel;
    private JPanel mainPanel;
    private JLabel errorLabel;

    private final TnsNamesTable tnsNamesTable;

    @Getter
    private TnsNames tnsNames;

    TnsNamesImportForm(@NotNull TnsNamesImportDialog parent, @Nullable File file) {
        super(parent);
        tnsNamesTable = new TnsNamesTable(this, new TnsNames());
        tnsNamesScrollPanel.setViewportView(tnsNamesTable);
        tnsNamesScrollPanel.getViewport().setBackground(Colors.getTableBackground());
        errorLabel.setIcon(Icons.COMMON_ERROR);
        errorLabel.setVisible(false);

        if (file != null) {
            tnsNamesFileTextField.setText(file.getPath());
            updateTnsNamesTable();
        }
        updateSelections();

        tnsNamesTable.getSelectionModel().addListSelectionListener(e -> updateSelections());

        tnsNamesFileTextField.addBrowseFolderListener(
                null,
                null,
                getProject(),
                TnsNamesParser.FILE_CHOOSER_DESCRIPTOR);

        onTextChange(tnsNamesFileTextField, e -> updateTnsNamesTable());
        onTextChange(filterTextField, e -> filterTnsNamesTable());
    }

    private void filterTnsNamesTable() {
        TnsNamesTableModel model = tnsNamesTable.getModel();
        model.filter(filterTextField.getText());
    }

    private void updateSelections() {
        int rowCount = tnsNamesTable.getRowCount();
        int selectedRowCount = tnsNamesTable.getSelectedRowCount();

        TnsNamesImportDialog parentComponent = ensureParentComponent();
        parentComponent.getImportSelectedAction().setEnabled(selectedRowCount > 0);
        parentComponent.getImportAllAction().setEnabled(rowCount > 0);

        List<TnsProfile> profiles = tnsNamesTable.getModel().getProfiles();
        for (int i = 0; i < rowCount; i++) {
            boolean selected = tnsNamesTable.isRowSelected(i);
            TnsProfile profile = profiles.get(tnsNamesTable.convertRowIndexToModel(i));
            profile.setSelected(selected);
        }
    }

    private void updateTnsNamesTable() {
        try {
            String fileName = tnsNamesFileTextField.getTextField().getText();
            if (Strings.isNotEmpty(fileName)) {
                tnsNames = TnsNamesParser.get(new File(fileName));
                tnsNamesTable.setModel(new TnsNamesTableModel(tnsNames));
                tnsNamesTable.accommodateColumnsSize();
                filterTextField.setText(tnsNames.getFilter().getText());
            }
            errorLabel.setVisible(false);
        } catch (Exception e) {
            conditionallyLog(e);
            tnsNamesTable.setModel(new TnsNamesTableModel(new TnsNames()));
            tnsNamesTable.accommodateColumnsSize();

            errorLabel.setVisible(true);
            String message = e.getMessage();
            message = Strings.isEmpty(message) ? "File may be corrupt or not a valid tnsnames.ora file." : message;
            errorLabel.setText("Error reading file: " + message);
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }


}
