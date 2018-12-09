package com.dci.intellij.dbn.common.thread;

import com.dci.intellij.dbn.common.property.Property;

public enum TaskInstruction implements Property {
    START_IN_BACKGROUND,
    CANCELLABLE,
    MANAGED;

    private final int index = Property.idx(this);

    @Override
    public int index() {
        return index;
    }
}
