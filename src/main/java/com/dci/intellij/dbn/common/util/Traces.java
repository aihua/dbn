package com.dci.intellij.dbn.common.util;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.thread.*;
import com.dci.intellij.dbn.diagnostics.Diagnostics;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Objects;

import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;

@UtilityClass
public final class Traces {

    public static boolean isCalledThrough(Class ... oneOfClasses) {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        try {
            for (int i = 3; i < callStack.length; i++) {
                StackTraceElement stackTraceElement = callStack[i];
                String className = stackTraceElement.getClassName();
                for (Class clazz : oneOfClasses) {
                    if (Objects.equals(clazz.getName(), className) /*|| clazz.isAssignableFrom(Class.forName(className))*/) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            conditionallyLog(e);
            return false;
        }
        return false;
    }

    public static boolean isCalledThrough(Class clazz, String methodName) {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        try {
            for (int i = 3; i < callStack.length; i++) {
                StackTraceElement stackTraceElement = callStack[i];
                String className = stackTraceElement.getClassName();
                if (Objects.equals(clazz.getName(), className) /*|| clazz.isAssignableFrom(Class.forName(className))*/) {
                    String methName = stackTraceElement.getMethodName();
                    if (Objects.equals(methodName, methName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            conditionallyLog(e);
            return false;
        }
        return false;
    }

    public static StackTraceElement[] diagnosticsCallStack() {
        if (!Diagnostics.isDeveloperMode()) return null;

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return Arrays
                .stream(stackTrace)
                .filter(st -> !Arrays.asList(
                        Traces.class.getName(),
                        ThreadInfo.class.getName(),
                        ThreadMonitor.class.getName(),
                        Synchronized.class.getName(),
                        Background.class.getName(),
                        Progress.class.getName(),
                        Failsafe.class.getName()
                ).contains(st.getClassName()))
                .toArray(StackTraceElement[]::new);
    }
}
