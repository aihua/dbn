package com.dci.intellij.dbn.common.properties.ui;

import com.dci.intellij.dbn.common.ui.DBNForm;
import com.dci.intellij.dbn.common.ui.table.DBNEditableTable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PropertiesEditorTable extends DBNEditableTable<PropertiesTableModel> {

    public PropertiesEditorTable(@NotNull DBNForm parent, Map<String, String> properties) {
        super(parent, new PropertiesTableModel(properties), true);
    }

    public void setProperties(Map<String, String> properties) {
        setModel(new PropertiesTableModel(properties));
    }
}