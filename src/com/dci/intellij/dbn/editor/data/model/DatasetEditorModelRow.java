package com.dci.intellij.dbn.editor.data.model;

import com.dci.intellij.dbn.common.property.PropertyHolder;
import com.dci.intellij.dbn.common.util.MessageUtil;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.model.ColumnInfo;
import com.dci.intellij.dbn.data.model.DataModelCell;
import com.dci.intellij.dbn.data.model.DataModelRow;
import com.dci.intellij.dbn.data.model.resultSet.ResultSetDataModelRow;
import com.dci.intellij.dbn.editor.data.DatasetEditorError;
import com.dci.intellij.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dci.intellij.dbn.object.DBColumn;
import com.dci.intellij.dbn.object.DBConstraint;
import com.dci.intellij.dbn.object.DBTable;
import com.dci.intellij.dbn.object.common.DBObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DatasetEditorModelRow
        extends ResultSetDataModelRow<DatasetEditorModel, DatasetEditorModelCell>
        implements PropertyHolder<RecordStatus>{

    public DatasetEditorModelRow(DatasetEditorModel model, ResultSet resultSet, int resultSetRowIndex) throws SQLException {
        super(model, resultSet, resultSetRowIndex);
    }

    @NotNull
    @Override
    public DatasetEditorModel getModel() {
        return super.getModel();
    }

    @Nullable
    DatasetEditorModelCell getCellForColumn(DBColumn column) {
        int columnIndex = getModel().getHeader().indexOfColumn(column);
        return getCellAtIndex(columnIndex);
    }

    @NotNull
    @Override
    protected DatasetEditorModelCell createCell(ResultSet resultSet, ColumnInfo columnInfo) throws SQLException {
        return new DatasetEditorModelCell(this, resultSet, (DatasetEditorColumnInfo) columnInfo);
    }

    void updateStatusFromRow(DatasetEditorModelRow oldRow) {
        if (oldRow != null) {
            replace(oldRow);
            setIndex(oldRow.getIndex());
            if (oldRow.is(RecordStatus.MODIFIED)) {
                for (int i=1; i<getCells().size(); i++) {
                    DatasetEditorModelCell oldCell = oldRow.getCellAtIndex(i);
                    DatasetEditorModelCell newCell = getCellAtIndex(i);
                    if (oldCell != null && newCell != null) {
                        newCell.setOriginalUserValue(oldCell.getOriginalUserValue());
                    }
                }
            }
        }
    }

    void updateDataFromRow(DataModelRow oldRow) {
        for (int i=0; i<getCells().size(); i++) {
            DataModelCell oldCell = oldRow.getCellAtIndex(i);
            DatasetEditorModelCell newCell = getCellAtIndex(i);
            if (oldCell != null && newCell != null) {
                newCell.updateUserValue(oldCell.getUserValue(), false);
            }
        }
    }

    public void delete() {
        try {
            ResultSetAdapter resultSetAdapter = getModel().getResultSetAdapter();
            resultSetAdapter.scroll(getResultSetRowIndex());
            resultSetAdapter.deleteRow();

            reset();
            set(RecordStatus.DELETED, true);
        } catch (SQLException e) {
            MessageUtil.showErrorDialog(getProject(), "Could not delete row at index " + getIndex() + '.', e);
        }
    }

    public boolean matches(DataModelRow row, boolean lenient) {
        // try fast match by primary key
        DatasetEditorModel model = getModel();
        if (model.getDataset() instanceof DBTable) {
            DBTable table = (DBTable) model.getDataset();
            List<DBColumn> uniqueColumns = table.getPrimaryKeyColumns();
            if (uniqueColumns.size() == 0) {
                uniqueColumns = table.getUniqueKeyColumns();
            }
            if (uniqueColumns.size() > 0) {
                for (DBColumn uniqueColumn : uniqueColumns) {
                    int index = model.getHeader().indexOfColumn(uniqueColumn);
                    DatasetEditorModelCell localCell = getCellAtIndex(index);
                    DatasetEditorModelCell remoteCell = (DatasetEditorModelCell) row.getCellAtIndex(index);
                    if (localCell != null && remoteCell != null) {
                        if (!localCell.matches(remoteCell, false)) return false;
                    }
                }
                return true;
            }
        }

        // try to match all columns
        for (int i=0; i<getCells().size(); i++) {
            DatasetEditorModelCell localCell = getCellAtIndex(i);
            DatasetEditorModelCell remoteCell = (DatasetEditorModelCell) row.getCellAtIndex(i);

            //local cell is usually the cell on client side.
            // remote cell may have been changed by a trigger on update/insert
            /*if (!localCell.equals(remoteCell) && (localCell.getUserValue()!= null || !ignoreNulls)) {
                return false;
            }*/
            if (localCell != null && remoteCell != null && !localCell.matches(remoteCell, lenient)) {
                return false;
            }
        }
        return true;
    }

    public void notifyError(DatasetEditorError error, boolean startEditing, boolean showPopup) {
        checkDisposed();

        DBObject messageObject = error.getMessageObject();
        if (messageObject != null) {
            if (messageObject instanceof DBColumn) {
                DBColumn column = (DBColumn) messageObject;
                DatasetEditorModelCell cell = getCellForColumn(column);
                if (cell != null) {
                    boolean isErrorNew = cell.notifyError(error, true);
                    if (isErrorNew && startEditing) cell.edit();
                }
            } else if (messageObject instanceof DBConstraint) {
                DBConstraint constraint = (DBConstraint) messageObject;
                DatasetEditorModelCell firstCell = null;
                boolean isErrorNew = false;
                for (DBColumn column : constraint.getColumns()) {
                    DatasetEditorModelCell cell = getCellForColumn(column);
                    if (cell != null) {
                        isErrorNew = cell.notifyError(error, false);
                        if (firstCell == null) firstCell = cell;
                    }
                }
                if (isErrorNew && showPopup) {
                    DatasetEditorTable table = getModel().getEditorTable();
                    table.showErrorPopup(firstCell);
                    error.setNotified(true);
                }
            }
        }
    }

    public void revertChanges() {
        if (is(RecordStatus.MODIFIED)) {
            for (DatasetEditorModelCell cell : getCells()) {
                cell.revertChanges();
            }
        }
    }


    @Override
    public int getResultSetRowIndex() {
        return is(RecordStatus.DELETED) ? -1 : super.getResultSetRowIndex();
    }

    @Override
    public void shiftResultSetRowIndex(int delta) {
        assert isNot(RecordStatus.DELETED);
        super.shiftResultSetRowIndex(delta);
    }

    @NotNull
    ConnectionHandler getConnectionHandler() {
        return getModel().getConnectionHandler();
    }

    public boolean isResultSetUpdatable() {
        return getModel().isResultSetUpdatable();
    }

    public boolean isEmptyData() {
        for (DatasetEditorModelCell cell : getCells()) {
            Object userValue = cell.getUserValue();
            if (userValue != null) {
                if (userValue instanceof String) {
                    String stringUserValue = (String) userValue;
                    if (StringUtil.isNotEmpty(stringUserValue)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
