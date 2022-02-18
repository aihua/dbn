package com.dci.intellij.dbn.editor.data.model;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.connection.Savepoints;
import com.dci.intellij.dbn.connection.jdbc.DBNResultSet;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.value.ValueAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

public class EditableResultSetAdapter extends ResultSetAdapter {
    private DBNResultSet resultSet;

    public EditableResultSetAdapter(DatasetEditorModel model, DBNResultSet resultSet) {
        super(model);
        this.resultSet = resultSet;
    }

    @Override
    public void scroll(int rowIndex) throws SQLException {
        if (!isInsertMode()) {
            DBNResultSet resultSet = getResultSet();
            if (isUseSavePoints()) {
                Savepoints.run(resultSet, () -> absolute(resultSet, rowIndex));
            } else {
                absolute(resultSet, rowIndex);
            }
        }
    }

    @Override
    public void updateRow() throws SQLException {
        if (!isInsertMode())  {
            DBNResultSet resultSet = getResultSet();
            if (isUseSavePoints()) {
                Savepoints.run(resultSet, () -> updateRow(resultSet));
            } else {
                updateRow(resultSet);
            }
        }
    }

    @Override
    public void refreshRow() throws SQLException {
        if (!isInsertMode())  {
            DBNResultSet resultSet = getResultSet();
            if (isUseSavePoints()) {
                Savepoints.run(resultSet, () -> refreshRow(resultSet));
            } else {
                refreshRow(resultSet);
            }
        }
    }

    @Override
    public void startInsertRow() throws SQLException {
        if (!isInsertMode())  {
            DBNResultSet resultSet = getResultSet();
            if (isUseSavePoints()) {
                Savepoints.run(resultSet, () -> {
                    moveToInsertRow(resultSet);
                    setInsertMode(true);
                });
            } else {
                moveToInsertRow(resultSet);
                setInsertMode(true);
            }
        }
    }

    @Override
    public void cancelInsertRow() throws SQLException {
        if (isInsertMode())  {
            DBNResultSet resultSet = getResultSet();
            if (isUseSavePoints()) {
                Savepoints.run(resultSet, () -> {
                    moveToCurrentRow(resultSet);
                    setInsertMode(false);
                });
            } else {
                moveToCurrentRow(resultSet);
                setInsertMode(false);
            }
        }
    }

    @Override
    public void insertRow() throws SQLException {
        if (isInsertMode())  {
            DBNResultSet resultSet = getResultSet();
            if (isUseSavePoints()) {
                Savepoints.run(resultSet, () -> {
                    insertRow(resultSet);
                    moveToCurrentRow(resultSet);
                    setInsertMode(false);
                });
            } else {
                insertRow(resultSet);
                moveToCurrentRow(resultSet);
                setInsertMode(false);
            }
        }
    }

    @Override
    public void deleteRow() throws SQLException {
        if (!isInsertMode())  {
            DBNResultSet resultSet = getResultSet();
            if (isUseSavePoints()) {
                Savepoints.run(resultSet, () -> deleteRow(resultSet));
            } else {
                deleteRow(resultSet);
            }
        }
    }

    @Override
    public void setValue(int columnIndex, @NotNull ValueAdapter valueAdapter, @Nullable Object value) throws SQLException {
        DBNResultSet resultSet = getResultSet();
        Connection connection = resultSet.getConnection();
        if (isUseSavePoints()) {
            Savepoints.run(resultSet,
                    () -> valueAdapter.write(connection, resultSet, columnIndex, value));
        } else {
            valueAdapter.write(connection, resultSet, columnIndex, value);
        }
    }

    @Override
    public void setValue(int columnIndex, @NotNull DBDataType dataType, @Nullable Object value) throws SQLException {
        DBNResultSet resultSet = getResultSet();
        if (isUseSavePoints()) {
            Savepoints.run(resultSet,
                    () -> dataType.setValueToResultSet(resultSet, columnIndex, value));
        } else {
            dataType.setValueToResultSet(resultSet, columnIndex, value);
        }
    }

    @NotNull
    DBNResultSet getResultSet() {
        return Failsafe.nn(resultSet);
    }

    @Override
    protected void disposeInner() {
        resultSet = null;
        super.disposeInner();
    }
}
