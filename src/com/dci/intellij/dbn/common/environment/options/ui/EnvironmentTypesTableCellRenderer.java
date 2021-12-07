package com.dci.intellij.dbn.common.environment.options.ui;

import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dci.intellij.dbn.common.ui.table.DBNTable;
import com.dci.intellij.dbn.common.util.Strings;
import com.intellij.ui.ColoredSideBorder;
import com.intellij.ui.SimpleTextAttributes;

import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.Color;

public class EnvironmentTypesTableCellRenderer extends DBNColoredTableCellRenderer {
    @Override
    protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
        if (column == 2 || column == 3) {

        }
        else if (column == 4) {
            Color color = (Color) value;
            if (color == null) color = EnvironmentType.EnvironmentColor.NONE;
            setBackground(color);
            setBorder(new CompoundBorder(
                    new LineBorder(table.getBackground()),
                    new ColoredSideBorder(color.brighter(), color.brighter(), color.darker(), color.darker(), 1)));
        } else {
            String stringValue = (String) value;
            if (Strings.isNotEmpty(stringValue)) {
                append(stringValue, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
        }
    }
}
