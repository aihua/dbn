package com.dci.intellij.dbn.common.thread;

import com.dci.intellij.dbn.common.routine.ParametricCallable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.ThrowableComputable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Read {
    private Read() {}

    public static <P, T, E extends Throwable> T call(P param, ParametricCallable<P, T, E> supplier) throws E {
        return getApplication().runReadAction((ThrowableComputable<T, E>) () -> supplier.call(param));
    }


    public static <T, E extends Throwable> T call(ThrowableComputable<T, E> supplier) throws E {
        return getApplication().runReadAction(supplier);
    }

    public static void run(Runnable runnable) {
        Application application = getApplication();
        application.runReadAction(runnable);
    }

    private static Application getApplication() {
        return ApplicationManager.getApplication();
    }
}
