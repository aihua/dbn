package com.dci.intellij.dbn.common.util;

import com.dci.intellij.dbn.common.routine.BasicCallable;
import com.dci.intellij.dbn.common.routine.ParametricCallable;
import com.dci.intellij.dbn.common.routine.ParametricRunnable;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Safe {
    @NotNull
    static <R> R call(@NotNull R defaultValue, @NotNull BasicCallable<R> callable){
        try {
            return callable.call();
        } catch (ProcessCanceledException e){
            return defaultValue;
        }
    }

    static void run(@NotNull Runnable runnable){
        try {
            runnable.run();
        } catch (ProcessCanceledException ignore){}
    }


    @Nullable
    static <R, S, E extends Throwable> R call(@Nullable S target, @NotNull ParametricCallable<S, R, E> callable) throws E{
        if (target == null) {
            return null;
        } else {
            return callable.call(target);
        }
    }

    static <S, E extends Throwable> void run(@Nullable S target, @NotNull ParametricRunnable<S, E> runnable) throws E{
        if (target != null) {
            runnable.run(target);
        }
    }

    static <T> boolean equal(@Nullable T value1, @Nullable T value2) {
        if (value1 == null && value2 == null) {
            return true;
        }

        if (value1 == value2) {
            return true;
        }

        if (value1 != null) {
            return value1.equals(value2);
        }
        return false;
    }

    static <T extends Comparable<T>> int compare(@Nullable T value1, @Nullable T value2) {
        if (value1 == null && value2 == null) {
            return 0;
        }
        if (value1 == null) {
            return -1;
        }

        if (value2 == null) {
            return 1;
        }

        return value1.compareTo(value2);
    }

    static void queueRequest(@NotNull Alarm alarm, int delayMillis, boolean cancelRequests, @NotNull Runnable runnable) {
        Dispatch.runConditional(() -> {
            if (alarm.isDisposed()) {
                if (cancelRequests) {
                    alarm.cancelAllRequests();
                }

                if (!alarm.isDisposed()) {
                    alarm.addRequest(runnable, delayMillis);
                }
            }
        });
    }
}
