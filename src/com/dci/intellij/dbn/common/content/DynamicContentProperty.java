package com.dci.intellij.dbn.common.content;

import com.dci.intellij.dbn.common.property.Property;

public enum DynamicContentProperty implements Property.IntBase {
    MASTER,
    MUTABLE,
    VIRTUAL,
    INTERNAL,
    HIDDEN,
    PASSIVE,
    GROUPED,
    DEPENDENCY,

    DIRTY,
    ERROR,
    LOADED,
    DISPOSED,

    CHANGING,
    REFRESHING,
    LOADING,
    LOADING_IN_BACKGROUND,

    SEARCHABLE
    ;

    public static final DynamicContentProperty[] VALUES = values();

    private final IntMasks masks = new IntMasks(this);

    @Override
    public IntMasks masks() {
        return masks;
    }
}
