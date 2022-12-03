package com.dci.intellij.dbn.common.event;

import com.dci.intellij.dbn.common.routine.Consumer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dci.intellij.dbn.common.dispose.Failsafe.guarded;
import static com.dci.intellij.dbn.common.dispose.Failsafe.nd;

public final class ApplicationEvents {
    private ApplicationEvents() {
    }

    public static <T> void subscribe(@Nullable Disposable parentDisposable, Topic<T> topic, T handler) {
        guarded(() -> {
            MessageBus messageBus = messageBus();
            MessageBusConnection connection = parentDisposable == null ?
                    messageBus.connect() :
                    messageBus.connect(nd(parentDisposable));
            connection.subscribe(topic, handler);
        });
    }

    public static <T> void notify(Topic<T> topic, Consumer<T> consumer) {
        T publisher = publisher(topic);
        consumer.accept(publisher);
    }

    public static <T> T publisher(Topic<T> topic) {
        MessageBus messageBus = messageBus();
        return messageBus.syncPublisher(topic);
    }

    @NotNull
    private static MessageBus messageBus() {
        return ApplicationManager.getApplication().getMessageBus();
    }
}
