package com.dci.intellij.dbn.object;

import com.dci.intellij.dbn.object.common.DBObject;

public interface DBOrderedObject extends DBObject {
    short getPosition();
}