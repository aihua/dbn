package com.dci.intellij.dbn.common.util;


public class Traceable {
    private static final ThreadLocal<Traceable> LOCAL = new ThreadLocal<Traceable>();
    private Throwable trace;

    public Traceable() {
        Traceable traceable = LOCAL.get();
        this.trace = traceable == null ? new Throwable() : new Throwable(traceable.trace);
    }

    public Throwable getTrace() {
        return trace;
    }

    public static void trace(Traceable source) {
        LOCAL.set(source);
    }
}
