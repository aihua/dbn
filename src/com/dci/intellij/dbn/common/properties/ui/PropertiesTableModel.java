package com.dci.intellij.dbn.common.properties.ui;

import com.dci.intellij.dbn.common.properties.KeyValueProperty;
import com.dci.intellij.dbn.common.ui.table.DBNEditableTableModel;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.common.util.Safe;
import com.dci.intellij.dbn.common.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertiesTableModel extends DBNEditableTableModel {
    private final List<KeyValueProperty> properties = new ArrayList<KeyValueProperty>();

    public PropertiesTableModel(Map<String, String> propertiesMap) {
        loadProperties(propertiesMap);
    }

    public void loadProperties(Map<String, String> propertiesMap) {
        for (String key : propertiesMap.keySet()) {
            KeyValueProperty property = new KeyValueProperty(key, propertiesMap.get(key));
            properties.add(property);
        }
    }

    public Map<String, String> exportProperties() {
        Map<String, String> propertiesMap = new HashMap<String, String>();

        for (KeyValueProperty property : properties) {
            String key = property.getKey();
            if (!StringUtil.isEmptyOrSpaces(key)) {
                String value = CommonUtil.nvl(property.getValue(), "");
                propertiesMap.put(key, value);
            }
        }
        return propertiesMap;
    }

    @Override
    public int getRowCount() {
        return properties.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnIndex == 0 ? "Property" :
               columnIndex == 1 ? "Value" : null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;

    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return
           columnIndex == 0 ? getKey(rowIndex) :
           columnIndex == 1 ? getValue(rowIndex) : null;
    }

    @Override
    public void setValueAt(Object o, int rowIndex, int columnIndex) {
        Object actualValue = getValueAt(rowIndex, columnIndex);
        if (!Safe.equal(actualValue, o)) {
            KeyValueProperty property = properties.get(rowIndex);
            if (columnIndex == 0) {
                property.setKey((String) o);

            } else if (columnIndex == 1) {
                property.setValue((String) o);
            }

            notifyListeners(rowIndex, rowIndex, columnIndex);
        }
    }

    private String getKey(int rowIndex) {
        KeyValueProperty property = getProperty(rowIndex);
        return property.getKey();
    }

    private String getValue(int rowIndex) {
        KeyValueProperty property = getProperty(rowIndex);
        return property.getValue();
    }

    private KeyValueProperty getProperty(int rowIndex) {
        while (properties.size() <= rowIndex) {
            properties.add(new KeyValueProperty());
        }
        return properties.get(rowIndex);
    }

    @Override
    public void insertRow(int rowIndex) {
        properties.add(rowIndex, new KeyValueProperty());
        notifyListeners(rowIndex, properties.size()-1, -1);
    }

    @Override
    public void removeRow(int rowIndex) {
        if (properties.size() > rowIndex) {
            properties.remove(rowIndex);
            notifyListeners(rowIndex, properties.size()-1, -1);
        }
    }
}
