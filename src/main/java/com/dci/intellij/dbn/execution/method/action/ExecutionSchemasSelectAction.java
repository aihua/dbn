package com.dci.intellij.dbn.execution.method.action;

import com.dci.intellij.dbn.common.action.ComboBoxAction;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.execution.method.MethodExecutionInput;
import com.dci.intellij.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ExecutionSchemasSelectAction extends ComboBoxAction {
    private MethodExecutionInput executionInput;

    public ExecutionSchemasSelectAction(MethodExecutionInput executionInput) {
        this.executionInput = executionInput;
        SchemaId schema = executionInput.getTargetSchemaId();
        if (schema != null) {
            Presentation presentation = getTemplatePresentation();
            presentation.setText(schema.getName(), false);
            presentation.setIcon(schema.getIcon());
        }
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent jComponent) {
        ConnectionHandler connection = executionInput.getConnection();
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        if (connection != null) {
            for (DBSchema schema : connection.getObjectBundle().getSchemas()){
                actionGroup.add(new ExecutionSchemaSelectAction(executionInput, schema));
            }
        }

        return actionGroup;
    }

    @Override
    public void update(AnActionEvent e) {
        SchemaId schema = executionInput.getTargetSchemaId();
        Presentation presentation = e.getPresentation();
        schema = Commons.nvl(schema, SchemaId.NONE);
        presentation.setText(schema.getName(), false);
        presentation.setIcon(schema.getIcon());
    }
 }