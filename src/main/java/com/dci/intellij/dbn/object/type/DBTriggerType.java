package com.dci.intellij.dbn.object.type;

import com.dci.intellij.dbn.common.constant.Constant;

public enum DBTriggerType implements Constant<DBTriggerType> {
    BEFORE("before"),
    AFTER("after"),
    INSTEAD_OF("instead of"),
    UNKNOWN("unknown");

    private String name;

    DBTriggerType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
