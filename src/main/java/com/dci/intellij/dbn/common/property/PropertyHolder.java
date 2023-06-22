package com.dci.intellij.dbn.common.property;

public interface PropertyHolder<T extends Property> {
    boolean set(T property, boolean value);

    boolean is(T property);

    default boolean isNot(T property) {
        return !is(property);
    };

    @SuppressWarnings("unchecked")
    default boolean isOneOf(T... properties) {
        for (T property : properties) {
            if (is(property)) return true;
        }
        return false;
    }

    default void conditional(T property, Runnable runnable) {
        if(isNot(property)) {
            synchronized (this) {
                if (isNot(property)) {
                    try {
                        set(property, true);
                        runnable.run();
                    } finally {
                        set(property, false);
                    }
                }
            }
        }
    }
}
