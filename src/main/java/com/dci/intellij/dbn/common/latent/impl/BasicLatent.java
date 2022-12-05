package com.dci.intellij.dbn.common.latent.impl;

import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.latent.Loader;

public class BasicLatent<T> implements Latent<T> {
    private final Loader<T> loader;
    private T value;
    private volatile boolean loaded;

    public BasicLatent(Loader<T> loader) {
        this.loader = loader;
    }

    public final T get(){
        if (shouldLoad()) {
            synchronized (this) {
                if (shouldLoad()) {
                    beforeLoad();
                    T newValue = loader == null ? null : loader.load();
                    if (value != newValue) {
                        value = newValue;
                    }
                    afterLoad(newValue);
                }
            }
        }
        return value;
    }

    protected boolean shouldLoad() {
        return !loaded;
    }

    protected void beforeLoad() {};

    protected void afterLoad(T value) {
        loaded = true;
    }

    public final void set(T value) {
        this.value = value;
        loaded = true;
    }

    public final boolean loaded() {
        return loaded;
    }

    @Override
    public T value() {
        return value;
    }

    public void reset() {
        value = null;
        loaded = false;
    }
}
