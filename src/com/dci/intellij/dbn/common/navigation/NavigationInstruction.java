package com.dci.intellij.dbn.common.navigation;

import com.dci.intellij.dbn.common.property.Property;

public enum NavigationInstruction implements Property.IntBase {
    OPEN,
    FOCUS,
    SCROLL,
    SELECT,
    RESET;

    public static final NavigationInstruction[] VALUES = values();

    private final IntMasks masks = new IntMasks(this);

    @Override
    public IntMasks masks() {
        return masks;
    }
}
