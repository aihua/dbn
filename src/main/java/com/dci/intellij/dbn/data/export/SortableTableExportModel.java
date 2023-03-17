package com.dci.intellij.dbn.data.export;

import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.data.grid.ui.table.sortable.SortableTable;
import com.dci.intellij.dbn.data.model.ColumnInfo;
import com.dci.intellij.dbn.data.model.sortable.SortableDataModel;
import com.dci.intellij.dbn.data.model.sortable.SortableDataModelCell;
import com.dci.intellij.dbn.data.type.DBNativeDataType;
import com.dci.intellij.dbn.data.type.GenericDataType;
import com.google.common.base.CaseFormat;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SortableTableExportModel implements DataExportModel{
    private final boolean selection;
    private final SortableTable<? extends SortableDataModel> table;

    private final Map<String, String> columnFriendlyNames = new HashMap<>();
    private final List<String> warnings = new ArrayList<>();

    private int[] selectedRows;
    private int[] selectedColumns;

    public SortableTableExportModel(boolean selection, SortableTable<? extends SortableDataModel>  table) {
        this.selection = selection;
        this.table = table;

        if (selection) {
            selectedRows = table.getSelectedRows();
            selectedColumns = table.getSelectedColumns();
        }
    }

    @Override
    public Project getProject() {
        return table.getProject();
    }

    @Override
    public String getTableName() {
        return table.getName();
    }

    @Override
    public int getColumnCount() {
        return selection ?
            selectedColumns.length :
            table.getModel().getColumnCount();
    }

    @Override
    public int getRowCount() {
        return selection ?
            selectedRows.length :
            table.getModel().getRowCount();
    }

    @Override
    public Object getValue(int rowIndex, int columnIndex) {
        int realRowIndex = getRealRowIndex(rowIndex);
        int realColumnIndex = getRealColumnIndex(columnIndex);
        SortableDataModelCell dataModelCell = (SortableDataModelCell) table.getModel().getValueAt(realRowIndex, realColumnIndex);
        return dataModelCell == null ? null : dataModelCell.getUserValue();
    }

    @Override
    public String getColumnName(int columnIndex) {
        int realColumnIndex = getRealColumnIndex(columnIndex);
        return table.getModel().getColumnName(realColumnIndex);
    }

    @Override
    public String getColumnFriendlyName(int columnIndex) {
        String columnName = getColumnName(columnIndex);
        return columnFriendlyNames.computeIfAbsent(columnName, n -> produceColumnFriendlyName(n));
    }

    @Nullable
    private static String produceColumnFriendlyName(String key) {
        if (Strings.isNotEmpty(key)) {
            key = key.trim().toUpperCase();
            if (key.matches("[A-Z][A-Z0-9_]*")) {
                key = key.replaceAll("_", " _");
                return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, key);
            }
        }
        return key;
    }


    @Override
    public GenericDataType getGenericDataType(int columnIndex) {
        int realColumnIndex = getRealColumnIndex(columnIndex);
        ColumnInfo columnInfo = table.getModel().getColumnInfo(realColumnIndex);
        DBNativeDataType nativeDataType = columnInfo.getDataType().getNativeType();

        return nativeDataType == null ?
                GenericDataType.LITERAL :
                nativeDataType.getGenericDataType();

    }

    /**
     * Returns the column index from the underlying sortable table model.
     */
    private int getRealColumnIndex(int columnIndex) {
        if (selection) {
            int selectedColumnIndex = selectedColumns[columnIndex];
            return table.convertColumnIndexToModel(selectedColumnIndex);
        } else {
            return table.convertColumnIndexToModel(columnIndex);
        }
    }

    private int getRealRowIndex(int rowIndex) {
        if (selection) {
            return selectedRows[rowIndex];
        } else {
            return rowIndex;
        }
    }

    @Override
    public void addWarning(String warning) {
        if (!warnings.contains(warning)) warnings.add(warning);
    }
}
