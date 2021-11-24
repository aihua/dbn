package com.dci.intellij.dbn.common.thread;

import com.dci.intellij.dbn.common.routine.ThrowableCallable;
import com.dci.intellij.dbn.common.routine.ThrowableRunnable;
import com.intellij.openapi.progress.ProcessCanceledException;
import org.jetbrains.annotations.NotNull;

public final class Synchronized {
    private Synchronized() {}

    private static final SyncObjectProvider SYNC_OBJECT_PROVIDER = new SyncObjectProvider();

    public static <E extends Throwable> void run(Object syncObject, Condition condition, ThrowableRunnable<E> runnable) throws E{
        try {
            if(condition.evaluate()) {
                synchronized (syncObject) {
                    if(condition.evaluate()) {
                        runnable.run();
                    }
                }
            }
        }
        catch (ProcessCanceledException ignore) {}
    }

    public static <E extends Throwable> void run(@NotNull String syncKey, ThrowableRunnable<E> runnable) throws E {
        try {
            Object syncObject = SYNC_OBJECT_PROVIDER.get(syncKey);
            synchronized (syncObject) {
                runnable.run();
            }
        }
        catch (ProcessCanceledException ignore) {}
        finally {
            SYNC_OBJECT_PROVIDER.release(syncKey);
        }
    }

    public static <T, E extends Throwable> T call(@NotNull String syncKey, ThrowableCallable<T, E> callable) throws E{
        try {
            Object syncObject = SYNC_OBJECT_PROVIDER.get(syncKey);
            synchronized (syncObject) {
                return callable.call();
            }
        }
        //catch (ProcessCanceledException ignore) {} // TODO return default?
        finally {
            SYNC_OBJECT_PROVIDER.release(syncKey);
        }
    }


    @FunctionalInterface
    public interface Condition {
        boolean evaluate();
    }
}
