package com.dci.intellij.dbn.common.dispose;

import com.dci.intellij.dbn.common.routine.ParametricCallable;
import com.dci.intellij.dbn.common.routine.ParametricRunnable;
import com.dci.intellij.dbn.common.routine.ThrowableCallable;
import com.dci.intellij.dbn.common.routine.ThrowableRunnable;
import com.dci.intellij.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dci.intellij.dbn.common.dispose.Checks.isNotValid;
import static com.dci.intellij.dbn.common.util.Commons.nvl;

@Slf4j
public class Failsafe {
    public static @NotNull <T> T nn(@Nullable T object) {
        if (object == null) {
            throw new AlreadyDisposedException(null);
        }
        return object;
    }

    @NotNull
    public static <T> T nd(@Nullable T object) {
        if (isNotValid(object)) {
            throw new AlreadyDisposedException(object);
        }
        return object;
    }

    public static <P, R, E extends Throwable> R guarded(R defaultValue, P param, @Nullable ParametricCallable<P, R, E> callable) throws E{
        try {
            return callable == null ? defaultValue : callable.call(param);
        } catch (ProcessCanceledException | IllegalStateException | AbstractMethodError e /*| UnsupportedOperationException*/){
            conditionallyLog(e);
            return defaultValue;
        } catch (Exception e) {
            conditionallyLog(e);
            throwExecutionException(e);
            return defaultValue;

        }
    }

    public static <R, E extends Throwable> R guarded(R defaultValue, @Nullable ThrowableCallable<R, E> callable) throws E{
        try {
            return callable == null ? defaultValue : callable.call();
        } catch (ProcessCanceledException | IllegalStateException | AbstractMethodError e /*| UnsupportedOperationException*/){
            conditionallyLog(e);
            return defaultValue;
        } catch (Exception e) {
            conditionallyLog(e);
            throwExecutionException(e);
            return defaultValue;

        }
    }


    public static <P, E extends Throwable> void guarded(P param, @Nullable ParametricRunnable<P, E> runnable) throws E{
        try {
            if (runnable != null) runnable.run(param);
        } catch (ProcessCanceledException | IllegalStateException | AbstractMethodError e /*| UnsupportedOperationException*/){
            conditionallyLog(e);
        } catch (Exception e) {
            conditionallyLog(e);
            throwExecutionException(e);
        }
    }

    public static <E extends Throwable> void guarded(@Nullable ThrowableRunnable<E> runnable) throws E{
        try {
            if (runnable != null) runnable.run();
        } catch (ProcessCanceledException | IllegalStateException | AbstractMethodError e /*| UnsupportedOperationException*/){
            conditionallyLog(e);
        } catch (Exception e) {
            conditionallyLog(e);
            throwExecutionException(e);
        }
    }

    public static void conditionallyLog(Throwable exception) {
        if (Diagnostics.isFailsafeLoggingEnabled()) {
            String message = nvl(exception.getMessage(), exception.getClass().getSimpleName());
            log.warn("[FAILSAFE] " + message, exception);
        }
    }

    @SneakyThrows
    private static void throwExecutionException(Exception e) {
        // DBNE-4876 (????!!)
        if (!e.getClass().getName().equals(AlreadyDisposedException.class.getName())) {
            conditionallyLog(e);
            throw e;
        }
    }


}
