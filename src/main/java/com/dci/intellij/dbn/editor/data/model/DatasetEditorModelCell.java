package com.dci.intellij.dbn.editor.data.model;


import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.data.model.resultSet.ResultSetDataModelCell;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.type.GenericDataType;
import com.dci.intellij.dbn.data.value.ValueAdapter;
import com.dci.intellij.dbn.editor.data.DatasetEditorError;
import com.dci.intellij.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dci.intellij.dbn.editor.data.ui.table.cell.DatasetTableCellEditor;
import com.dci.intellij.dbn.object.DBColumn;
import com.dci.intellij.dbn.object.DBDataset;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static com.dci.intellij.dbn.editor.data.model.RecordStatus.*;

@Getter
public class DatasetEditorModelCell
        extends ResultSetDataModelCell<DatasetEditorModelRow, DatasetEditorModel>
        implements ChangeListener {

    private Object originalUserValue;
    private String temporaryUserValue;
    private DatasetEditorError error;

    public DatasetEditorModelCell(DatasetEditorModelRow row, ResultSet resultSet, DatasetEditorColumnInfo columnInfo) throws SQLException {
        super(row, resultSet, columnInfo);
        originalUserValue = getUserValue();
    }

    @Override
    public DatasetEditorColumnInfo getColumnInfo() {
        return (DatasetEditorColumnInfo) super.getColumnInfo();
    }

    @Override
    public void updateUserValue(Object newUserValue, boolean bulk) {
        try {
            set(UPDATING, true);
            updateValue(newUserValue, bulk);
        } finally {
            setTemporaryUserValue(null);
            set(UPDATING, false);
        }
    }

    private void updateValue(Object newUserValue, boolean bulk) {
        getConnection().updateLastAccess();

        boolean valueChanged = userValueChanged(newUserValue);
        if (hasError() || valueChanged) {
            DatasetEditorColumnInfo columnInfo = getColumnInfo();
            GenericDataType genericDataType = columnInfo.getDataType().getGenericDataType();
            boolean isValueAdapter = ValueAdapter.supports(genericDataType);

            if (!isValueAdapter && valueChanged) {
                setUserValue(newUserValue);
            }

            DatasetEditorModelRow row = getRow();
            ResultSetAdapter resultSetAdapter = getModel().getResultSetAdapter();
            try {
                resultSetAdapter.scroll(row.getResultSetRowIndex());
            } catch (Exception e) {
                Messages.showErrorDialog(getProject(), "Could not update cell value for " + columnInfo.getName() + ".", e);
                return;
            }

            ConnectionHandler connection = getConnectionHandler();
            try {
                clearError();
                int columnIndex = columnInfo.getResultSetColumnIndex();
                if (isValueAdapter && userValue == null) {
                    userValue = ValueAdapter.create(genericDataType);
                }

                if (isValueAdapter) {
                    ValueAdapter<?> valueAdapter = (ValueAdapter<?>) userValue;
                    if (valueAdapter != null) {
                        if (newUserValue instanceof ValueAdapter) {
                            ValueAdapter<?> newValueAdapter = (ValueAdapter<?>) newUserValue;
                            newUserValue = newValueAdapter.read();
                        }
                        resultSetAdapter.setValue(columnIndex, valueAdapter, newUserValue);
                    }
                } else {
                    DBDataType dataType = columnInfo.getDataType();
                    resultSetAdapter.setValue(columnIndex, dataType, newUserValue);
                }


                resultSetAdapter.updateRow();
            } catch (Exception e) {
                //try { Thread.sleep(6000); } catch (InterruptedException e1) { e1.printStackTrace(); }

                DatasetEditorError error = new DatasetEditorError(connection, e);

                // error may affect other cells in the row (e.g. foreign key constraint for multiple primary key)
                if (e instanceof SQLException) {
                    row.notifyError(error, false, !bulk);
                }

                // if error was not notified yet on row level, notify it on cell isolation level
                if (!error.isNotified()) notifyError(error, !bulk);
            } finally {
                if (valueChanged) {
                    DBNConnection conn = getModel().getConnection();
                    conn.notifyDataChanges(getDataset().getVirtualFile());
                    ProjectEvents.notify(getProject(),
                            DatasetEditorModelCellValueListener.TOPIC,
                            (listener) -> listener.valueChanged(this));
                }
                try {
                    resultSetAdapter.refreshRow();
                } catch (SQLException e) {
                    DatasetEditorError error = new DatasetEditorError(connection, e);
                    row.notifyError(error, false, !bulk);
                }
            }

            if (row.isNot(INSERTING) && !connection.isAutoCommit()) {
                reset();
                set(MODIFIED, true);

                row.reset();
                row.set(RecordStatus.MODIFIED, true);
                row.getModel().set(MODIFIED, true);
            }
        }
    }

    protected DBDataset getDataset() {
        return getEditorModel().getDataset();
    }

    private boolean userValueChanged(Object newUserValue) {
        if (userValue instanceof ValueAdapter) {
            ValueAdapter<?> valueAdapter = (ValueAdapter<?>) userValue;
            try {
                return !Commons.match(valueAdapter.read(), newUserValue);
            } catch (SQLException e) {
                return true;
            }
        }

        if (userValue != null && newUserValue != null) {
            if (userValue.equals(newUserValue)) {
                return false;
            }
            // user input may not contain the entire precision (e.g. date time format)
            Formatter formatter = getFormatter();
            String formattedValue1 = formatter.formatObject(userValue);
            String formattedValue2 = formatter.formatObject(newUserValue);
            return !Objects.equals(formattedValue1, formattedValue2);
        }
        
        return !Commons.match(userValue, newUserValue);
    }

    public void updateUserValue(Object userValue, String errorMessage) {
        getConnection().updateLastAccess();

        if (!Commons.match(userValue, getUserValue()) || hasError()) {
            DatasetEditorModelRow row = getRow();
            DatasetEditorError error = new DatasetEditorError(errorMessage, getColumnInfo().getColumn());
            getRow().notifyError(error, true, true);
            setUserValue(userValue);
            ConnectionHandler connection = getConnectionHandler();
            if (row.isNot(INSERTING) && !connection.isAutoCommit()) {
                reset();
                set(MODIFIED, true);

                row.reset();
                row.set(MODIFIED, true);
                row.getModel().set(MODIFIED, true);
            }
        }
    }

    public boolean matches(DatasetEditorModelCell remoteCell, boolean lenient) {
        if (Commons.match(getUserValue(), remoteCell.getUserValue())){
            return true;
        }
        if (lenient && (getRow().is(INSERTED) || getRow().is(MODIFIED)) && getUserValue() == null && remoteCell.getUserValue() != null) {
            return true;
        }
        return false;
    }

    public ConnectionHandler getConnectionHandler() {
        return getEditorModel().getConnectionHandler();
    }

    private DatasetEditorTable getEditorTable() {
        return getEditorModel().getEditorTable();
    }

    @NotNull
    private DatasetEditorModel getEditorModel() {
        return getRow().getModel();
    }

    public void edit() {
        Dispatch.run(true, () -> {
            int index = getEditorTable().convertColumnIndexToView(getIndex());
            if (index > 0) {
                DatasetEditorTable table = getEditorTable();
                table.editCellAt(getRow().getIndex(), index);
            }
        });
    }

    public void editPrevious() {
        Dispatch.run(true, () -> {
            int index = getEditorTable().convertColumnIndexToView(getIndex());
            if (index > 0) {
                DatasetEditorTable table = getEditorTable();
                DatasetEditorModelRow row = getRow();
                table.editCellAt(row.getIndex(), index -1);
            }
        });
    }

    public void editNext(){
        Dispatch.run(true, () -> {
            int index = getEditorTable().convertColumnIndexToView(getIndex());
            DatasetEditorModelRow row = getRow();
            if (index < row.getCells().size()-1) {
                DatasetEditorTable table = getEditorTable();
                table.editCellAt(row.getIndex(), index + 1);
            }
        });
    }

    @Override
    @NotNull
    public DatasetEditorModelRow getRow() {
        return super.getRow();
    }

    @NotNull
    @Override
    public DatasetEditorModel getModel() {
        return super.getModel();
    }

    void setOriginalUserValue(Object value) {
        if (originalUserValue == null) {
            set(MODIFIED, value != null);
        } else {
            set(MODIFIED, !originalUserValue.equals(value));
        }
        this.originalUserValue = value;
    }

    public void setTemporaryUserValue(String temporaryUserValue) {
        this.temporaryUserValue = temporaryUserValue;
    }

    public boolean isEditing() {
        DatasetEditorTable table = getEditorTable();
        return table.isEditing() &&
               table.isCellSelected(getRow().getIndex(), getIndex());
    }

    public boolean isNavigable() {
        DBColumn column = getColumnInfo().getColumn();
        return column.isForeignKey() && getUserValue() != null;
    }

    private void notifyCellUpdated() {
        getEditorModel().notifyCellUpdated(getRow().getIndex(), getIndex());
    }

    private void scrollToVisible() {
        Dispatch.run(() -> {
            int rowIndex = getRow().getIndex();
            int colIndex = getIndex();
            DatasetEditorTable table = getEditorTable();
            Rectangle cellRect = table.getCellRect(rowIndex, colIndex, true);
            table.scrollRectToVisible(cellRect);
        });
    }

    public boolean isResultSetUpdatable() {
        return getRow().isResultSetUpdatable();
    }

    /*********************************************************
     *                    ChangeListener                     *
     *********************************************************/
    @Override
    public void stateChanged(ChangeEvent e) {
        notifyCellUpdated();
    }


    /*********************************************************
     *                        ERROR                          *
     *********************************************************/
    public boolean hasError() {
        if (error != null && error.isDirty()) {
            error = null;
        }
        return error != null;
    }

    boolean notifyError(DatasetEditorError error, boolean showPopup) {
        error.setNotified(true);
        if(!Commons.match(this.error, error, err -> err.getMessage())) {
            clearError();
            this.error = error;
            notifyCellUpdated();
            if (showPopup) {
                scrollToVisible();
            }

            DatasetEditorTable table = getEditorTable();
            if (isEditing()) {
                DatasetTableCellEditor cellEditor = table.getCellEditor();
                if (cellEditor != null) {
                    cellEditor.highlight(DatasetTableCellEditor.HIGHLIGHT_TYPE_ERROR);
                }
            }
            error.addChangeListener(this);
            if (showPopup) {
                table.showErrorPopup(this);
            }
            return true;
        }
        return false;
    }

    private void clearError() {
        if (error != null ) {
            error.markDirty();
            error = null;
        }
    }

    public void revertChanges() {
        if (is(MODIFIED)) {
            updateUserValue(originalUserValue, false);
            set(MODIFIED, false);
        }
    }
}
