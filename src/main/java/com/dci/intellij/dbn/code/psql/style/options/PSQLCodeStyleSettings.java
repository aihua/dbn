package com.dci.intellij.dbn.code.psql.style.options;

import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dci.intellij.dbn.code.common.style.options.CodeStyleFormattingSettings;
import com.dci.intellij.dbn.code.common.style.options.DBLCodeStyleSettings;
import com.dci.intellij.dbn.code.psql.style.options.ui.PSQLCodeStyleSettingsEditorForm;
import com.dci.intellij.dbn.common.Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PSQLCodeStyleSettings extends DBLCodeStyleSettings<DBLCodeStyleSettings, PSQLCodeStyleSettingsEditorForm> {

    PSQLCodeStyleSettings(DBLCodeStyleSettings parent) {
        super(parent);
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return Icons.FILE_PLSQL;
    }

    @Override
    public String getDisplayName() {
        return "PL/SQL";
    }

    @Override
    protected CodeStyleCaseSettings createCaseSettings(DBLCodeStyleSettings parent) {
        return new PSQLCodeStyleCaseSettings(parent);
    }

    @Override
    protected CodeStyleFormattingSettings createAttributeSettings(DBLCodeStyleSettings parent) {
        return new PSQLCodeStyleFormattingSettings(parent);
    }

    @Override
    protected String getElementName() {
        return "DBN-PSQL";
    }

    /*********************************************************
    *                     Configuration                     *
    *********************************************************/
    @Override
    @NotNull
    public PSQLCodeStyleSettingsEditorForm createConfigurationEditor() {
        return new PSQLCodeStyleSettingsEditorForm(this);
    }


}