package com.dci.intellij.dbn.data.grid.ui.table.basic;

import com.dci.intellij.dbn.data.model.ColumnInfo;
import com.dci.intellij.dbn.data.model.DataModelHeader;
import com.dci.intellij.dbn.data.model.basic.BasicDataModel;
import com.intellij.ui.SpeedSearchBase;

public class BasicTableSpeedSearch extends SpeedSearchBase<BasicTable<? extends BasicDataModel>> {

    private ColumnInfo[] columnInfos;
    private int columnIndex = 0;

    public BasicTableSpeedSearch(BasicTable<? extends BasicDataModel> table) {
        super(table);
    }

    BasicTable<? extends BasicDataModel> getTable() {
        return getComponent();
    }

    @Override
    protected int getSelectedIndex() {
        return columnIndex;
    }

    @Override
    protected Object[] getAllElements() {
        if (columnInfos == null) {
            DataModelHeader<? extends ColumnInfo> modelHeader = getTable().getModel().getHeader();
            columnInfos = modelHeader.getColumnInfos().toArray(new ColumnInfo[modelHeader.getColumnCount()]);
        }
        return columnInfos;
    }

    @Override
    protected String getElementText(Object o) {
        ColumnInfo columnInfo = (ColumnInfo) o;
        return columnInfo.getName();
    }

    @Override
    protected void selectElement(Object o, String s) {
        for(ColumnInfo columnInfo : columnInfos) {
            if (columnInfo == o) {
                columnIndex = columnInfo.getColumnIndex();
                BasicTable table = getTable();
                int rowIndex = table.getSelectedRow();
                if (rowIndex == -1) rowIndex = 0;
                table.scrollRectToVisible(table.getCellRect(rowIndex, columnIndex, true));
                table.setColumnSelectionInterval(columnIndex, columnIndex);
                break;
            }
        }
    }

    
}