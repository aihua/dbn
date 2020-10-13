package com.dci.intellij.dbn.generator.action;

import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.generator.StatementGenerationManager;
import com.dci.intellij.dbn.generator.StatementGeneratorResult;
import com.dci.intellij.dbn.object.DBTable;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GenerateInsertStatementAction extends GenerateStatementAction {
    private DBObjectRef<DBTable> tableRef;

    GenerateInsertStatementAction(DBTable table) {
        super("INSERT Statement");
        tableRef = DBObjectRef.of(table);
    }

    @Override
    protected StatementGeneratorResult generateStatement(Project project) {
        StatementGenerationManager statementGenerationManager = StatementGenerationManager.getInstance(project);
        DBTable table = getTable();
        return statementGenerationManager.generateInsert(table);
    }

    @NotNull
    private DBTable getTable() {
        return DBObjectRef.ensure(tableRef);
    }

    @Nullable
    @Override
    public ConnectionHandler getConnectionHandler() {
        return getTable().getConnectionHandler();
    }
}
