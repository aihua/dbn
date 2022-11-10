package com.dci.intellij.dbn.connection.jdbc;

import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.common.thread.ThreadMonitor;
import com.dci.intellij.dbn.common.thread.Timeout;
import com.dci.intellij.dbn.common.util.Exceptions;
import com.dci.intellij.dbn.common.util.TimeUtil;
import com.dci.intellij.dbn.language.common.WeakRef;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.dci.intellij.dbn.common.util.Commons.nvl;
import static com.dci.intellij.dbn.common.util.TimeUtil.isOlderThan;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.isDatabaseResourceDebug;

@Slf4j
public abstract class ResourceStatusAdapterImpl<T extends Resource> implements ResourceStatusAdapter<T> {
    private final WeakRef<T> resource;
    private final ResourceStatus subject;
    private final ResourceStatus changing;
    private final ResourceStatus evaluating;
    private final Boolean terminalStatus;
    private final long checkInterval;
    private long checkTimestamp;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    ResourceStatusAdapterImpl(T resource, ResourceStatus subject, ResourceStatus changing, ResourceStatus evaluating, long checkInterval, @NotNull Boolean initialStatus, @Nullable Boolean terminalStatus) {
        this.resource = WeakRef.of(resource);
        this.subject = subject;
        this.changing = changing;
        this.evaluating = evaluating;
        this.checkInterval = checkInterval;
        this.terminalStatus = terminalStatus;
        set(subject, initialStatus);
    }

    @Override
    public final boolean get() {
        if (readLock.tryLock()) {
            try {
                if (canEvaluate()) {
                    evaluate();
                }
            } finally {
                readLock.unlock();
            }
        }
        return value();
    }

    @Override
    public final void set(boolean value) throws SQLException {
        if (writeLock.tryLock()) {
            try {
                if (canChange(value)) {
                    change(value);
                }
            } finally {
                writeLock.unlock();
            }
        }
    }


    private boolean is(ResourceStatus status) {
        T resource = getResource();
        return resource.is(status);
    }

    private boolean set(ResourceStatus status, boolean value) {
        T resource = getResource();
        boolean changed = resource.set(status, value);
        if (status == this.subject && changed) resource.statusChanged(this.subject);
        return changed;
    }

    @NotNull
    private T getResource() {
        return this.resource.ensure();
    }

    private boolean value() {
        return is(subject);
    }

    private boolean isEvaluating() {
        return is(evaluating);
    }

    private boolean isChanging() {
        return is(changing);
    }

    private void evaluate() {
        set(evaluating, true);

        if (ThreadMonitor.isDispatchThread()) {
            Background.run(() -> evaluate());
            return;
        }

        try {
            if (preEvaluate()) {
                boolean value = checkControlled();
                set(subject, value);
            }
        } catch (SQLRecoverableException e){
            fail();
        } catch (Exception e){
            log.warn("[DBN] Failed to check resource {} status", subject, e);
            fail();
        } finally {
            set(evaluating, false);
        }
    }

    private boolean preEvaluate() {
        boolean check = false;
        if (checkInterval == 0) {
            check = true;
        } else {
            if (isOlderThan(checkTimestamp, checkInterval)) {
                checkTimestamp = System.currentTimeMillis();
                check = true;
            }
        }
        return check;
    }

    private void change(boolean value) throws SQLException {
        set(changing, true);

        if (ThreadMonitor.isDispatchThread()) {
            Background.run(() -> change(value));
            return;
        }

        try {
            changeControlled(value);
        } finally {
            set(changing, false);
        }
    }

    private void fail() {
        if (terminalStatus != null) {
            set(subject, terminalStatus);
        } else {
            if (checkInterval > 0) {
                checkTimestamp =
                    System.currentTimeMillis() - checkInterval + TimeUtil.Millis.FIVE_SECONDS; // retry in 5 seconds
            }

        }
    }

    private boolean canEvaluate() {
        if (isEvaluating() || isChanging() || isTerminal()) {
            return false;
        } else {
            return true;
        }
    }

    private boolean canChange(boolean value) {
        if (isChanging() || isTerminal()) {
            return false;
        } else {
            return value() != value;
        }
    }

    private boolean isTerminal() {
        return terminalStatus != null && terminalStatus == value();
    }

    private boolean checkControlled() throws SQLException{
        AtomicReference<SQLException> exception = new AtomicReference<>();
        Boolean result = Timeout.call(5, is(subject), true, () -> {
            try {
                return checkInner();
            } catch (SQLException e) {
                exception.set(e);
                return nvl(terminalStatus, () -> value());
            } catch (AbstractMethodError | NoSuchMethodError e) {
                // not implemented (??) TODO suggest using built in drivers
                log.warn("[DBN] Functionality not supported by jdbc driver", e);
                return value();
            } catch (RuntimeException t){
                log.warn("[DBN] Failed to invoke jdbc utility", t);
                return nvl(terminalStatus, () -> value());
            }
        });
        if (exception.get() != null) {
            throw exception.get();
        }
        return result;
    }

    private void changeControlled(boolean value) throws SQLException{
        boolean daemon = true;
        T resource = getResource();
        ResourceType resourceType = resource.getResourceType();
        if (resourceType == ResourceType.CONNECTION && subject == ResourceStatus.CLOSED) {
            // non daemon threads for closing connections
            daemon = false;
        }

        SQLException exception = Timeout.call(10, null, daemon, () -> {
            try {
                if (isDatabaseResourceDebug())
                    log.info("[DBN] Applying status {} = {} for {}", subject, value, resource);
                changeInner(value);
                set(subject, value);
                if (isDatabaseResourceDebug())
                    log.info("[DBN] Done applying status {} = {} for {}", subject, value, resource);
            } catch (Throwable e) {
                log.warn("[DBN] Failed to apply status {} = {} for {}: {}", subject, value, resource, e.getMessage());
                fail();
                return Exceptions.toSqlException(e);
            }
            return null;
        });

        if (exception != null) {
            throw exception;
        }

    }

    protected abstract void changeInner(boolean value) throws SQLException;

    protected abstract boolean checkInner() throws SQLException;

    @Override
    public String toString() {
        return getResource().toString();
    }
}
