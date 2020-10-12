package com.dci.intellij.dbn.execution.script.options.ui;

import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.DBNForm;
import com.dci.intellij.dbn.common.ui.table.DBNTable;
import com.dci.intellij.dbn.connection.DatabaseType;
import com.dci.intellij.dbn.execution.script.CmdLineInterface;
import com.dci.intellij.dbn.execution.script.CmdLineInterfaceBundle;
import com.dci.intellij.dbn.execution.script.ScriptExecutionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.TableUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Set;

public class CmdLineInterfacesTable extends DBNTable<CmdLineInterfacesTableModel> {

    CmdLineInterfacesTable(DBNForm parent, CmdLineInterfaceBundle environmentTypes) {
        super(parent, new CmdLineInterfacesTableModel(environmentTypes), true);
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        getSelectionModel().addListSelectionListener(selectionListener);
        setSelectionBackground(UIUtil.getTableBackground());
        setSelectionForeground(UIUtil.getTableForeground());
        setCellSelectionEnabled(true);
        setDefaultRenderer(Object.class, new CmdLineInterfacesTableCellRenderer());
        adjustRowHeight(3);

        columnModel.getColumn(0).setMaxWidth(100);
        columnModel.getColumn(1).setMaxWidth(220);
        columnModel.getColumn(0).setPreferredWidth(100);
        columnModel.getColumn(1).setPreferredWidth(220);

        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        addMouseListener(mouseListener);
    }

    public void setExecutorBundle(CmdLineInterfaceBundle executorBundle) {
        super.setModel(new CmdLineInterfacesTableModel(executorBundle));
        columnModel.getColumn(0).setMaxWidth(120);
        columnModel.getColumn(1).setMaxWidth(120);
        columnModel.getColumn(0).setPreferredWidth(120);
        columnModel.getColumn(1).setPreferredWidth(120);
    }

    private MouseListener mouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                Point point = e.getPoint();
                int rowIndex = rowAtPoint(point);
                int columnIndex = columnAtPoint(point);
                DatabaseType databaseType = (DatabaseType) getValueAt(rowIndex, 0);
                if (columnIndex == 2) {
                    Project project = getProject();
                    String executablePath = (String) getValueAt(rowIndex, 2);
                    ScriptExecutionManager scriptExecutionManager = ScriptExecutionManager.getInstance(project);
                    VirtualFile virtualFile = scriptExecutionManager.selectCmdLineExecutable(databaseType, executablePath);
                    if (virtualFile != null) {
                        setValueAt(virtualFile.getPath(), rowIndex, 2);
                    }
                }
            }
        }
    };

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        Point point = e.getPoint();
        int columnIndex = columnAtPoint(point);
        setCursor(columnIndex == 2 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }


    private ListSelectionListener selectionListener = e -> {
        if (!e.getValueIsAdjusting()) {
            startCellEditing();
        }
    };

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        super.columnSelectionChanged(e);
        JTableHeader tableHeader = getTableHeader();
        if (tableHeader != null && tableHeader.getDraggedColumn() == null) {
            if (!e.getValueIsAdjusting()) {
                startCellEditing();
            }
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 1;
    }

    private void startCellEditing() {
        if (getModel().getRowCount() > 0) {
            int[] selectedRows = getSelectedRows();
            int selectedRow = selectedRows.length > 0 ? selectedRows[0] : 0;

            int[] selectedColumns = getSelectedColumns();
            int selectedColumn = selectedColumns.length > 0 ? selectedColumns[0] : 0;

            editCellAt(selectedRow, selectedColumn);
        }
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        super.editingStopped(e);
        CmdLineInterfacesTableModel model = getModel();
        model.notifyListeners(0, model.getRowCount(), 0);
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int rowIndex, int columnIndex) {
        JTextField textField = (JTextField) super.prepareEditor(editor, rowIndex, columnIndex);
        textField.setBorder(JBUI.Borders.emptyLeft(3));

        Dispatch.run(() -> {
            textField.selectAll();
            textField.grabFocus();
        });

        selectCell(rowIndex, columnIndex);
        return textField;
    }

    public void insertRow() {
        stopCellEditing();
        int rowIndex = getSelectedRow();
        CmdLineInterfacesTableModel model = getModel();
        rowIndex = model.getRowCount() == 0 ? 0 : rowIndex + 1;
        model.insertRow(rowIndex);

        resizeAndRepaint();

        getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
    }


    public void removeRow() {
        stopCellEditing();
        int selectedRow = getSelectedRow();
        CmdLineInterfacesTableModel model = getModel();
        model.removeRow(selectedRow);

        resizeAndRepaint();

        if (model.getRowCount() == selectedRow && selectedRow > 0) {
            getSelectionModel().setSelectionInterval(selectedRow -1, selectedRow -1);
        }
    }

    void moveRowUp() {
        int selectedRow = getSelectedRow();
        int selectedColumn = getSelectedColumn();
        stopCellEditing();
        TableUtil.moveSelectedItemsUp(this);
        selectCell(selectedRow -1, selectedColumn);
    }

    void moveRowDown() {
        int selectedRow = getSelectedRow();
        int selectedColumn = getSelectedColumn();
        stopCellEditing();
        TableUtil.moveSelectedItemsDown(this);
        selectCell(selectedRow + 1, selectedColumn);
    }

    void addInterface(CmdLineInterface value) {
        getModel().addInterface(value);
        resizeAndRepaint();
    }

    public Set<String> getNames() {
        return getModel().getInterfaceNames();
    }
}
