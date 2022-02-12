package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConnectionProvider {
    @Nullable
    ConnectionHandler getConnection();

    @NotNull
    default ConnectionHandler ensureConnectionHandler() {
        return Failsafe.nn(getConnection());
    }
}
