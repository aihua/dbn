package com.dci.intellij.dbn.data.editor.ui;

import com.dci.intellij.dbn.common.dispose.StatefulDisposable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public interface DataEditorComponent extends StatefulDisposable {

    JTextField getTextField();

    void setEditable(boolean editable);

    boolean isEditable();

    void setEnabled(boolean enabled);

    boolean isEnabled();

    <T> UserValueHolder<T> getUserValueHolder();

    <T> void setUserValueHolder(UserValueHolder<T> userValueHolder);

    String getText();

    void setText(String text);

    void setFont(Font font);

    void setBorder(Border border);

    default void afterUpdate() {}
}
