package com.dci.intellij.dbn.common.thread;

import com.dci.intellij.dbn.common.load.ProgressMonitor;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.dci.intellij.dbn.connection.Savepoints;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.database.interfaces.queue.InterfaceTaskCancelledException;
import com.intellij.openapi.progress.ProgressIndicator;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.dci.intellij.dbn.common.exception.Exceptions.causeOf;
import static com.dci.intellij.dbn.common.exception.Exceptions.toSqlException;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public abstract class CancellableDatabaseCall<T> implements Callable<T> {

    private final long startTimestamp = System.currentTimeMillis();
    private final ThreadInfo invoker = ThreadInfo.copy();

    private final ConnectionRef connection;
    private final DBNConnection conn;
    private final int timeout;
    private final TimeUnit timeUnit;
    private final boolean createSavepoint;

    private transient Future<T> future;
    private transient boolean cancelled = false;
    private transient boolean cancelRequested = false;
    private Timer cancelCheckTimer;

    protected CancellableDatabaseCall(ConnectionHandler connection, @Nullable DBNConnection conn, int timeout, TimeUnit timeUnit) {
        this.connection = ConnectionRef.of(connection);
        this.conn = conn;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        createSavepoint = !DatabaseFeature.CONNECTION_ERROR_RECOVERY.isSupported(connection);
    }

    public void requestCancellation() {
        cancelRequested = true;
    }

    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    @Override
    public T call() throws Exception {
        return ThreadMonitor.surround(
                invoker.getProject(),
                invoker,
                ThreadProperty.CANCELABLE,
                () -> {
                    if (createSavepoint) {
                        AtomicReference<Exception> innerException = new AtomicReference<>();
                        T result = Savepoints.call(conn, () -> {
                            try {
                                return CancellableDatabaseCall.this.execute();
                            } catch (SQLException e) {
                                conditionallyLog(e);
                                throw e;
                            } catch (Exception e) {
                                conditionallyLog(e);
                                innerException.set(e);
                                return null;
                            }
                        });

                        Exception exception = innerException.get();
                        if (exception != null) {
                            throw exception;
                        }
                        return result;

                    } else {
                        return execute();
                    }
                });
    }

    public abstract T execute() throws Exception;

    public abstract void cancel();

    public boolean isCancelRequested() {
        return cancelRequested || ProgressMonitor.isProgressCancelled();
    }


    public final T start() throws SQLException {
        try {
            cancelCheckTimer = new Timer("DBN - Execution Cancel Watcher");
            ProgressIndicator progress = ProgressMonitor.getProgressIndicator();
            TimerTask cancelCheckTask = new TimerTask() {
                @Override
                public void run() {
                    if (!cancelled && progress != null && progress.isCanceled()) {
                        cancelled = true;
                        if (future != null) future.cancel(true);
                        cancelSilently();
                        cancelCheckTimer.cancel();
                    } else {
                        if (progress != null && timeout > 0) {
                            String text = progress.getText();
                            int index = text.indexOf(" (timing out in ");
                            if (index > -1) {
                                text = text.substring(0, index);
                            }

                            long runningForSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTimestamp);
                            long timeoutSeconds = timeUnit.toSeconds(timeout);
                            long timingOutIn = Math.max(timeoutSeconds - runningForSeconds, 0);
                            if (timingOutIn < 60)
                                text = text + " (timing out in " + timingOutIn + " seconds) "; else
                                text = text + " (timing out in " + TimeUnit.SECONDS.toMinutes(timingOutIn) + " minutes) ";


                            progress.setText(text);
                        }
                    }
                }
            };

            cancelCheckTimer.schedule(cancelCheckTask, 100, 100);

            try {
                ExecutorService executorService = Threads.cancellableExecutor();
                future = executorService.submit(this);
                return timeout == 0 ?  future.get() : future.get(timeout, timeUnit);
            } finally {
                future = null;
                if (cancelCheckTimer != null) {
                    cancelCheckTimer.cancel();
                }
            }

        } catch (CancellationException | InterruptedException e) {
            conditionallyLog(e);
            cancelSilently();
            throw InterfaceTaskCancelledException.INSTANCE;

        } catch (ExecutionException e) {
            conditionallyLog(e);
            Throwable cause = causeOf(e);
            if (cause instanceof SQLTimeoutException) {
                handleTimeout();
            } else {
                handleException(cause);
            }

        } catch (TimeoutException e)  {
            conditionallyLog(e);
            handleTimeout();
        }
        return null;
    }

    public void handleTimeout() throws SQLTimeoutException {
        if (cancelled) {
            throw InterfaceTaskCancelledException.INSTANCE;
        }

        cancelSilently();
        throw new SQLTimeoutException("Operation has timed out (" + timeout + "s). Check timeout settings");
    }

    public void handleException(Throwable e) throws SQLException{
        throw toSqlException(e);
    }

    public void cancelSilently() {
        try {
            cancel();
        } catch (Exception e) {
            conditionallyLog(e);
            log.warn("Failed to cancel database call", e);
        }
    }

}
