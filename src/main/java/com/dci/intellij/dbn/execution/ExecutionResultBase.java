package com.dci.intellij.dbn.execution;

import com.dci.intellij.dbn.common.dispose.StatefulDisposableBase;
import com.dci.intellij.dbn.execution.common.result.ui.ExecutionResultForm;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public abstract class ExecutionResultBase<F extends ExecutionResultForm> extends StatefulDisposableBase implements ExecutionResult<F> {
    private ExecutionResult<F> previous;

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        return null;
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
