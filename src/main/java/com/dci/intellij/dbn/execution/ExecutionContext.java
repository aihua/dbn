package com.dci.intellij.dbn.execution;

import com.dci.intellij.dbn.common.load.ProgressMonitor;
import com.dci.intellij.dbn.common.property.PropertyHolder;
import com.dci.intellij.dbn.common.property.PropertyHolderBase;
import com.dci.intellij.dbn.common.ref.WeakRef;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.jdbc.DBNStatement;
import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dci.intellij.dbn.common.util.Unsafe.cast;
import static com.dci.intellij.dbn.execution.ExecutionStatus.*;

@Getter
@Setter
public abstract class ExecutionContext<T extends ExecutionInput> extends PropertyHolderBase.IntStore<ExecutionStatus> implements PropertyHolder<ExecutionStatus> {
    private int timeout;
    private boolean logging = false;
    private long executionTimestamp;
    private DBNConnection connection;
    private DBNStatement statement;
    private DBDebuggerType debuggerType;
    private WeakRef<T> input;
    private WeakRef<ProgressIndicator> progress;

    public ExecutionContext(T input) {
        this.input = WeakRef.of(input);
        this.progress = WeakRef.of(ProgressMonitor.getProgressIndicator());
    }

    @NotNull
    public T getInput() {
        return WeakRef.ensure(input);
    }

    @Override
    protected ExecutionStatus[] properties() {
        return VALUES;
    }

    @NotNull
    public abstract String getTargetName();

    @Nullable
    public abstract ConnectionHandler getTargetConnection();

    @Nullable
    public abstract SchemaId getTargetSchema();

    public boolean canExecute() {
        return isNot(QUEUED) && isNot(EXECUTING) && isNot(CANCELLED);
    }

    public <S extends DBNStatement> S getStatement() {
        return cast(statement);
    }

    public Project getProject() {
        return getInput().getProject();
    }

    @Nullable
    public ProgressIndicator getProgress() {
        return WeakRef.get(progress);
    }

    @Override
    public void reset() {
        super.reset();
        timeout = 0;
        logging = false;
        executionTimestamp = System.currentTimeMillis();
        connection = null;
        statement = null;
        this.progress = WeakRef.of(ProgressMonitor.getProgressIndicator());
    }
}
