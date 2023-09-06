package com.dci.intellij.dbn.common.thread;

import com.dci.intellij.dbn.common.util.Measured;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;

import static com.dci.intellij.dbn.common.dispose.Failsafe.guarded;
import static com.dci.intellij.dbn.common.dispose.Failsafe.nd;
import static com.intellij.openapi.command.WriteCommandAction.writeCommandAction;

@UtilityClass
public final class Write {

    public static void run(Runnable runnable) {
        run(null, runnable);
    }

    public static void run(Project project, Runnable runnable) {
        Application application = ApplicationManager.getApplication();
        if (application.isWriteAccessAllowed()) {
            if (project == null) {
                Measured.run("executing Write action", () -> guarded(runnable, r -> r.run()));
            } else {
                Measured.run("executing Write action", () -> guarded(() -> writeCommandAction(nd(project)).run(() -> runnable.run())));
            }

        } else if (application.isDispatchThread()) {
            application.runWriteAction(() -> run(project, runnable));

        } else {
            Dispatch.run(() -> run(project, runnable));
        }
    }
}
