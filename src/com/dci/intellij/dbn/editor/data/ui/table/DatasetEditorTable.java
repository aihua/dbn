package com.dci.intellij.dbn.editor.data.ui.table;

import com.dci.intellij.dbn.common.Pair;
import com.dci.intellij.dbn.common.property.PropertyHolder;
import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.ui.form.DBNForm;
import com.dci.intellij.dbn.common.ui.table.DBNTableGutter;
import com.dci.intellij.dbn.common.ui.util.Mouse;
import com.dci.intellij.dbn.common.ui.util.UserInterface;
import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.data.grid.options.DataGridAuditColumnSettings;
import com.dci.intellij.dbn.data.grid.ui.table.basic.BasicTableCellRenderer;
import com.dci.intellij.dbn.data.grid.ui.table.basic.BasicTableGutter;
import com.dci.intellij.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dci.intellij.dbn.data.model.ColumnInfo;
import com.dci.intellij.dbn.data.model.DataModelCell;
import com.dci.intellij.dbn.data.preview.LargeValuePreviewPopup;
import com.dci.intellij.dbn.data.record.RecordViewInfo;
import com.dci.intellij.dbn.data.sorting.SortDirection;
import com.dci.intellij.dbn.data.value.ArrayValue;
import com.dci.intellij.dbn.data.value.LargeObjectValue;
import com.dci.intellij.dbn.data.value.ValueAdapter;
import com.dci.intellij.dbn.editor.EditorProviderId;
import com.dci.intellij.dbn.editor.data.DatasetEditor;
import com.dci.intellij.dbn.editor.data.DatasetLoadInstructions;
import com.dci.intellij.dbn.editor.data.action.DatasetEditorTableActionGroup;
import com.dci.intellij.dbn.editor.data.model.DatasetEditorModel;
import com.dci.intellij.dbn.editor.data.model.DatasetEditorModelCell;
import com.dci.intellij.dbn.editor.data.model.RecordStatus;
import com.dci.intellij.dbn.editor.data.options.DataEditorGeneralSettings;
import com.dci.intellij.dbn.editor.data.ui.DatasetEditorErrorForm;
import com.dci.intellij.dbn.editor.data.ui.table.cell.DatasetTableCellEditor;
import com.dci.intellij.dbn.editor.data.ui.table.cell.DatasetTableCellEditorFactory;
import com.dci.intellij.dbn.editor.data.ui.table.listener.DatasetEditorHeaderMouseListener;
import com.dci.intellij.dbn.editor.data.ui.table.listener.DatasetEditorKeyListener;
import com.dci.intellij.dbn.editor.data.ui.table.listener.DatasetEditorMouseListener;
import com.dci.intellij.dbn.editor.data.ui.table.renderer.DatasetEditorTableCellRenderer;
import com.dci.intellij.dbn.editor.data.ui.table.renderer.DatasetEditorTableHeaderRenderer;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.dci.intellij.dbn.object.DBColumn;
import com.dci.intellij.dbn.object.DBDataset;
import com.dci.intellij.dbn.vfs.DatabaseFileSystem;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.awt.RelativePoint;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.EventObject;

import static com.dci.intellij.dbn.editor.data.DatasetLoadInstruction.*;
import static com.dci.intellij.dbn.editor.data.model.RecordStatus.*;

public class DatasetEditorTable extends ResultSetTable<DatasetEditorModel> {
    private static final DatasetLoadInstructions SORT_LOAD_INSTRUCTIONS = new DatasetLoadInstructions(USE_CURRENT_FILTER, PRESERVE_CHANGES, DELIBERATE_ACTION);
    private final WeakRef<DatasetEditor> datasetEditor;

    private final DatasetTableCellEditorFactory cellEditorFactory = new DatasetTableCellEditorFactory();
    private final DatasetEditorMouseListener tableMouseListener = new DatasetEditorMouseListener(this);

    private @Getter @Setter boolean editingEnabled = true;

    public DatasetEditorTable(DBNForm parent, DatasetEditor datasetEditor) throws SQLException {
        super(parent, createModel(datasetEditor), false,
                new RecordViewInfo(
                    datasetEditor.getDataset().getQualifiedName(),
                    datasetEditor.getDataset().getIcon()));
        JTableHeader tableHeader = getTableHeader();
        tableHeader.setDefaultRenderer(new DatasetEditorTableHeaderRenderer());
        setName(datasetEditor.getDataset().getName());
        this.datasetEditor = WeakRef.of(datasetEditor);

        getSelectionModel().addListSelectionListener(getModel());
        addKeyListener(new DatasetEditorKeyListener(this));
        addMouseListener(tableMouseListener);

        tableHeader.addMouseListener(new DatasetEditorHeaderMouseListener(this));

        Disposer.register(this, cellEditorFactory);
        /*
        DataProvider dataProvider = datasetEditor.getDataProvider();
        ActionUtil.registerDataProvider(this, dataProvider, false);
        ActionUtil.registerDataProvider(getTableHeader(), dataProvider, false);
*/
    }

    @Override
    protected BasicTableCellRenderer createCellRenderer() {
        return new DatasetEditorTableCellRenderer();
    }

    private static DatasetEditorModel createModel(DatasetEditor datasetEditor) throws SQLException {
        return new DatasetEditorModel(datasetEditor);
    }

    @NotNull
    public DBDataset getDataset() {
        return getModel().getDataset();
    }

    @Override
    protected BasicTableGutter<?> createTableGutter() {
        return new DatasetEditorTableGutter(this);
    }

    public boolean isInserting() {
        return getModel().is(INSERTING);
    }

    public void hideColumn(int columnIndex) {
        TableColumnModel columnModel = getColumnModel();
        int viewColumnIndex = convertColumnIndexToView(columnIndex);
        TableColumn column = columnModel.getColumn(viewColumnIndex);
        columnModel.removeColumn(column);

        ColumnInfo columnInfo = getModel().getColumnInfo(columnIndex);
        getDatasetEditor().getColumnSetup().getColumnState(columnInfo.getName()).setVisible(false);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        return getCellRenderer();
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        int fromIndex = e.getFromIndex();
        int toIndex = e.getToIndex();
        if (fromIndex != toIndex) {
            getDatasetEditor().getColumnSetup().moveColumn(fromIndex, toIndex);
        }
        super.columnMoved(e);
    }

    @Override
    public void moveColumn(int column, int targetColumn) {
        super.moveColumn(column, targetColumn);
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        DatasetTableCellEditor cellEditor = getCellEditor();
        if (cellEditor != null && cellEditor.isEditable()) {
            int rowIndex = editingRow;
            int columnIndex = editingColumn;

            DatasetEditorModelCell cell = cellEditor.getCell();
            if (cell != null) {
                String editorTextValue = cellEditor.getCellEditorTextValue();
                Pair<Object, Throwable> result = Pair.create();
                try {
                    result.first(cellEditor.getCellEditorValue());
                } catch (Throwable t) {
                    result.first(editorTextValue);
                    result.second(t);
                }
                removeEditor();

                performUpdate(rowIndex, columnIndex, () -> {
                    cell.setTemporaryUserValue(editorTextValue);
                    Throwable exception = result.second();
                    Object value = result.first();
                    if (exception == null) {
                        setValueAt(value, rowIndex, columnIndex);
                    } else {
                        setValueAt(value, exception.getMessage(), rowIndex, columnIndex);
                    }
                });
            }
        }
    }

    public void performUpdate(int rowIndex, int columnIndex, Runnable runnable) {
        PropertyHolder<RecordStatus> scope = getUpdateScope(rowIndex, columnIndex);
        if (scope != null) {
            scope.set(UPDATING, true);
            Background.run(() -> {
                try {
                    runnable.run();
                } finally {
                    scope.set(UPDATING, false);
                    Dispatch.run(() -> {
                        DBNTableGutter tableGutter = getTableGutter();
                        UserInterface.repaint(tableGutter);
                        UserInterface.repaint(DatasetEditorTable.this);
                    });
                }
            });
        }
    }

    @Nullable
    private PropertyHolder<RecordStatus> getUpdateScope(int rowIndex, int columnIndex) {
        DatasetEditorModel model = getModel();
        if (rowIndex != -1 && columnIndex != -1) {
            return model.getCellAt(rowIndex, columnIndex);
        } else if (rowIndex > -1) {
            return model.getRowAtIndex(rowIndex);
        }
        return model;
    }

    public void showErrorPopup(@NotNull DatasetEditorModelCell cell) {
        Dispatch.run(() -> {
            checkDisposed();

            if (!isShowing()) {
                DBDataset dataset = getDataset();
                DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
                databaseFileSystem.connectAndOpenEditor(dataset, EditorProviderId.DATA, false, true);
            }
            if (cell.getError() != null) {
                DatasetEditorErrorForm errorForm = new DatasetEditorErrorForm(cell);
                errorForm.show();
            }
        });
    }

    @Override
    public DatasetTableCellEditor getCellEditor() {
        TableCellEditor cellEditor = super.getCellEditor();
        return cellEditor instanceof DatasetTableCellEditor ? (DatasetTableCellEditor) cellEditor : null;
    }

    @Override
    public void clearSelection() {
        Dispatch.run(true, () -> DatasetEditorTable.super.clearSelection());
    }

    @Override
    public void removeEditor() {
        Dispatch.run(true, () -> DatasetEditorTable.super.removeEditor());
    }

    public void updateTableGutter() {
        Dispatch.run(true, () -> {
            DBNTableGutter tableGutter = getTableGutter();
            UserInterface.repaint(tableGutter);
        });
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        int modelRowIndex = rowIndex;//convertRowIndexToModel(rowIndex);
        int modelColumnIndex = convertColumnIndexToModel(columnIndex);
        if (modelRowIndex > -1 && modelColumnIndex > -1) {
            getModel().setValueAt(value, modelRowIndex, modelColumnIndex);
        }
    }

    public void setValueAt(Object value, String errorMessage, int rowIndex, int columnIndex) {
        int modelRowIndex = rowIndex;//convertRowIndexToModel(rowIndex);
        int modelColumnIndex = convertColumnIndexToModel(columnIndex);
        if (modelRowIndex > -1 && modelColumnIndex > -1) {
            getModel().setValueAt(value, errorMessage, modelRowIndex, modelColumnIndex);
        }
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int rowIndex, int columnIndex) {
        Component component = super.prepareEditor(editor, rowIndex, columnIndex);
        selectCell(rowIndex, columnIndex);

        if (editor instanceof DatasetTableCellEditor) {
            DatasetTableCellEditor cellEditor = (DatasetTableCellEditor) editor;
            DatasetEditorModelCell cell = (DatasetEditorModelCell) getCellAtPosition(rowIndex, columnIndex);
            if (cell != null) {
                cellEditor.prepareEditor(cell);
            }
        }
        return component;
    }

    @Override
    public boolean editCellAt(final int row, final int column, final EventObject e) {
        return super.editCellAt(row, column, e);
    }

    @Override
    public TableCellEditor getCellEditor(int rowIndex, int columnIndex) {
        if (isLoading()) {
            return null;
        }

        int modelColumnIndex = getModelColumnIndex(columnIndex);
        ColumnInfo columnInfo = getModel().getColumnInfo(modelColumnIndex);

        DataGridAuditColumnSettings auditColumnSettings = getDataGridSettings().getAuditColumnSettings();
        if (!auditColumnSettings.isAllowEditing()) {
            boolean auditColumn = auditColumnSettings.isAuditColumn(columnInfo.getName());
            if (auditColumn) return null;
        }

        return cellEditorFactory.getCellEditor(columnInfo, this);
    }

    @Override
    public TableCellEditor getDefaultEditor(Class<?> columnClass) {
        return super.getDefaultEditor(columnClass);
    }

    @Override
    protected void initLargeValuePopup(LargeValuePreviewPopup viewer) {
        super.initLargeValuePopup(viewer);
    }

    @Override
    protected boolean isLargeValuePopupActive() {
        DataEditorGeneralSettings generalSettings = getDatasetEditor().getSettings().getGeneralSettings();
        return generalSettings.getLargeValuePreviewActive().value();
    }

    @Override
    public int getColumnWidthBuffer() {
        return 30;
    }

    @Override
    public String getToolTipText(@NotNull MouseEvent event) {
        DataModelCell cell = getCellAtLocation(event.getPoint());
        if (cell instanceof DatasetEditorModelCell) {
            DatasetEditorModelCell editorTableCell = (DatasetEditorModelCell) cell;
/*            if (event.isControlDown() && isNavigableCellAtMousePosition()) {
                DBColumn column = editorTableCell.getColumnInfo().getColumn();
                DBColumn foreignKeyColumn = column.getForeignKeyColumn();
                if (foreignKeyColumn != null) {
                    StringBuilder text = new StringBuilder("<html>");
                    text.append("Show ");
                    text.append(foreignKeyColumn.getDataset().getName());
                    text.append(" record");
                    text.append("</html>");
                    return text.toString();
                }
            }*/

            if (editorTableCell.hasError()) {
                StringBuilder text = new StringBuilder("<html>");

                if (editorTableCell.hasError()) {
                    text.append(editorTableCell.getError().getMessage());
                    text.append("<br>");
                }

                if (editorTableCell.is(MODIFIED) && !(editorTableCell.getUserValue() instanceof ValueAdapter)) {
                    text.append("<br>Original value: <b>");
                    text.append(editorTableCell.getOriginalUserValue());
                    text.append("</b>");
                }

                text.append("</html>");

                return text.toString();
            }

            if (editorTableCell.is(MODIFIED) && !event.isControlDown()) {
                if (editorTableCell.getUserValue() instanceof ArrayValue) {
                    return "Array value has changed";
                } else  if (editorTableCell.getUserValue() instanceof LargeObjectValue) {
                    return "LOB content has changed";
                } else {
                    return "<html>Original value: <b>" + editorTableCell.getOriginalUserValue() + "</b></html>";
                }

            }
        }
        return super.getToolTipText(event);
    }

    public void fireEditingCancel() {
        if (isEditing()) {
            Dispatch.run(true, () -> cancelEditing());
        }
    }

    public void cancelEditing() {
        if (isEditing()) {
            TableCellEditor cellEditor = getCellEditor();
            if (cellEditor != null) {
                cellEditor.cancelCellEditing();
            }
        }
    }

    @Override
    protected void regionalSettingsChanged() {
        cancelEditing();
        super.regionalSettingsChanged();
    }

    @Override
    public void sort() {
        DatasetEditorModel model = getModel();
        if (!isLoading() && !model.is(UPDATING)) {
            super.sort();
            if (!model.isResultSetExhausted()) {
                getDatasetEditor().loadData(SORT_LOAD_INSTRUCTIONS);
            }
            resizeAndRepaint();
        }
    }

    @Override
    public boolean sort(int columnIndex, SortDirection sortDirection, boolean keepExisting) {
        int modelColumnIndex = convertColumnIndexToModel(columnIndex);
        DatasetEditorModel model = getModel();
        ColumnInfo columnInfo = model.getColumnInfo(modelColumnIndex);
        if (columnInfo.isSortable()) {
            if (!isLoading() && !model.is(UPDATING)) {
                boolean sorted = super.sort(columnIndex, sortDirection, keepExisting);

                if (sorted && !model.isResultSetExhausted()) {
                    getDatasetEditor().loadData(SORT_LOAD_INSTRUCTIONS);
                }
                return sorted;
            }
        }
        return false;
    }

    @NotNull
    public DatasetEditor getDatasetEditor() {
        return datasetEditor.ensure();
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (e.isControlDown() && isNavigableCellAtMousePosition()) {
            Mouse.processMouseEvent(e, tableMouseListener);
        } else {
            super.processMouseEvent(e);
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (e.isControlDown() && e.getID() != MouseEvent.MOUSE_DRAGGED && isNavigableCellAtMousePosition()) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            DatasetEditorModelCell cell = (DatasetEditorModelCell) getCellAtMouseLocation();
            if (cell != null) {
                DBColumn column = cell.getColumnInfo().getColumn();
                DBColumn foreignKeyColumn = column.getForeignKeyColumn();
                if (foreignKeyColumn != null) {
                    setToolTipText("<html>Show referenced <b>" + foreignKeyColumn.getDataset().getQualifiedName() + "</b> record<html>");
                }
            }
        } else {
            super.processMouseMotionEvent(e);
            setCursor(Cursor.getDefaultCursor());
            setToolTipText(null);
        }
    }

    private boolean isNavigableCellAtMousePosition() {
        DatasetEditorModelCell cell = (DatasetEditorModelCell) getCellAtMouseLocation();
        return cell != null && cell.isNavigable();
    }

    /**********************************************************
     *                  ListSelectionListener                 *
     **********************************************************/
    @Override
    public void valueChanged(ListSelectionEvent e) {
        super.valueChanged(e);
        if (!e.getValueIsAdjusting()) {
            DatasetEditorModel model = getModel();
            if (model.is(INSERTING)) {
                int insertRowIndex = getModel().getInsertRowIndex();
                if (insertRowIndex != -1 && (insertRowIndex == e.getFirstIndex() || insertRowIndex == e.getLastIndex()) && getSelectedRow() != insertRowIndex) {
                    Progress.prompt(getProject(), "Refreshing data", false, progress -> {
                        try {
                            model.postInsertRecord(false, true, false);
                        } catch (SQLException e1) {
                            Messages.showErrorDialog(getProject(), "Could not create row in " + getDataset().getQualifiedNameWithType() + ".", e1);
                        }
                    });
                }
            }
            startCellEditing(e);
        }

    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        JTableHeader tableHeader = getTableHeader();
        if (tableHeader != null && tableHeader.getDraggedColumn() == null) {
            super.columnSelectionChanged(e);
            if (!e.getValueIsAdjusting()) {
                startCellEditing(e);
            }
        }
    }

    private void startCellEditing(ListSelectionEvent e) {
        if (!isEditing()) {
            int[] selectedRows = getSelectedRows();
            int[] selectedColumns = getSelectedColumns();

            if (selectedRows.length == 1 && selectedColumns.length == 1) {
                int selectedRow = selectedRows[0];
                int selectedColumn = selectedColumns[0];
                if (getModel().isCellEditable(selectedRow, selectedColumn)) {
                    editCellAt(selectedRow, selectedColumn);
                }
            }
        }
    }

    public RelativePoint getColumnHeaderLocation(DBColumn column) {
        int columnIndex = convertColumnIndexToView(getModel().getHeader().indexOfColumn(column));
        Rectangle rectangle = getTableHeader().getHeaderRect(columnIndex);
        Point point = new Point(
                (int) (rectangle.getX() + rectangle.getWidth() - 20),
                (int) (rectangle.getY() + rectangle.getHeight()) + 20);
        return new RelativePoint(getTableHeader(), point);
    }

    /********************************************************
     *                        Popup                         *
     ********************************************************/
    public void showPopupMenu(
            MouseEvent event,
            DatasetEditorModelCell cell,
            ColumnInfo columnInfo) {

        Progress.modal(getProject(), "Loading column information", true, progress -> {
            ActionGroup actionGroup = new DatasetEditorTableActionGroup(getDatasetEditor(), cell, columnInfo);
            Progress.check(progress);

            ActionPopupMenu actionPopupMenu = Actions.createActionPopupMenu(DatasetEditorTable.this, "", actionGroup);
            JPopupMenu popupMenu = actionPopupMenu.getComponent();
            Dispatch.run(() -> {
                Component component = (Component) event.getSource();
                if (component.isShowing()) {
                    int x = event.getX();
                    int y = event.getY();
                    if (x >= 0 && x < component.getWidth() && y >= 0 && y < component.getHeight()) {
                        popupMenu.show(component, x, y);
                    }
                }
            });
        });
    }
}
