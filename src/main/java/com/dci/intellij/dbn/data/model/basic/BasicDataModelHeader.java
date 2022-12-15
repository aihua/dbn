package com.dci.intellij.dbn.data.model.basic;


import com.dci.intellij.dbn.common.collections.CaseInsensitiveStringKeyMap;
import com.dci.intellij.dbn.common.dispose.Disposed;
import com.dci.intellij.dbn.common.dispose.StatefulDisposableBase;
import com.dci.intellij.dbn.data.model.ColumnInfo;
import com.dci.intellij.dbn.data.model.DataModelHeader;
import com.dci.intellij.dbn.data.type.DBDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dci.intellij.dbn.common.dispose.Disposer.replace;

public class BasicDataModelHeader<T extends ColumnInfo> extends StatefulDisposableBase implements DataModelHeader<T> {
    private List<T> columnInfos = new ArrayList<>();
    private Map<String, T> nameIndex = new CaseInsensitiveStringKeyMap<>();


    protected void addColumnInfo(T columnInfo) {
        columnInfos.add(columnInfo);
        nameIndex.put(columnInfo.getName(), columnInfo);
    }

    @Override
    public List<T> getColumnInfos() {
        return columnInfos;
    }

    @Override
    public T getColumnInfo(int index) {
        return columnInfos.get(index);
    }

    @Override
    public T getColumnInfo(String name) {
        return nameIndex.get(name);
    }

    @Override
    public int getColumnIndex(String name) {
        T columnInfo = getColumnInfo(name);
        return columnInfo == null ? -1 : columnInfo.getIndex();
    }

    @Override
    public String getColumnName(int index) {
        return getColumnInfo(index).getName();
    }

    @Override
    public DBDataType getColumnDataType(int index) {
        return getColumnInfo(index).getDataType();
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
        columnInfos = replace(columnInfos, Disposed.list());
        nameIndex = replace(nameIndex, Disposed.map());
        nullify();
    }
}
