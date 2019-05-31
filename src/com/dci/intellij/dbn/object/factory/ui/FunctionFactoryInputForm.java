package com.dci.intellij.dbn.object.factory.ui;

import com.dci.intellij.dbn.data.type.ui.DataTypeEditor;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.factory.ArgumentFactoryInput;
import com.dci.intellij.dbn.object.factory.MethodFactoryInput;
import com.dci.intellij.dbn.object.factory.ObjectFactoryInput;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;

public class FunctionFactoryInputForm extends MethodFactoryInputForm {

    public FunctionFactoryInputForm(Project project, DBSchema schema, DBObjectType objectType, int index) {
        super(project, schema, objectType, index);
    }

    @Override
    public MethodFactoryInput createFactoryInput(ObjectFactoryInput parent) {
        MethodFactoryInput methodFactoryInput = super.createFactoryInput(parent);

        DataTypeEditor returnTypeEditor = (DataTypeEditor) returnArgumentDataTypeEditor;

        ArgumentFactoryInput returnArgument = new ArgumentFactoryInput(
                methodFactoryInput, 0, "return",
                returnTypeEditor.getDataTypeRepresentation(),
                false, true);

        methodFactoryInput.setReturnArgument(returnArgument);
        return methodFactoryInput;
    }

    @Override
    public boolean hasReturnArgument() {
        return true;
    }
}
