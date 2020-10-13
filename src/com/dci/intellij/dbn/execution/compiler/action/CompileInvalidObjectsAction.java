package com.dci.intellij.dbn.execution.compiler.action;

import com.dci.intellij.dbn.connection.operation.options.OperationSettings;
import com.dci.intellij.dbn.execution.compiler.CompileType;
import com.dci.intellij.dbn.execution.compiler.DatabaseCompilerManager;
import com.dci.intellij.dbn.execution.compiler.options.CompilerSettings;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class CompileInvalidObjectsAction extends DumbAwareAction {
    private DBObjectRef<DBSchema> schemaRef;
    public CompileInvalidObjectsAction(DBSchema schema) {
        super("Compile invalid objects");
        this.schemaRef = DBObjectRef.of(schema);
    }

    @NotNull
    public DBSchema getSchema() {
        return DBObjectRef.ensure(schemaRef);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DBSchema schema = getSchema();
        Project project = schema.getProject();
        DatabaseCompilerManager compilerManager = DatabaseCompilerManager.getInstance(project);
        compilerManager.compileInvalidObjects(schema, getCompilerSettings(project).getCompileType());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        DBSchema schema = getSchema();
        CompileType compileType = getCompilerSettings(schema.getProject()).getCompileType();
        String text = "Compile Invalid Objects";
        if (compileType == CompileType.DEBUG) text = text + " (Debug)";
        if (compileType == CompileType.ASK) text = text + "...";

        e.getPresentation().setText(text);
    }

    private static CompilerSettings getCompilerSettings(Project project) {
        return OperationSettings.getInstance(project).getCompilerSettings();
    }
}