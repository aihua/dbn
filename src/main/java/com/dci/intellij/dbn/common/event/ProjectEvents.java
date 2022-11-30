package com.dci.intellij.dbn.common.event;

import com.dci.intellij.dbn.common.project.Projects;
import com.dci.intellij.dbn.common.routine.Consumer;
import com.dci.intellij.dbn.common.util.Guarded;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dci.intellij.dbn.common.dispose.Checks.isNotValid;
import static com.dci.intellij.dbn.common.dispose.Failsafe.nd;


public final class ProjectEvents {
    private ProjectEvents() {}

    public static <T> void subscribe(@NotNull Project project, @Nullable Disposable parentDisposable, Topic<T> topic, T handler) {
        Guarded.run(() -> {
            if (isNotValid(project) || project.isDefault()) return;

            MessageBus messageBus = messageBus(project);
            MessageBusConnection connection = parentDisposable == null ?
                    messageBus.connect() :
                    messageBus.connect(nd(parentDisposable));

            connection.subscribe(topic, handler);
        });
    }

    public static <T> void subscribe(Topic<T> topic, T handler) {
        Project[] openProjects = Projects.getOpenProjects();
        for (Project openProject : openProjects) {
            subscribe(openProject, null, topic, handler);
        }

        Application application = ApplicationManager.getApplication();
        application.invokeLater(() ->
                Projects.projectOpened(project ->
                    subscribe(project, null, topic, handler)));

    }

    public static <T> void notify(@Nullable Project project, Topic<T> topic, Consumer<T> consumer) {
        Guarded.run(() -> {
            if (isNotValid(project) || project.isDefault()) return;
            T publisher = publisher(project, topic);
            consumer.accept(publisher);
        });
    }

    @NotNull
    private static <T> T publisher(@Nullable Project project, Topic<T> topic) {
        MessageBus messageBus = messageBus(project);
        return messageBus.syncPublisher(topic);
    }

    @NotNull
    private static MessageBus messageBus(@Nullable Project project) {
        return nd(project).getMessageBus();
    }
}
