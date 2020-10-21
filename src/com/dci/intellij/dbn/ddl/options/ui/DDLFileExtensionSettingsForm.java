package com.dci.intellij.dbn.ddl.options.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorUtil;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.ddl.DDLFileManager;
import com.dci.intellij.dbn.ddl.DDLFileType;
import com.dci.intellij.dbn.ddl.DDLFileTypeId;
import com.dci.intellij.dbn.ddl.options.DDLFileExtensionSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dci.intellij.dbn.common.ui.GUIUtil.updateBorderTitleForeground;

public class DDLFileExtensionSettingsForm extends ConfigurationEditorForm<DDLFileExtensionSettings> {
    private JPanel mainPanel;
    private JLabel viewIconLabel;
    private JLabel triggerIconLabel;
    private JLabel procedureIconLabel;
    private JLabel functionIconLabel;
    private JLabel packageIconLabel;
    private JLabel typeIconLabel;
    private JTextField viewTextField;
    private JTextField triggerTextField;
    private JTextField procedureTextField;
    private JTextField functionTextField;
    private JTextField packageTextField;
    private JTextField packageSpecTextField;
    private JTextField packageBodyTextField;
    private JTextField typeTextField;
    private JTextField typeSpecTextField;
    private JTextField typeBodyTextField;

    private Map<String, JTextField> extensionTextFields = new HashMap<String, JTextField>();

    public DDLFileExtensionSettingsForm(DDLFileExtensionSettings settings) {
        super(settings);
        updateBorderTitleForeground(mainPanel);
        resetFormChanges();
        viewIconLabel.setText(null);
        triggerIconLabel.setText(null);
        procedureIconLabel.setText(null);
        functionIconLabel.setText(null);
        packageIconLabel.setText(null);
        typeIconLabel.setText(null);

        viewIconLabel.setIcon(Icons.DBO_VIEW);
        triggerIconLabel.setIcon(Icons.DBO_TRIGGER);
        procedureIconLabel.setIcon(Icons.DBO_PROCEDURE);
        functionIconLabel.setIcon(Icons.DBO_FUNCTION);
        packageIconLabel.setIcon(Icons.DBO_PACKAGE);
        typeIconLabel.setIcon(Icons.DBO_TYPE);

        registerComponent(mainPanel);

        extensionTextFields.put("View", viewTextField);
        extensionTextFields.put("Trigger", triggerTextField);
        extensionTextFields.put("Procedure", procedureTextField);
        extensionTextFields.put("Function", functionTextField);
        extensionTextFields.put("Package", packageTextField);
        extensionTextFields.put("Package Spec", packageSpecTextField);
        extensionTextFields.put("Package Body", packageBodyTextField);
        extensionTextFields.put("Type", typeTextField);
        extensionTextFields.put("Type Spec", typeSpecTextField);
        extensionTextFields.put("Type Bpdy", typeBodyTextField);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    private void validateInputs() throws ConfigurationException {
        List<String> allExtensions = new ArrayList<String>();
        for (String fieldName : extensionTextFields.keySet()) {
            JTextField extensionTextField = extensionTextFields.get(fieldName);
            String extensionsText = ConfigurationEditorUtil.validateStringInputValue(extensionTextField, fieldName, false);
            List<String> extensions = StringUtil.tokenize(extensionsText, ",");
            for (String extension : extensions) {
                if (allExtensions.contains(extension)) {
                    throw new ConfigurationException("Duplicate value for extension \"" + extension + "\" found.");
                }
            }
            allExtensions.addAll(extensions);
        }
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        validateInputs();
        AtomicBoolean changed = new AtomicBoolean(false);
        applySetting(viewTextField, DDLFileTypeId.VIEW, changed);
        applySetting(triggerTextField, DDLFileTypeId.TRIGGER, changed);
        applySetting(procedureTextField, DDLFileTypeId.PROCEDURE, changed);
        applySetting(functionTextField, DDLFileTypeId.FUNCTION, changed);
        applySetting(packageTextField, DDLFileTypeId.PACKAGE, changed);
        applySetting(packageSpecTextField, DDLFileTypeId.PACKAGE_SPEC, changed);
        applySetting(packageBodyTextField, DDLFileTypeId.PACKAGE_BODY, changed);
        applySetting(typeTextField, DDLFileTypeId.TYPE, changed);
        applySetting(typeSpecTextField, DDLFileTypeId.TYPE_SPEC, changed);
        applySetting(typeBodyTextField, DDLFileTypeId.TYPE_BODY, changed);

        if (changed.get()) {
            Project project = getConfiguration().getProject();
            DDLFileManager ddlFileManager = DDLFileManager.getInstance(project);
            ddlFileManager.registerExtensions(getConfiguration());
        }
    }

    private void applySetting(JTextField textField, DDLFileTypeId fileTypeId, AtomicBoolean changed) throws ConfigurationException {
        DDLFileType ddlFileType = getConfiguration().getDDLFileType(fileTypeId);
        boolean valueChanged = ddlFileType.setExtensionsAsString(textField.getText().trim());
        if (valueChanged) {
            changed.set(true);
        }
    }

    @Override
    public void resetFormChanges() {
        resetSetting(viewTextField, DDLFileTypeId.VIEW);
        resetSetting(triggerTextField, DDLFileTypeId.TRIGGER);
        resetSetting(procedureTextField, DDLFileTypeId.PROCEDURE);
        resetSetting(functionTextField, DDLFileTypeId.FUNCTION);
        resetSetting(packageTextField, DDLFileTypeId.PACKAGE);
        resetSetting(packageSpecTextField, DDLFileTypeId.PACKAGE_SPEC);
        resetSetting(packageBodyTextField, DDLFileTypeId.PACKAGE_BODY);
        resetSetting(typeTextField, DDLFileTypeId.TYPE);
        resetSetting(typeSpecTextField, DDLFileTypeId.TYPE_SPEC);
        resetSetting(typeBodyTextField, DDLFileTypeId.TYPE_BODY);
    }

    private void resetSetting(JTextField textField, DDLFileTypeId fileTypeId) {
        textField.setText(getConfiguration().getDDLFileType(fileTypeId).getExtensionsAsString());
    }
}
