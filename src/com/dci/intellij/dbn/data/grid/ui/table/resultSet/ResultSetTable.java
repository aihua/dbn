package com.dci.intellij.dbn.data.grid.ui.table.resultSet;

import com.dci.intellij.dbn.common.ui.component.DBNComponent;
import com.dci.intellij.dbn.data.grid.ui.table.resultSet.record.ResultSetRecordViewerDialog;
import com.dci.intellij.dbn.data.grid.ui.table.sortable.SortableTable;
import com.dci.intellij.dbn.data.model.resultSet.ResultSetDataModel;
import com.dci.intellij.dbn.data.record.RecordViewInfo;
import org.jetbrains.annotations.NotNull;

public class ResultSetTable<T extends ResultSetDataModel<?, ?>> extends SortableTable<T> {
    private final RecordViewInfo recordViewInfo;
    public ResultSetTable(@NotNull DBNComponent parent, @NotNull T dataModel, boolean enableSpeedSearch, RecordViewInfo recordViewInfo) {
        super(parent, dataModel, enableSpeedSearch);
        this.recordViewInfo = recordViewInfo;
        addMouseListener(new ResultSetTableMouseListener(this));
    }

    public RecordViewInfo getRecordViewInfo() {
        return recordViewInfo;
    }

    public void showRecordViewDialog() {
        ResultSetRecordViewerDialog dialog = new ResultSetRecordViewerDialog(this, showRecordViewDataTypes());
        dialog.show();
    }

    protected boolean showRecordViewDataTypes() {
        return true;
    }

    @NotNull
    @Override
    public T getModel() {
        return super.getModel();
    }
}
