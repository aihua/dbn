package com.dci.intellij.dbn.execution;

import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;

public abstract class RemoteExecutionInput extends ExecutionInput{
    public RemoteExecutionInput(Project project, ExecutionTarget executionTarget) {
        super(project, executionTarget);
    }

    @Override
    public ConnectionId getConnectionHandlerId() {
        ConnectionHandler connectionHandler = getConnectionHandler();
        return connectionHandler == null ? null : connectionHandler.getId();
    }
}
