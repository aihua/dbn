package com.dci.intellij.dbn.common.load;

import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.util.CollectionUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Disposer;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public abstract class LoadInProgressRegistry<T extends StatefulDisposable> extends StatefulDisposable.Base {
    private final List<T> nodes = CollectionUtil.createConcurrentList();

    private LoadInProgressRegistry(Disposable parentDisposable) {
        Disposer.register(parentDisposable, this);
    }

    @Override
    protected void disposeInner() {
        nodes.clear();
    }

    public void register(T node) {
        boolean startTimer = nodes.size() == 0;
        nodes.add(node);
        if (startTimer) {
            Timer reloader = new Timer("DBN - Object Browser (load in progress reload timer)");
            reloader.schedule(new RefreshTask(), 0, LoadInProgressIcon.ROLL_INTERVAL);
        }
    }

    private class RefreshTask extends TimerTask {
        @Override
        public void run() {
            for (T node : nodes) {
                try {
                    if (node.isDisposed()) {
                        nodes.remove(node);
                    } else {
                        LoadInProgressRegistry.this.notify(node);
                    }
                } catch (ProcessCanceledException ignore) {
                    nodes.remove(node);
                }
            }

            if (nodes.isEmpty()) {
                cancel();
            }
        }
    }

    protected abstract void notify(T node);

    public static <T extends StatefulDisposable> LoadInProgressRegistry<T> create(StatefulDisposable parentDisposable, Notifier<T> notifier) {
        return new LoadInProgressRegistry<T>(parentDisposable) {
            @Override
            protected void notify(T node) {
                notifier.notify(node);
            }
        };
    }

    @FunctionalInterface
    public interface Notifier<T extends StatefulDisposable> {
        void notify(T node);
    }
}
