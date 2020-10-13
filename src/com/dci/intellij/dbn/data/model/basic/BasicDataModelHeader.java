package com.dci.intellij.dbn.data.model.basic;


import com.dci.intellij.dbn.common.dispose.SafeDisposer;
import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.data.model.ColumnInfo;
import com.dci.intellij.dbn.data.model.DataModelHeader;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.intellij.openapi.util.text.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class BasicDataModelHeader<T extends ColumnInfo> extends StatefulDisposable.Base implements DataModelHeader<T> {
    private final List<T> columnInfos = new ArrayList<T>();


    protected void addColumnInfo(T columnInfo) {
        columnInfos.add(columnInfo);
    }

    @Override
    public List<T> getColumnInfos() {
        return columnInfos;
    }

    @Override
    public T getColumnInfo(int columnIndex) {
        return columnInfos.get(columnIndex);
    }

    @Override
    public int getColumnIndex(String name) {
        for (int i=0; i<columnInfos.size(); i++) {
            T columnInfo = columnInfos.get(i);
            if (StringUtil.equalsIgnoreCase(columnInfo.getName(), name)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return getColumnInfo(columnIndex).getName();
    }

    @Override
    public DBDataType getColumnDataType(int columnIndex) {
        return getColumnInfo(columnIndex).getDataType();
    }

    @Override
    public int getColumnCount() {
        return columnInfos.size();
    }


    /********************************************************
     *                    Disposable                        *
     *******************************************************  */
    @Override
    public void disposeInner() {
        SafeDisposer.dispose(columnInfos, false, false);
        nullify();
    }
}
