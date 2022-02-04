package com.dci.intellij.dbn.common.ui.table;

import com.dci.intellij.dbn.common.color.Colors;

import javax.swing.JList;

public class IndexTableGutterCellRenderer extends DBNTableGutterRendererBase {


    @Override
    protected void adjustListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        DBNTableGutter tableGutter = (DBNTableGutter) list;
        DBNTable table = tableGutter.getTable();
        boolean isCaretRow = table.getCellSelectionEnabled() && table.getSelectedRow() == index && table.getSelectedRowCount() == 1;

        mainPanel.setBackground(isSelected ?
                Colors.getTableSelectionBackground(true) :
                isCaretRow ?
                        Colors.getTableCaretRowColor() :
                        Colors.getPanelBackground());
        textLabel.setForeground(isSelected ?
                Colors.getTableSelectionForeground(cellHasFocus) :
                Colors.getTableLineNumberColor());
    }
}
