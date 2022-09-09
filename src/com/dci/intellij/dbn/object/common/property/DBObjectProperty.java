package com.dci.intellij.dbn.object.common.property;

import com.dci.intellij.dbn.common.property.Property;

public enum DBObjectProperty implements Property.LongBase {
    // generic
    TEMPORARY,
    NAVIGABLE,
    EDITABLE,
    COMPILABLE,
    DISABLEABLE,
    DEBUGABLE,
    INVALIDABLE,
    REFERENCEABLE,
    ROOT_OBJECT,
    SCHEMA_OBJECT,
    SYSTEM_OBJECT,

    DETERMINISTIC,
    COLLECTION,

    // schema
    USER_SCHEMA,
    EMPTY_SCHEMA,
    PUBLIC_SCHEMA,
    SYSTEM_SCHEMA,

    // column
    PRIMARY_KEY,
    FOREIGN_KEY,
    UNIQUE_KEY,
    IDENTITY,
    NULLABLE,
    HIDDEN,
    UNIQUE,

    // argument
    INPUT,
    OUTPUT,

    // user, privileges
    EXPIRED,
    LOCKED,
    ADMIN_OPTION,
    DEFAULT_ROLE,
    SESSION_USER,

    // trigger
    FOR_EACH_ROW,

    // other
    TREE_LOADED // belongs to DBObjectStatus (here for optimization reasons)
    ;

    public static final DBObjectProperty[] VALUES = values();

    private final LongMasks masks = new LongMasks(this);

    @Override
    public LongMasks masks() {
        return masks;
    }
}
