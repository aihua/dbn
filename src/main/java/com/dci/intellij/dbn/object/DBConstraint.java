package com.dci.intellij.dbn.object;

import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.type.DBConstraintType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DBConstraint extends DBSchemaObject {
    DBConstraintType getConstraintType();
    boolean isPrimaryKey();
    boolean isForeignKey();
    boolean isUniqueKey();
    DBDataset getDataset();

    @Nullable
    DBConstraint getForeignKeyConstraint();

    List<DBColumn> getColumns();
    short getColumnPosition(DBColumn constraint);
    @Nullable DBColumn getColumnForPosition(short position);
}