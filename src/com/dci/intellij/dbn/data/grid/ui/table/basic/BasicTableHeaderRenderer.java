package com.dci.intellij.dbn.data.grid.ui.table.basic;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.ui.Borders;
import com.dci.intellij.dbn.common.ui.table.DBNTableHeaderRendererBase;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.List;

public class BasicTableHeaderRenderer extends DBNTableHeaderRendererBase {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JLabel sortingLabel;

    public BasicTableHeaderRenderer() {
        mainPanel.setOpaque(true);
        mainPanel.setBackground(Colors.getPanelBackground());
        mainPanel.setBorder(Borders.lineBorder(Colors.getTableHeaderGridColor(), 0, 0, 0, 1));
        nameLabel.setForeground(Colors.getLabelForeground());
        sortingLabel.setText("");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        int width = 0;
        String columnName = value.toString();

        nameLabel.setText(columnName);

        FontMetrics fontMetrics = getFontMetrics();
        width += fontMetrics.stringWidth(columnName) + 24;
        int height = fontMetrics.getHeight() + 6;
        mainPanel.setPreferredSize(new Dimension(width, height));

        Icon icon = null;
        RowSorter rowSorter = table.getRowSorter();
        if (rowSorter != null) {
            final Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            mainPanel.setCursor(handCursor);
            nameLabel.setCursor(handCursor);
            sortingLabel.setCursor(handCursor);
            List<? extends RowSorter.SortKey> sortKeys = rowSorter.getSortKeys();
            if (sortKeys.size() == 1) {
                RowSorter.SortKey sortKey = sortKeys.get(0);
                if (sortKey.getColumn() == columnIndex) {
                    SortOrder sortOrder = sortKey.getSortOrder();
                    icon =
                            sortOrder == SortOrder.ASCENDING ? Icons.DATA_EDITOR_SORT_ASC :
                            sortOrder == SortOrder.DESCENDING ? Icons.DATA_EDITOR_SORT_DESC :
                            null;
                }
            }
        }
        sortingLabel.setIcon(icon);
        return mainPanel;
    }

    @Override
    protected JLabel getNameLabel() {
        return nameLabel;
    }
}
