package com.dci.intellij.dbn.object.dependency.action;

import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

public class DependenciesActionGroup extends DefaultActionGroup {
    public DependenciesActionGroup(DBSchemaObject object) {
        super("Dependencies", true);
        add(new ObjectDependencyTreeAction(object));
        add(Actions.SEPARATOR);
        add(new ReferencedObjectsListShowAction(object));
        add(new ReferencingObjectsListShowAction(object));
    }
}