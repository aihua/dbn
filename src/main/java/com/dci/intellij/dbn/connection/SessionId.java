package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.constant.PseudoConstant;
import com.dci.intellij.dbn.common.constant.PseudoConstantConverter;

import javax.swing.*;
import java.util.UUID;

public final class SessionId extends PseudoConstant<SessionId> {

    public static final SessionId MAIN = get("MAIN");
    public static final SessionId POOL = get("POOL");
    public static final SessionId TEST = get("TEST");
    public static final SessionId DEBUG = get("DEBUG");
    public static final SessionId DEBUGGER = get("DEBUGGER");

    public SessionId(String id) {
        super(id);
    }

    public static SessionId get(String id) {
        return get(SessionId.class, id);
    }

    public static SessionId create() {
        return SessionId.get(UUID.randomUUID().toString());
    }

    public static class Converter extends PseudoConstantConverter<SessionId> {
        public Converter() {
            super(SessionId.class);
        }
    }

    public ConnectionType getConnectionType() {
        if (this == MAIN) return ConnectionType.MAIN;
        if (this == POOL) return ConnectionType.POOL;
        if (this == TEST) return ConnectionType.TEST;
        if (this == DEBUG) return ConnectionType.DEBUG;
        if (this == DEBUGGER) return ConnectionType.DEBUGGER;
        return ConnectionType.SESSION;
    }

    public Icon getIcon() {
        if (this == MAIN) return Icons.SESSION_MAIN;
        if (this == POOL) return Icons.SESSION_POOL;
        if (this == DEBUG) return Icons.SESSION_DEBUG;
        if (this == DEBUGGER) return Icons.SESSION_DEBUG;
        return Icons.SESSION_CUSTOM;
    }
}
