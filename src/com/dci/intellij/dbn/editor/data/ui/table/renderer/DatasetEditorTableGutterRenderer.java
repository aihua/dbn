package com.dci.intellij.dbn.editor.data.ui.table.renderer;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.ui.table.DBNTableGutterRendererBase;
import com.dci.intellij.dbn.data.grid.ui.table.basic.BasicTableGutter;
import com.dci.intellij.dbn.editor.data.model.DatasetEditorModelRow;
import com.dci.intellij.dbn.editor.data.ui.table.DatasetEditorTable;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.ListModel;
import java.awt.Color;

import static com.dci.intellij.dbn.editor.data.model.RecordStatus.*;

public class DatasetEditorTableGutterRenderer extends DBNTableGutterRendererBase {

    @Override
    protected void adjustListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        BasicTableGutter tableGutter = (BasicTableGutter) list;
        ListModel model = list.getModel();
        DatasetEditorModelRow row = (DatasetEditorModelRow) model.getElementAt(index);
        DatasetEditorTable table = (DatasetEditorTable) tableGutter.getTable();
        if (row != null) {
            Icon icon =
                    row.is(INSERTING) ? Icons.DATA_EDITOR_ROW_INSERT :
                    row.is(INSERTED) ? Icons.DATA_EDITOR_ROW_INSERTED :
                    row.is(DELETED) ? Icons.DATA_EDITOR_ROW_DELETED :
                    row.is(MODIFIED) ? Icons.DATA_EDITOR_ROW_MODIFIED :
                    table.getModel().is(MODIFIED) ? Icons.DATA_EDITOR_ROW_DEFAULT : null;

            if (icon == null || icon != iconLabel.getIcon()) {
                iconLabel.setIcon(icon);
            }
        }
        //lText.setFont(isSelected ? BOLD_FONT : REGULAR_FONT);

        boolean isCaretRow = table.getCellSelectionEnabled() && table.getSelectedRow() == index && table.getSelectedRowCount() == 1;
        Color background = isSelected ?
                Colors.getTableSelectionBackground(cellHasFocus) :
                isCaretRow ?
                        Colors.getTableCaretRowColor() :
                        Colors.getPanelBackground();
        mainPanel.setBackground(background);
        iconLabel.setBackground(background);
        textLabel.setForeground(isSelected ?
                Colors.getTableSelectionForeground(cellHasFocus) :
                Colors.getTableGutterForeground());
    }
}
