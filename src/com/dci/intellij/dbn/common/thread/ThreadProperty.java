package com.dci.intellij.dbn.common.thread;

import com.dci.intellij.dbn.common.property.Property;

public enum ThreadProperty implements Property{
    CODE_COMPLETION (true),
    CODE_ANNOTATING (true),

    TIMEOUT    (true),
    CANCELABLE (true),
    BACKGROUND (false),
    PROGRESS   (false),
    MODAL      (false)

;
    boolean propagatable;

    ThreadProperty(boolean propagatable) {
        this.propagatable = propagatable;
    }

    public boolean propagatable() {
        return propagatable;
    }
}
