package com.dci.intellij.dbn.common.thread;

import com.dci.intellij.dbn.common.property.Property;

public enum ThreadProperty implements Property.IntBase {
    CODE_COMPLETION (true),
    CODE_ANNOTATING (true),

    TIMEOUT    (true),
    PROMPTED   (true),
    CANCELABLE (true),

    BACKGROUND (false),
    PROGRESS   (false),
    MODAL      (false),
    DISPOSER   (false)

;
    public static final ThreadProperty[] VALUES = values();

    private final IntMasks masks = new IntMasks(this);
    private final boolean propagatable;

    ThreadProperty(boolean propagatable) {
        this.propagatable = propagatable;
    }

    @Override
    public IntMasks masks() {
        return masks;
    }

    public boolean propagatable() {
        return propagatable;
    }
}
