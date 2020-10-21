package com.dci.intellij.dbn.editor.data.record.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.DisposableContainer;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.ui.DBNHeaderForm;
import com.dci.intellij.dbn.common.ui.GUIUtil;
import com.dci.intellij.dbn.common.util.ActionUtil;
import com.dci.intellij.dbn.data.record.ColumnSortingType;
import com.dci.intellij.dbn.editor.data.DatasetEditorManager;
import com.dci.intellij.dbn.editor.data.model.DatasetEditorColumnInfo;
import com.dci.intellij.dbn.editor.data.model.DatasetEditorModel;
import com.dci.intellij.dbn.editor.data.model.DatasetEditorModelCell;
import com.dci.intellij.dbn.editor.data.model.DatasetEditorModelRow;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.dci.intellij.dbn.object.DBDataset;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class DatasetRecordEditorForm extends DBNFormImpl {
    private JPanel actionsPanel;
    private JPanel columnsPanel;
    private JPanel mainPanel;
    private JScrollPane columnsPanelScrollPane;
    private JPanel headerPanel;

    private final List<DatasetRecordEditorColumnForm> columnForms = DisposableContainer.list(this);

    private WeakRef<DatasetEditorModelRow> row;

    public DatasetRecordEditorForm(DatasetRecordEditorDialog parentComponent, DatasetEditorModelRow row) {
        super(parentComponent);
        this.row = WeakRef.of(row);
        DBDataset dataset = row.getModel().getDataset();

        DBNHeaderForm headerForm = new DBNHeaderForm(this, dataset);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        ActionToolbar actionToolbar = ActionUtil.createActionToolbar(
                "DBNavigator.Place.DataEditor.TextAreaPopup", true,
                new SortAlphabeticallyAction(),
                ActionUtil.SEPARATOR,
                new FirstRecordAction(),
                new PreviousRecordAction(),
                new NextRecordAction(),
                new LastRecordAction());
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);


        columnsPanel.setLayout(new BoxLayout(columnsPanel, BoxLayout.Y_AXIS));

        for (DatasetEditorModelCell cell: row.getCells()) {
            DatasetRecordEditorColumnForm columnForm = new DatasetRecordEditorColumnForm(this, cell);
            columnForms.add(columnForm);
        }

        Project project = ensureProject();
        DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(project);
        ColumnSortingType columnSortingType = datasetEditorManager.getRecordViewColumnSortingType();
        sortColumns(columnSortingType);

        int[] metrics = new int[]{0, 0};
        for (DatasetRecordEditorColumnForm columnForm : columnForms) {
            metrics = columnForm.getMetrics(metrics);
        }

        for (DatasetRecordEditorColumnForm columnForm : columnForms) {
            columnForm.adjustMetrics(metrics);
        }

        Dimension preferredSize = mainPanel.getPreferredSize();
        int width = (int) preferredSize.getWidth() + 24;
        int height = (int) Math.min(preferredSize.getHeight(), 380);
        mainPanel.setPreferredSize(new Dimension(width, height));

        if (columnForms.size() > 0) {
            int scrollUnitIncrement = (int) columnForms.get(0).getComponent().getPreferredSize().getHeight();
            columnsPanelScrollPane.getVerticalScrollBar().setUnitIncrement(scrollUnitIncrement);
        }
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return isDisposed() || columnForms.size() == 0 ? null : columnForms.get(0).getEditorComponent();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public JComponent getColumnsPanel() {
        return columnsPanel;
    }

    @NotNull
    public DatasetEditorModelRow getRow() {
        return row.ensure();
    }

    public int geRowIndex() {
        return getRow().getIndex();
    }

    @NotNull
    public DatasetEditorModel getModel() {
        return getRow().getModel();
    }

    public void setRow(DatasetEditorModelRow row) {
        this.row = WeakRef.of(row);
        for (DatasetEditorModelCell cell : row.getCells()) {
            DatasetRecordEditorColumnForm columnForm = getColumnPanel(cell.getColumnInfo());
            if (columnForm != null) {
                columnForm.setCell(cell);
            }
        }
    }

    private DatasetRecordEditorColumnForm getColumnPanel(DatasetEditorColumnInfo columnInfo) {
        for (DatasetRecordEditorColumnForm columnForm : columnForms) {
            if (columnForm.getCell().getColumnInfo() == columnInfo) {
                return columnForm;
            }
        }
        return null;
    }

    /*********************************************************
     *                   Column sorting                      *
     *********************************************************/
    private void sortColumns(ColumnSortingType columnSortingType) {
        Comparator<DatasetRecordEditorColumnForm> comparator =
                columnSortingType == ColumnSortingType.ALPHABETICAL ? ALPHANUMERIC_COMPARATOR :
                columnSortingType == ColumnSortingType.BY_INDEX ? INDEXED_COMPARATOR : null;

        if (comparator != null) {
            columnForms.sort(comparator);
            columnsPanel.removeAll();
            for (DatasetRecordEditorColumnForm columnForm : columnForms) {
                columnsPanel.add(columnForm.getComponent());
            }
            GUIUtil.repaint(columnsPanel);
        }
    }

    private static final Comparator<DatasetRecordEditorColumnForm> ALPHANUMERIC_COMPARATOR = (columnPanel1, columnPanel2) -> {
        String name1 = columnPanel1.getCell().getColumnInfo().getName();
        String name2 = columnPanel2.getCell().getColumnInfo().getName();
        return name1.compareTo(name2);
    };

    private static final Comparator<DatasetRecordEditorColumnForm> INDEXED_COMPARATOR = (columnPanel1, columnPanel2) -> {
        int index1 = columnPanel1.getCell().getColumnInfo().getColumnIndex();
        int index2 = columnPanel2.getCell().getColumnInfo().getColumnIndex();
        return index1-index2;
    };

    public void focusNextColumnPanel(DatasetRecordEditorColumnForm source) {
        int index = columnForms.indexOf(source);
        if (index < columnForms.size() - 1) {
            DatasetRecordEditorColumnForm columnForm = columnForms.get(index + 1);
            columnForm.getEditorComponent().requestFocus();
        }
    }

    public void focusPreviousColumnPanel(DatasetRecordEditorColumnForm source) {
        int index = columnForms.indexOf(source);
        if (index > 0) {
            DatasetRecordEditorColumnForm columnForm = columnForms.get(index - 1);
            columnForm.getEditorComponent().requestFocus();
        }
    }

    /*********************************************************      
     *                       Actions                         *
     *********************************************************/
    private class SortAlphabeticallyAction extends ToggleAction {
        private SortAlphabeticallyAction() {
            super("Sort Columns Alphabetically", null, Icons.ACTION_SORT_ALPHA);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            Project project = getModel().getDataset().getProject();
            ColumnSortingType columnSortingType = DatasetEditorManager.getInstance(project).getRecordViewColumnSortingType();
            return columnSortingType == ColumnSortingType.ALPHABETICAL;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean selected) {
            ColumnSortingType columnSortingType = selected ? ColumnSortingType.ALPHABETICAL : ColumnSortingType.BY_INDEX;
            Project project = getModel().getDataset().getProject();
            DatasetEditorManager.getInstance(project).setRecordViewColumnSortingType(columnSortingType);
            sortColumns(columnSortingType);
        }
    }

    private class FirstRecordAction extends AnAction {
        private FirstRecordAction() {
            super("First Record", null, Icons.DATA_EDITOR_FIRST_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DatasetEditorModelRow firstRow = getModel().getRowAtIndex(0);
            if (firstRow != null) {
                setRow(firstRow);
                getModel().getEditorTable().selectRow(0);
            }
        }

        @Override
        public void update(AnActionEvent anactionevent) {
            anactionevent.getPresentation().setEnabled(geRowIndex() > 0);
        }
    }

    private class PreviousRecordAction extends AnAction {
        private PreviousRecordAction() {
            super("Previous Record", null, Icons.DATA_EDITOR_PREVIOUS_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (geRowIndex() > 0) {
                int index = geRowIndex() - 1;
                DatasetEditorModelRow previousRow = getModel().getRowAtIndex(index);
                if (previousRow != null) {
                    setRow(previousRow);
                    getModel().getEditorTable().selectRow(index);
                }
            }
        }

        @Override
        public void update(AnActionEvent anactionevent) {
            anactionevent.getPresentation().setEnabled(geRowIndex() > 0);
        }
    }

    private class NextRecordAction extends AnAction {
        private NextRecordAction() {
            super("Next Record", null, Icons.DATA_EDITOR_NEXT_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (geRowIndex() < getModel().getRowCount() -1) {
                int index = geRowIndex() + 1;
                DatasetEditorModelRow nextRow = getModel().getRowAtIndex(index);
                if (nextRow != null) {
                    setRow(nextRow);
                    getModel().getEditorTable().selectRow(index);
                }
            }
        }

        @Override
        public void update(AnActionEvent anactionevent) {
            anactionevent.getPresentation().setEnabled(geRowIndex() < getModel().getRowCount() -1);
        }
    }

    private class LastRecordAction extends AnAction {
        private LastRecordAction() {
            super("Last Record", null, Icons.DATA_EDITOR_LAST_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            int index = getModel().getRowCount() - 1 ;
            DatasetEditorModelRow lastRow = getModel().getRowAtIndex(index);
            if (lastRow != null) {
                setRow(lastRow);
                getModel().getEditorTable().selectRow(index);
            }
        }

        @Override
        public void update(AnActionEvent anactionevent) {
            anactionevent.getPresentation().setEnabled(geRowIndex() < getModel().getRowCount() -1);
        }
    }

}
