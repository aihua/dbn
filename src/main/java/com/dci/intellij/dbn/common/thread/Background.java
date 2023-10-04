package com.dci.intellij.dbn.common.thread;

import com.dci.intellij.dbn.common.routine.ThrowableRunnable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static com.dci.intellij.dbn.common.thread.ThreadProperty.BACKGROUND;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
public final class Background {
    private static final Object lock = new Object();


    public static void run(Project project, ThrowableRunnable<Throwable> runnable) {
        try {
            Threads.delay(lock);
            ThreadInfo threadInfo = ThreadInfo.copy();
            ExecutorService executorService = Threads.backgroundExecutor();
            AtomicReference<Future<?>> future = new AtomicReference<>();

            future.set(executorService.submit(() -> {
                String taskId = PooledThread.enter(future.get());
                try {
                    ThreadMonitor.surround(
                            project,
                            threadInfo,
                            BACKGROUND,
                            runnable);
                } catch (ProcessCanceledException | UnsupportedOperationException | InterruptedException e) {
                    conditionallyLog(e);
                } catch (Throwable e) {
                    conditionallyLog(e);
                    log.error("Error executing background task", e);
                } finally {
                    PooledThread.exit(taskId);
                }
            }));
        } catch (RejectedExecutionException e) {
            conditionallyLog(e);
            log.warn("Background execution rejected: " + e.getMessage());
        }
    }

    public static void run(Project project, AtomicReference<PooledThread> handle, ThrowableRunnable<Throwable> runnable) {
        try {
            Threads.delay(lock);
            PooledThread current = handle.get();
            if (current != null) current.cancel();

            ThreadInfo threadInfo = ThreadInfo.copy();
            ExecutorService executorService = Threads.backgroundExecutor();

            AtomicReference<Future<?>> future = new AtomicReference<>();

            future.set(executorService.submit(() -> {
                String taskId = PooledThread.enter(future.get());
                try {
                    try {
                        handle.set(PooledThread.current());
                        ThreadMonitor.surround(
                                project,
                                threadInfo,
                                BACKGROUND,
                                runnable);
                    } finally {
                        handle.set(null);
                    }
                } catch (ProcessCanceledException | UnsupportedOperationException | InterruptedException e) {
                    conditionallyLog(e);
                } catch (Throwable e) {
                    conditionallyLog(e);
                    log.error("Error executing background task", e);
                } finally {
                    PooledThread.exit(taskId);
                }
            }));
        } catch (RejectedExecutionException e) {
            conditionallyLog(e);
            log.warn("Background execution rejected: " + e.getMessage());
        }
    }

}
