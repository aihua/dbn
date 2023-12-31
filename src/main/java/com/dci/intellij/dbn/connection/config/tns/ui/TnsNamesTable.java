package com.dci.intellij.dbn.connection.config.tns.ui;

import com.dci.intellij.dbn.common.ui.component.DBNComponent;
import com.dci.intellij.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dci.intellij.dbn.common.ui.table.DBNTable;
import com.dci.intellij.dbn.common.ui.table.DBNTableTransferHandler;
import com.dci.intellij.dbn.common.ui.util.Borders;
import com.dci.intellij.dbn.connection.config.tns.TnsNames;
import com.dci.intellij.dbn.connection.config.tns.TnsProfile;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableModel;

public class TnsNamesTable extends DBNTable<TnsNamesTableModel> {

    public TnsNamesTable(@NotNull DBNComponent parent, TnsNames tnsNames) {
        super(parent, new TnsNamesTableModel(tnsNames), true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setDefaultRenderer(TnsProfile.class, new CellRenderer());
        setTransferHandler(DBNTableTransferHandler.INSTANCE);
        initTableSorter();

    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        initTableSorter();
    }

    private class CellRenderer extends DBNColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            TnsProfile entry = (TnsProfile) value;
            Object columnValue = getModel().getPresentableValue(entry, column);
            append(columnValue == null ? "" : columnValue.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            setBorder(Borders.TEXT_FIELD_INSETS);
        }
    }
}
