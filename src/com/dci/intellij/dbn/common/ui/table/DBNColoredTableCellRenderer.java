package com.dci.intellij.dbn.common.ui.table;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.ui.ColoredTableCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class DBNColoredTableCellRenderer extends ColoredTableCellRenderer {
    @Override
    protected final void customizeCellRenderer(@NotNull JTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
        try {
            DBNTable dbnTable = (DBNTable) table;
            customizeCellRenderer(dbnTable, value, selected, hasFocus, row, column);
        } catch (ProcessCanceledException ignore) {}
    }

    protected abstract void customizeCellRenderer(DBNTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column);
}
