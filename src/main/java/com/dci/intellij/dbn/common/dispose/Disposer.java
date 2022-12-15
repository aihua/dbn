package com.dci.intellij.dbn.common.dispose;

import com.dci.intellij.dbn.common.list.FilteredList;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.vfs.DBVirtualFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.tabs.JBTabs;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.*;

import static com.dci.intellij.dbn.common.dispose.Checks.isNotValid;
import static com.dci.intellij.dbn.common.dispose.Failsafe.guarded;
import static com.dci.intellij.dbn.common.thread.ThreadMonitor.isDispatchThread;

@Slf4j
public final class Disposer {
    private static final List<Class<?>> DISPATCH_CANDIDATES = Arrays.asList(
            JBPopup.class,
            JBTabs.class
            /*, ...*/);

    private Disposer() {}

    public static void register(@Nullable Disposable parent, @NotNull Object object) {
        if (object instanceof Disposable) {
            Disposable disposable = (Disposable) object;
            register(parent, disposable);
        }
    }

    public static void register(@Nullable Disposable parent, @NotNull Disposable disposable) {
        if (parent == null) return;

        if (disposable instanceof UnlistedDisposable) {
            log.error("Unlisted disposable {} should not be registered",
                    disposable.getClass().getName(),
                    new IllegalArgumentException("Unlisted disposable"));
        }

        if (Checks.isValid(parent)) {
            com.intellij.openapi.util.Disposer.register(parent, disposable);
        } else {
            // dispose if parent already disposed
            dispose(disposable);
        }
    }

    public static void dispose(@Nullable Disposable disposable) {
        try {
            guarded(() -> {
                if (isNotValid(disposable)) return;

                if (isDispatchCandidate(disposable) && !isDispatchThread()) {
                    Dispatch.run(() -> dispose(disposable));
                    return;
                }

                if (disposable instanceof UnlistedDisposable) {
                    disposable.dispose();
                } else {
                    com.intellij.openapi.util.Disposer.dispose(disposable);
                }

            });
        } catch (Throwable e) {
            log.warn("Failed to dispose entity {}", disposable, e);
        }
    }

    public static <T> T replace(T oldElement, T newElement) {
        dispose(oldElement);
        return newElement;
    }

    public static void dispose(@Nullable Object object) {
        if (object == null) return;

        if (object instanceof Disposable) {
            Disposable disposable = (Disposable) object;
            BackgroundDisposer.queue(() -> dispose(disposable));

        } else if (object instanceof Collection) {
            BackgroundDisposer.queue(() -> disposeCollection((Collection<?>) object));

        } else if (object instanceof Map) {
            BackgroundDisposer.queue(() -> disposeMap((Map) object));

        } else if (object.getClass().isArray()) {
            BackgroundDisposer.queue(() -> disposeArray((Object[]) object));

        } else if (object instanceof Reference) {
            Reference reference = (Reference) object;
            dispose(reference.get());
        }
    }

    public static void disposeCollection(@Nullable Collection<?> collection) {
        if (collection == null) return;

        if (collection instanceof FilteredList) {
            FilteredList<?> filteredList = (FilteredList<?>) collection;
            collection = filteredList.getBase();
        }

        if (collection.isEmpty()) return;

        for (Object object : collection) {
            if (object instanceof Disposable) {
                Disposable disposable = (Disposable) object;
                dispose(disposable);
            }
        }
        Nullifier.clearCollection(collection);
    }

    public static void disposeArray(@Nullable Object[] array) {
        if (array == null || array.length == 0) return;

        for (int i = 0; i < array.length; i++) {
            Object object = array[i];
            if (object instanceof Disposable) {
                Disposable disposable = (Disposable) object;
                dispose(disposable);
            }
            array[i] = null;
        }
    }

    public static void disposeMap(@Nullable Map<?, ?> map) {
        if (map == null || map.isEmpty()) return;

        for (Object object : map.values()) {
            if (object instanceof Disposable) {
                Disposable disposable = (Disposable) object;
                dispose(disposable);
            }
        }
        Nullifier.clearMap(map);
    }

    public static void dispose(@Nullable Timer timer) {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    public static void dispose(@Nullable DBVirtualFile virtualFile) {
        if (virtualFile == null) return;

        virtualFile.invalidate();
    }

    private static boolean isDispatchCandidate(Object object) {
        for (Class<?> candidate : DISPATCH_CANDIDATES) {
            if (candidate.isAssignableFrom(object.getClass())) {
                return true;
            }
        }
        return false;
    }
}
