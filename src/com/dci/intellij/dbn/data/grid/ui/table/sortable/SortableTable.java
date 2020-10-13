package com.dci.intellij.dbn.data.grid.ui.table.sortable;

import com.dci.intellij.dbn.common.ui.GUIUtil;
import com.dci.intellij.dbn.common.ui.component.DBNComponent;
import com.dci.intellij.dbn.data.grid.ui.table.basic.BasicTable;
import com.dci.intellij.dbn.data.grid.ui.table.basic.BasicTableSpeedSearch;
import com.dci.intellij.dbn.data.model.ColumnInfo;
import com.dci.intellij.dbn.data.model.sortable.SortableDataModel;
import com.dci.intellij.dbn.data.model.sortable.SortableTableHeaderMouseListener;
import com.dci.intellij.dbn.data.model.sortable.SortableTableMouseListener;
import com.dci.intellij.dbn.data.sorting.SortDirection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.JTableHeader;

public abstract class SortableTable<T extends SortableDataModel<?, ?>> extends BasicTable<T> {

    public SortableTable(@NotNull DBNComponent parent, @NotNull T dataModel, boolean enableSpeedSearch) {
        super(parent, dataModel);
        addMouseListener(new SortableTableMouseListener(this));
        JTableHeader tableHeader = getTableHeader();
        tableHeader.setDefaultRenderer(new SortableTableHeaderRenderer());
        tableHeader.addMouseListener(new SortableTableHeaderMouseListener(this));

        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setCellSelectionEnabled(true);
        accommodateColumnsSize();
        if (enableSpeedSearch) {
            new BasicTableSpeedSearch(this);
        }
    }

    public void sort() {
        getModel().sort();
        JTableHeader tableHeader = getTableHeader();
        GUIUtil.repaint(tableHeader);
    }

    public boolean sort(int columnIndex, SortDirection sortDirection, boolean keepExisting) {
        SortableDataModel<?, ?> model = getModel();
        int modelColumnIndex = convertColumnIndexToModel(columnIndex);
        ColumnInfo columnInfo = model.getColumnInfo(modelColumnIndex);
        if (columnInfo.isSortable()) {
            boolean sorted = model.sort(modelColumnIndex, sortDirection, keepExisting);
            if (sorted) {
                JTableHeader tableHeader = getTableHeader();
                GUIUtil.repaint(tableHeader);
            }
            return sorted;
        }
        return false;
    }

}
