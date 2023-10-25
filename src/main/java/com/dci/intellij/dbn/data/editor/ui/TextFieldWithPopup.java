package com.dci.intellij.dbn.data.editor.ui;

import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.dispose.DisposableContainers;
import com.dci.intellij.dbn.common.ui.misc.DBNButton;
import com.dci.intellij.dbn.common.ui.util.Mouse;
import com.dci.intellij.dbn.data.editor.ui.calendar.CalendarPopupProviderForm;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class TextFieldWithPopup<T extends JComponent> extends TextFieldWithButtons {
    private final JPanel buttonsPanel;

    private final List<TextFieldPopupProvider> popupProviders = DisposableContainers.list(this);
    private T parentComponent;

    public TextFieldWithPopup() {
        this(null);
    }

    public TextFieldWithPopup(Project project) {
        this(project, null);

    }
    public TextFieldWithPopup(Project project, @Nullable T parentComponent) {
        super(project);
        this.parentComponent = parentComponent;

        JTextField textField = getTextField();
        Dimension preferredSize = textField.getPreferredSize();
        Dimension maximumSize = new Dimension((int) preferredSize.getWidth(), (int) preferredSize.getHeight());

        buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        buttonsPanel.setMaximumSize(maximumSize);
        add(buttonsPanel, BorderLayout.EAST);

        textField.addKeyListener(keyListener);
        textField.addFocusListener(focusListener);

        customizeTextField(super.getTextField());
    }

    @Nullable
    public T getParentComponent() {
        return parentComponent;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        popupProviders
                .stream()
                .map(p -> p.getButton())
                .filter(b -> b != null)
                .forEach(b -> b.setVisible(enabled));
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        popupProviders
                .stream()
                .map(p -> p.getButton())
                .filter(b -> b != null)
                .forEach(b -> b.setVisible(editable));
    }

    /******************************************************
     *                    PopupProviders                  *
     ******************************************************/
    public void createValuesListPopup(ListPopupValuesProvider valuesProvider, boolean buttonVisible) {
        ValueListPopupProvider popupProvider = new ValueListPopupProvider(this, valuesProvider, false, buttonVisible);
        addPopupProvider(popupProvider);
    }

    public void createTextEditorPopup(boolean autoPopup) {
        TextEditorPopupProviderForm popupProvider = new TextEditorPopupProviderForm(this, autoPopup);
        addPopupProvider(popupProvider);
    }

    public void createCalendarPopup(boolean autoPopup) {
        CalendarPopupProviderForm popupProviderForm = new CalendarPopupProviderForm(this, autoPopup);
        addPopupProvider(popupProviderForm);
    }

    public void createArrayEditorPopup(boolean autoPopup) {
        ArrayEditorPopupProviderForm popupProviderForm = new ArrayEditorPopupProviderForm(this, autoPopup);
        addPopupProvider(popupProviderForm);
    }

    private void addPopupProvider(TextFieldPopupProvider popupProvider) {
        popupProviders.add(popupProvider);

        if (popupProvider.isButtonVisible()) {
            Icon buttonIcon = popupProvider.getButtonIcon();
            DBNButton button = new DBNButton(buttonIcon);

            String toolTipText = "Open " + popupProvider.getDescription();
            String keyShortcutDescription = popupProvider.getKeyShortcutDescription();
            if (keyShortcutDescription != null) {
                toolTipText += " (" + keyShortcutDescription + ')';
            }
            button.setToolTipText(toolTipText);

            button.addMouseListener(Mouse.listener().onClick(e -> showPopup(popupProvider)));

            int index = buttonsPanel.getComponentCount();
            buttonsPanel.add(button, index);
            customizeButton(button);
            popupProvider.setButton(button);
            Colors.subscribe(this, () -> customizeButton(button));
        }
    }

    private void showPopup(TextFieldPopupProvider popupProvider) {
        getTextField().requestFocus();
        TextFieldPopupProvider activePopupProvider = getActivePopupProvider();
        if (activePopupProvider == null || activePopupProvider != popupProvider) {
            hideActivePopup();
            popupProvider.showPopup();
        }
    }

    public void setPopupEnabled(TextFieldPopupType popupType, boolean enabled) {
        for (TextFieldPopupProvider popupProvider : popupProviders) {
            if (popupProvider.getPopupType() == popupType) {
                popupProvider.setEnabled(enabled);
                JLabel button = popupProvider.getButton();
                if (button != null) {
                    button.setVisible(enabled);
                }
                break;
            }
        }
    }

    public void hideActivePopup() {
        TextFieldPopupProvider popupProvider = getActivePopupProvider();
        if ( popupProvider != null) {
             popupProvider.hidePopup();
        }
    }

    public TextFieldPopupProvider getAutoPopupProvider() {
        return popupProviders.stream().filter(p -> p.isAutoPopup()).findFirst().orElse(null);
    }

    private TextFieldPopupProvider getDefaultPopupProvider() {
        return popupProviders.get(0);
    }

    public TextFieldPopupProvider getActivePopupProvider() {
        return popupProviders.stream().filter(p -> p.isShowingPopup()).findFirst().orElse(null);
    }

    public TextFieldPopupProvider getPopupProvider(KeyEvent keyEvent) {
        return popupProviders.stream().filter(p -> p.matchesKeyEvent(keyEvent)).findFirst().orElse(null);
    }

    /********************************************************
     *                    FocusListener                     *
     ********************************************************/
    private final FocusListener focusListener = new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent focusEvent) {
            TextFieldPopupProvider popupProvider = getActivePopupProvider();
            if (popupProvider != null) {
                popupProvider.handleFocusLostEvent(focusEvent);
            }
        }
    };

    /********************************************************
     *                      KeyListener                     *
     ********************************************************/
    private final KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent keyEvent) {
            TextFieldPopupProvider popupProvider = getActivePopupProvider();
            if (popupProvider != null) {
                popupProvider.handleKeyPressedEvent(keyEvent);

            } else {
                popupProvider = getPopupProvider(keyEvent);
                if (popupProvider != null && popupProvider.isEnabled()) {
                    hideActivePopup();
                    popupProvider.showPopup();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent keyEvent) {
            TextFieldPopupProvider popupProvider = getActivePopupProvider();
            if (popupProvider != null) {
                popupProvider.handleKeyReleasedEvent(keyEvent);

            }
        }
    };
    /********************************************************
     *                    ActionListener                    *
     ********************************************************/
    private final ActionListener actionListener = e -> {
        TextFieldPopupProvider defaultPopupProvider = getDefaultPopupProvider();
        TextFieldPopupProvider popupProvider = getActivePopupProvider();
        if (popupProvider == null || popupProvider != defaultPopupProvider) {
            hideActivePopup();
            defaultPopupProvider.showPopup();
        }
    };


    @Override
    protected void disposeInner() {
        parentComponent = null;
    }
}
