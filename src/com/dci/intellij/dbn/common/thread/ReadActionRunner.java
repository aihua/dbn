package com.dci.intellij.dbn.common.thread;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Computable;

public abstract class ReadActionRunner<T> {
    private boolean conditional;
    private ReadActionRunner(boolean conditional) {
        this.conditional = conditional;
    }

    public final T start() {
        Application application = ApplicationManager.getApplication();
        if (conditional) {
            if (application.isReadAccessAllowed()) {
                try {
                    return run();
                } catch (ProcessCanceledException e) {
                    return null;
                }
            }
        }

        Computable<T> readAction = () -> {
            try {
                return ReadActionRunner.this.run();
            } catch (ProcessCanceledException e) {
                return null;
            }

        };
        return application.runReadAction(readAction);
    }

    protected abstract T run();

    public static <T> T invoke(boolean conditional, Callable<T> callable) {
        return new ReadActionRunner<T>(conditional) {
            @Override
            protected T run() {
                return callable.call();
            }

        }.start();
    }

    @FunctionalInterface
    public interface Callable<V> {
        V call();
    }
}
