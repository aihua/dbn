package com.dci.intellij.dbn.common.util;

import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.util.objectTree.ThrowableInterner;
import org.jetbrains.annotations.NotNull;

/**
 * Internal API utilities - find alternatives
 */
public class InternalApi {
    public static boolean isApplicationExitInProgress() {
        return Traces.isCalledThrough(ApplicationImpl.class, "exit");
        //return ApplicationManagerEx.getApplicationEx().isDisposeInProgress();
        //return ApplicationManagerEx.getApplicationEx().isExitInProgress();
    }

    public static boolean isComponentsLoadedOccurred() {
        return true;
    }

    @NotNull
    static Throwable getThrowableIntern(Throwable trace) {
        return ThrowableInterner.intern(trace);
    }
}
