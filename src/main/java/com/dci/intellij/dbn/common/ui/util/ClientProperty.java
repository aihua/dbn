package com.dci.intellij.dbn.common.ui.util;

import javax.swing.*;
import java.awt.*;

import static com.dci.intellij.dbn.common.util.Unsafe.cast;

public enum ClientProperty {
    REGULAR_SPLITTER,
    BORDER,
    BORDERLESS,
    REGISTERED;


    public boolean is(Component component) {
        Boolean value = get(component);
        return value != null && value;
    }

    public boolean isNot(Component component) {
        return !is(component);
    }

    public <T> T get(Component component) {
        if (component instanceof JComponent) {
            JComponent comp = (JComponent) component;
            return cast(comp.getClientProperty(this));
        }
        return null;
    }

    public <T> void set(Component component, T value) {
        if (component instanceof JComponent) {
            JComponent comp = (JComponent) component;
            comp.putClientProperty(this, value);
        }
    }

    public boolean isSet(Component component) {
        return get(component) != null;
    }

    public boolean isNotSet(Component component) {
        return !isSet(component);
    }

}
