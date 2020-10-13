package com.dci.intellij.dbn.object.properties.ui;

import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.ui.table.DBNTableModel;
import com.dci.intellij.dbn.object.properties.PresentableProperty;

import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.List;

public class ObjectPropertiesTableModel extends StatefulDisposable.Base implements DBNTableModel {
    private List<PresentableProperty> presentableProperties = new ArrayList<>();

    ObjectPropertiesTableModel() {}

    ObjectPropertiesTableModel(List<PresentableProperty> presentableProperties) {
        this.presentableProperties = presentableProperties;
    }

    @Override
    public int getRowCount() {
        return presentableProperties.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return
            columnIndex == 0 ? "Property" :
            columnIndex == 1 ? "Value" : null;
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }
    @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        return presentableProperties.get(rowIndex);
    }
    @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
    @Override public void addTableModelListener(TableModelListener l) {}
    @Override public void removeTableModelListener(TableModelListener l) {}

    @Override
    protected void disposeInner() {
        nullify();
    }
}
