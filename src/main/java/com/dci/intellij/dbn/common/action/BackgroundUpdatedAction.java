package com.dci.intellij.dbn.common.action;

import com.dci.intellij.dbn.common.compatibility.Compatibility;

@Compatibility
public interface BackgroundUpdatedAction {

    @Compatibility
    default boolean isUpdateInBackground() {
        return true;
    }

}
