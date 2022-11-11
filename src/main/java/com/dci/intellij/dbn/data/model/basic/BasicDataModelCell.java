package com.dci.intellij.dbn.data.model.basic;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.Nullifier;
import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.property.PropertyHolderBase;
import com.dci.intellij.dbn.data.editor.text.TextContentType;
import com.dci.intellij.dbn.data.model.ColumnInfo;
import com.dci.intellij.dbn.data.model.DataModelCell;
import com.dci.intellij.dbn.data.model.DataModelState;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.value.ArrayValue;
import com.dci.intellij.dbn.data.value.LargeObjectValue;
import com.dci.intellij.dbn.editor.data.model.RecordStatus;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class BasicDataModelCell<
        R extends BasicDataModelRow<M, ? extends BasicDataModelCell<R, M>>,
        M extends BasicDataModel<R, ? extends BasicDataModelCell<R, M>>>
        extends PropertyHolderBase.IntStore<RecordStatus>
        implements DataModelCell<R, M> {

    protected R row;
    protected int index;
    protected Object userValue;
    private String presentableValue;

    public BasicDataModelCell(Object userValue, R row, int index) {
        this.userValue = userValue;
        this.row = row;
        this.index = index;
    }

    @Override
    protected RecordStatus[] properties() {
        return RecordStatus.VALUES;
    }

    @Override
    public Project getProject() {
        return getRow().getProject();
    }

    @Override
    public TextContentType getContentType() {
        DataModelState state = getModel().getState();
        String contentTypeName = state.getTextContentTypeName(getColumnInfo().getName());
        if (contentTypeName == null) {
            DBDataType dataType = getColumnInfo().getDataType();
            if (dataType.isNative()) {
                contentTypeName = dataType.getNativeType().getDefinition().getContentTypeName();
            }
        }

        return TextContentType.get(getProject(), contentTypeName);
    }

    @Override
    public void setContentType(TextContentType contentType) {
        DataModelState state = getModel().getState();
        state.setTextContentType(getColumnInfo().getName(), contentType.getName());
    }



    @Override
    @NotNull
    public R getRow() {
        return Failsafe.nd(row);
    }

    @Override
    public void setUserValue(Object userValue) {
        this.userValue = userValue;
        this.presentableValue = null;
    }

    @Override
    public void updateUserValue(Object userValue, boolean bulk) {
        setUserValue(userValue);
    }

    @Override
    public Object getUserValue() {
        return userValue;
    }

    public boolean isLobValue() {
        return userValue instanceof LargeObjectValue;
    }
    public boolean isArrayValue() {
        return userValue instanceof ArrayValue;
    }

    @Override
    public String getPresentableValue() {
        if (userValue != null) {
            if (presentableValue == null) {
                Formatter formatter = getFormatter();
                presentableValue = formatter.formatObject(userValue);
            }
            return presentableValue;
        }
        return null;
    }

    @NotNull
    @Override
    public M getModel() {
        return getRow().getModel();
    }

    @Override
    public String getName() {
        return getColumnInfo().getName();
    }

    @Override
    public DBDataType getDataType() {
        return getColumnInfo().getDataType();
    }

    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.COLUMN;
    }

    @Override
    public ColumnInfo getColumnInfo() {
        return getModel().getColumnInfo(index);
    }

    @Override
    public int getIndex() {
        return index;
    }

    public String toString() {
        // IMPORTANT return user value for copy to clipboard support
        return getPresentableValue();
    }

    @NotNull
    public Formatter getFormatter() {
        return getModel().getFormatter();
    }

    @Override
    public void dispose() {
        row = null;
        Nullifier.nullify(this);

    }
}
