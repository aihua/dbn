package com.dci.intellij.dbn.data.editor.ui;

import com.dci.intellij.dbn.data.editor.text.TextContentType;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;

public interface UserValueHolder<T> {
    void setUserValue(T userValue);
    void updateUserValue(T userValue, boolean bulk);
    TextContentType getContentType();
    void setContentType(TextContentType contentType);
    T getUserValue();
    String getPresentableValue();
    String getName();
    DBDataType getDataType();
    DBObjectType getObjectType();
    Project getProject();
}
