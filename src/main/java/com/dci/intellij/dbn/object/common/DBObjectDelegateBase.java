package com.dci.intellij.dbn.object.common;

import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import lombok.experimental.Delegate;

class DBObjectDelegateBase implements DBObject {
    protected final DBObjectRef<?> ref;

    public DBObjectDelegateBase(DBObject object) {
        this.ref = DBObjectRef.of(object);
    }

    @Delegate
    public DBObject delegate() {
        return DBObjectRef.ensure(ref);
    }
}
