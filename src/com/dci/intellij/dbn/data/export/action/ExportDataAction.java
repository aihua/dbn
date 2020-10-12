package com.dci.intellij.dbn.data.export.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.data.export.ui.ExportDataDialog;
import com.dci.intellij.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.dci.intellij.dbn.object.DBDataset;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ExportDataAction extends AnAction {
    private final WeakRef<ResultSetTable<?>> table;
    private final DBObjectRef<DBDataset> dataset;

    public ExportDataAction(ResultSetTable<?> table, DBDataset dataset) {
        super("Export Data", null, Icons.DATA_EXPORT);
        this.table = WeakRef.of(table);
        this.dataset = DBObjectRef.of(dataset);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ExportDataDialog dialog = new ExportDataDialog(table.ensure(), dataset.ensure());
        dialog.show();
    }
}
