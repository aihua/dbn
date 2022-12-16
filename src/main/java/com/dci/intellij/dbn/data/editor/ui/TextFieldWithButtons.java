package com.dci.intellij.dbn.data.editor.ui;

import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.ui.misc.DBNButton;
import com.dci.intellij.dbn.common.ui.panel.DBNPanelImpl;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.Document;
import java.awt.*;

import static com.dci.intellij.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public abstract class TextFieldWithButtons extends DBNPanelImpl implements DataEditorComponent {
    private final JTextField textField;
    private final ProjectRef project;
    private UserValueHolder<?> userValueHolder;

    public TextFieldWithButtons(Project project) {
        this.project = ProjectRef.of(project);

        setLayout(new BorderLayout());
        this.textField = new JBTextField();
        this.textField.setMargin(JBUI.insets(0, 1));

        Dimension preferredSize = textField.getPreferredSize();
        Dimension maximumSize = new Dimension((int) preferredSize.getWidth(), (int) preferredSize.getHeight());

        textField.setMaximumSize(maximumSize);
        add(textField, BorderLayout.CENTER);

    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    public void customizeTextField(JTextField textField) {}

    public void customizeButton(DBNButton button) {
        int width = (int) button.getPreferredSize().getWidth();
        int height = (int) textField.getPreferredSize().getHeight();
        button.setPreferredSize(new Dimension(width, height));
        button.setMaximumSize(new Dimension(width, height));
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (textField != null) textField.setFont(font);
    }


    public void setBorder(Border border) {
        super.setBorder(border);
    }

    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        if (textField != null) textField.setBackground(color);
    }

    @Override
    public void setEnabled(boolean enabled) {
        textField.setEditable(enabled);
    }

    public void setEditable(boolean editable){
        textField.setEditable(editable);
    }

    public boolean isEditable() {
        return textField.isEditable();
    }

    public boolean isSelected() {
        Document document = textField.getDocument();
        return document.getLength() > 0 &&
               textField.getSelectionStart() == 0 &&
               textField.getSelectionEnd() == document.getLength();
    }

    public void clearSelection() {
        if (isSelected()) {
            textField.setSelectionStart(0);
            textField.setSelectionEnd(0);
            textField.setCaretPosition(0);
        }
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public <T> UserValueHolder<T> getUserValueHolder() {
        return cast(userValueHolder);
    }

    public <T> void setUserValueHolder(UserValueHolder<T> userValueHolder) {
        this.userValueHolder = userValueHolder;
    }
}
