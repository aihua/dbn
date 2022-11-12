package com.dci.intellij.dbn.common.component;

import com.dci.intellij.dbn.common.dispose.Nullifier;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.NamedComponent;
import com.intellij.openapi.util.Disposer;

public interface Service extends NamedComponent, Disposable {

    @Override
    default void dispose() {
        Disposer.dispose(this);
        Nullifier.nullify(this);
    }
}