package com.dci.intellij.dbn.execution;

import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.options.PersistentConfiguration;
import com.dci.intellij.dbn.common.options.setting.SettingsSupport;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.connection.context.ConnectionProvider;
import com.dci.intellij.dbn.execution.common.options.ExecutionEngineSettings;
import com.dci.intellij.dbn.execution.common.options.ExecutionTimeoutSettings;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ExecutionInput extends StatefulDisposable.Base implements StatefulDisposable, ConnectionProvider, PersistentConfiguration {
    private final ExecutionTimeout executionTimeout;
    private final ExecutionTimeout debugExecutionTimeout;
    private final ExecutionTarget executionTarget;

    private final ProjectRef project;
    protected ConnectionRef targetConnection;
    protected SchemaId targetSchemaId;

    private final Latent<ExecutionContext> executionContext = Latent.basic(() -> createExecutionContext());

    @NotNull
    public final ExecutionContext getExecutionContext() {
        return executionContext.get();
    }

    protected void resetExecutionContext() {
        executionContext.reset();
    }

    protected abstract ExecutionContext createExecutionContext();

    public ExecutionInput(Project project, ExecutionTarget executionTarget) {
        this.project = ProjectRef.of(project);
        this.executionTarget = executionTarget;
        executionTimeout = new ExecutionTimeout(project, executionTarget, false);
        debugExecutionTimeout = new ExecutionTimeout(project, executionTarget, true);
    }

    @NotNull
    public ExecutionTimeoutSettings getExecutionTimeoutSettings() {
        Project project = getProject();
        ExecutionEngineSettings executionEngineSettings = ExecutionEngineSettings.getInstance(project);
        return executionEngineSettings.getExecutionTimeoutSettings(getExecutionTarget());
    }

    @NotNull
    public final Project getProject() {
        return project.ensure();
    }

    @Nullable
    public final SchemaId getTargetSchemaId() {
        return targetSchemaId;
    }

    public final void setTargetSchemaId(@Nullable SchemaId schema){
        targetSchemaId = schema;
    }

    @Nullable
    public final ConnectionHandler getTargetConnection() {
        return ConnectionRef.get(targetConnection);
    }

    public void setTargetConnection(@Nullable ConnectionHandler connection) {
        this.targetConnection = ConnectionRef.of(connection);
    }

    @NotNull
    public ExecutionContext initExecutionContext() {
        ExecutionContext executionContext = getExecutionContext();
        executionContext.reset();
        return executionContext;
    }

    public int getExecutionTimeout() {
        return executionTimeout.get();
    }

    public void setExecutionTimeout(int timeout) {
        executionTimeout.set(timeout);
    }

    public int getDebugExecutionTimeout() {
        return debugExecutionTimeout.get();
    }

    public void setDebugExecutionTimeout(int timeout) {
        debugExecutionTimeout.set(timeout);
    }

    public abstract ConnectionId getConnectionId();

    @Override
    public void readConfiguration(Element element) {
        executionTimeout.set(SettingsSupport.integerAttribute(element, "execution-timeout", executionTimeout.get()));
        debugExecutionTimeout.set(SettingsSupport.integerAttribute(element, "debug-execution-timeout", debugExecutionTimeout.get()));
    }

    @Override
    public void writeConfiguration(Element element) {
        SettingsSupport.setIntegerAttribute(element, "execution-timeout", executionTimeout.get());
        SettingsSupport.setIntegerAttribute(element, "debug-execution-timeout", debugExecutionTimeout.get());
    }

    @NotNull
    public final ExecutionTarget getExecutionTarget() {
        return executionTarget;
    }

    @Override
    protected void disposeInner() {
        nullify();
    }
}