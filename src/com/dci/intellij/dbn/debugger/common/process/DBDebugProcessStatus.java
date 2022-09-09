package com.dci.intellij.dbn.debugger.common.process;

import com.dci.intellij.dbn.common.property.Property;

public enum DBDebugProcessStatus implements Property.IntBase {
    BREAKPOINT_SETTING_ALLOWED,
    TARGET_EXECUTION_STARTED,
    TARGET_EXECUTION_TERMINATED,
    TARGET_EXECUTION_THREW_EXCEPTION,
    SESSION_INITIALIZATION_THREW_EXCEPTION,
    PROCESS_TERMINATING,
    PROCESS_TERMINATED,
    PROCESS_STOPPED_NORMALLY,
    DEBUGGER_STOPPING;

    public static final DBDebugProcessStatus[] VALUES = values();

    private final IntMasks masks = new IntMasks(this);

    @Override
    public IntMasks masks() {
        return masks;
    }
}
