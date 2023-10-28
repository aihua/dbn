package com.dci.intellij.dbn.data.editor.ui.calendar;

import com.dci.intellij.dbn.common.color.Colors;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/******************************************************
 *                  TableCellRenderers                *
 ******************************************************/
class CalendarTableCellRenderer extends DefaultTableCellRenderer {
    static final Border SELECTION_BORDER = new CompoundBorder(new LineBorder(Colors.getLabelForeground(), 1, false), JBUI.Borders.emptyRight(6));
    static final Border EMPTY_BORDER = JBUI.Borders.empty(1, 1, 1, 9);
    static final Color INACTIVE_DAY_COLOR = new JBColor(new Color(0xC0C0C0), new Color(0x5B5B5B));

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        CalendarTableModel model = (CalendarTableModel) table.getModel();

        boolean isInputDate = model.isInputDate(row, column);
        boolean isFromActiveMonth = model.isFromActiveMonth(row, column);
        Color foreground =
                isInputDate ? Colors.getTableForeground() :
                        isFromActiveMonth ? Colors.getLabelForeground() : INACTIVE_DAY_COLOR;

        setForeground(isSelected ? Colors.getTableSelectionForeground(true) : foreground);
        setHorizontalAlignment(RIGHT);
        setBorder(isInputDate && !isSelected ? SELECTION_BORDER : EMPTY_BORDER);
        setBackground(isSelected ? Colors.getListSelectionBackground(true) : Colors.getTableBackground());
        //setBorder(new DottedBorder(Color.BLACK));
        return component;
    }
}
