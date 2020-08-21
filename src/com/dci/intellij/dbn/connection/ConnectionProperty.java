package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.common.property.Property;

public enum ConnectionProperty implements Property {
    RS_TYPE_SCROLL_INSENSITIVE,
    RS_TYPE_FORWARD_ONLY;


    private final int index = Property.idx(this);

    @Override
    public int index() {
        return index;
    }

}
