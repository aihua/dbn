package com.dci.intellij.dbn.data.record.ui;

import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.ui.util.Mouse;
import com.dci.intellij.dbn.data.record.DatasetRecord;
import com.dci.intellij.dbn.editor.data.DatasetEditorManager;
import com.dci.intellij.dbn.editor.data.filter.DatasetFilterInput;
import com.dci.intellij.dbn.object.DBColumn;
import com.dci.intellij.dbn.object.DBConstraint;
import com.dci.intellij.dbn.object.DBDataset;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static com.dci.intellij.dbn.common.util.Commons.nvl;
import static com.dci.intellij.dbn.common.util.TextAttributes.getSimpleTextAttributes;
import static com.dci.intellij.dbn.data.grid.color.DataGridTextAttributesKeys.FOREIGN_KEY;
import static com.dci.intellij.dbn.data.grid.color.DataGridTextAttributesKeys.PRIMARY_KEY;

@Getter
class ColumnValueTextField extends JTextField {
    private final DatasetRecord record;
    private final DBObjectRef<DBColumn> column;

    ColumnValueTextField(DatasetRecord record, DBColumn column) {
        this.record = record;
        this.column = DBObjectRef.of(column);
        SimpleTextAttributes textAttributes =
                column.isPrimaryKey() ? getSimpleTextAttributes(PRIMARY_KEY) :
                column.isForeignKey() ? getSimpleTextAttributes(FOREIGN_KEY) : null;

        if (textAttributes != null) {
            setForeground(textAttributes.getFgColor());
            Color background = textAttributes.getBgColor();
            if (background != null) {
                setBackground(background);
            }
        }

        if (column.isForeignKey()) {
            addMouseListener(mouseListener);
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        DBColumn column = getColumn();
        if (column != null && column.isForeignKey()) {
            if (e.isControlDown() && e.getID() != MouseEvent.MOUSE_DRAGGED && record.getColumnValue(column) != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                DBColumn foreignKeyColumn = column.getForeignKeyColumn();
                if (foreignKeyColumn != null) {
                    setToolTipText("<html>Show referenced <b>" + foreignKeyColumn.getDataset().getQualifiedName() + "</b> record<html>");
                }
            } else {
                super.processMouseMotionEvent(e);
                setCursor(Cursor.getDefaultCursor());
                setToolTipText(null);
            }
        } else {
            super.processMouseMotionEvent(e);
        }
    }
    
    @Nullable
    private DatasetFilterInput resolveForeignKeyRecord() {
        DBColumn column = getColumn();
        if (column == null) return null;

        for (DBConstraint constraint : column.getConstraints()) {
            constraint = constraint.getUndisposedEntity();
            if (constraint == null || !constraint.isForeignKey()) continue;

            DBConstraint fkConstraint = constraint.getForeignKeyConstraint();
            if (fkConstraint == null) continue;

            DBDataset fkDataset = fkConstraint.getDataset();
            DatasetFilterInput filterInput = null;

            for (DBColumn constraintColumn : constraint.getColumns()) {
                constraintColumn = constraintColumn.getUndisposedEntity();
                if (constraintColumn != null) {
                    DBColumn foreignKeyColumn = constraintColumn.getForeignKeyColumn();
                    if (foreignKeyColumn != null) {
                        Object value = record.getColumnValue(constraintColumn);
                        filterInput = nvl(filterInput, () -> new DatasetFilterInput(fkDataset));
                        filterInput.setColumnValue(foreignKeyColumn, value);
                    }
                }
            }
            return filterInput;
        }

        return null;
    }

    private DBColumn getColumn() {
        return DBObjectRef.get(column);
    }

    private final MouseListener mouseListener = Mouse.listener().onClick(e -> {
        DBColumn column = getColumn();
        if (column == null || !Mouse.isNavigationEvent(e)) return;
        if (!column.isForeignKey()) return;
        if (getRecord().getColumnValue(column) == null) return;


        Project project = column.getProject();
        Progress.prompt(project, column, true,
                "Opening record details",
                "Opening record details for " + column.getQualifiedNameWithType(),
                progress -> {
                    DatasetFilterInput filterInput = resolveForeignKeyRecord();
                    if (filterInput != null && filterInput.getColumns().size() > 0) {
                        Dispatch.run(() -> {
                            DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(project);
                            datasetEditorManager.navigateToRecord(filterInput, e);
                        });
                    }
                });
        e.consume();
    });
}
