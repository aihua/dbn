package com.dci.intellij.dbn.object.dependency.action;

import org.jetbrains.annotations.NotNull;

import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.dependency.ObjectDependencyManager;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

public class ObjectDependencyTreeAction extends DumbAwareAction {
    private DBObjectRef<DBSchemaObject> schemaObjectRef;

    public ObjectDependencyTreeAction(DBSchemaObject schemaObject) {
        super("Dependency Tree...");
        schemaObjectRef = DBObjectRef.from(schemaObject);
        setDefaultIcon(true);
    }

    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        DBSchemaObject schemaObject = DBObjectRef.get(schemaObjectRef);
        if (schemaObject != null && project != null) {
            ObjectDependencyManager dependencyManager = ObjectDependencyManager.getInstance(project);
            dependencyManager.openDependencyTree(schemaObject);
        }
    }
}