package com.dci.intellij.dbn.editor.data.options;

import com.dci.intellij.dbn.common.options.BasicConfiguration;
import com.dci.intellij.dbn.common.options.setting.Settings;
import com.dci.intellij.dbn.editor.data.filter.DatasetFilterType;
import com.dci.intellij.dbn.editor.data.options.ui.DataEditorFilterSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DataEditorFilterSettings extends BasicConfiguration<DataEditorSettings, DataEditorFilterSettingsForm> {
    private boolean promptFilterDialog = true;
    private DatasetFilterType defaultFilterType = DatasetFilterType.BASIC;

    DataEditorFilterSettings(DataEditorSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return "Data editor filters settings";
    }

    @Override
    public String getHelpTopic() {
        return "dataEditor";
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    @NotNull
    public DataEditorFilterSettingsForm createConfigurationEditor() {
        return new DataEditorFilterSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "filters";
    }

    @Override
    public void readConfiguration(Element element) {
        promptFilterDialog = Settings.getBoolean(element, "prompt-filter-dialog", promptFilterDialog);
        defaultFilterType = DatasetFilterType.get(Settings.getString(element, "default-filter-type", defaultFilterType.name()));
    }

    @Override
    public void writeConfiguration(Element element) {
        Settings.setBoolean(element, "prompt-filter-dialog", promptFilterDialog);
        Settings.setString(element, "default-filter-type", defaultFilterType.name());
    }
}
