package com.dci.intellij.dbn.data.editor.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.ref.WeakRef;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.util.Keyboard;
import com.dci.intellij.dbn.common.ui.util.Popups;
import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.common.util.Context;
import com.dci.intellij.dbn.common.util.Strings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static com.dci.intellij.dbn.common.dispose.Disposer.replace;
import static com.dci.intellij.dbn.common.util.Strings.nonEmptyStrings;

@Getter
@Setter
public class ValueListPopupProvider implements TextFieldPopupProvider{
    private final WeakRef<TextFieldWithPopup> editorComponent;
    private final ListPopupValuesProvider valuesProvider;

    private final boolean autoPopup;
    private final boolean buttonVisible;
    private boolean enabled = true;
    private boolean preparing = false;

    private JLabel button;
    private transient JBPopup popup;

    ValueListPopupProvider(TextFieldWithPopup editorComponent, ListPopupValuesProvider valuesProvider, boolean autoPopup, boolean buttonVisible) {
        this.editorComponent = WeakRef.of(editorComponent);
        this.valuesProvider = valuesProvider;
        this.autoPopup = autoPopup;
        this.buttonVisible = buttonVisible;
    }

    public TextFieldWithPopup getEditorComponent() {
        return editorComponent.ensure();
    }

    @Override
    public TextFieldPopupType getPopupType() {
        return null;
    }

    @Override
    public boolean isShowingPopup() {
        return popup != null && popup.isVisible();
    }

    @Override
    public void showPopup() {
        if (!valuesProvider.isLoaded()) {
            ModalityState modalityState = ModalityState.stateForComponent(getEditorComponent());
            if (preparing) return;

            preparing = true;
            Dispatch.background(
                    null, modalityState,
                    () -> {
                        getValues();
                        getSecondaryValues();
                        valuesProvider.setLoaded(true);
                    },
                    () -> {
                        try {
                            if (!isShowingPopup()) {
                                doShowPopup();
                            }
                        } finally {
                            preparing = false;
                        }
                    });
        } else {
            doShowPopup();
        }
    }

    private void doShowPopup() {
        TextFieldWithPopup editorComponent = getEditorComponent();
        List<String> values = getValues();
        List<String> secondaryValues = getSecondaryValues();
        if (false && values.size() < 20)  {
            String[] valuesArray = values.toArray(new String[0]);
            BaseListPopupStep<String> listPopupStep = new BaseListPopupStep<String>(null, valuesArray){
                @Override
                public PopupStep onChosen(String selectedValue, boolean finalChoice) {
                    editorComponent.setText(selectedValue);
                    return FINAL_CHOICE;
                }
            };
            popup = JBPopupFactory.getInstance().createListPopup(listPopupStep);
        } else {
            DefaultActionGroup actionGroup = new DefaultActionGroup();

            for (String value : values) {
                if (Strings.isNotEmpty(value)) {
                    actionGroup.add(new ValueSelectAction(value));
                }
            }
            if (secondaryValues.size() > 0) {
                if (values.size() > 0) {
                    actionGroup.add(Actions.SEPARATOR);
                }
                for (String secondaryValue : secondaryValues) {
                    if (Strings.isNotEmpty(secondaryValue)) {
                        actionGroup.add(new ValueSelectAction(secondaryValue));
                    }
                }
            }

            popup = JBPopupFactory.getInstance().createActionGroupPopup(
                    null,
                    actionGroup,
                    Context.getDataContext(editorComponent),
                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                    true, null, 10);
        }

        Popups.showUnderneathOf(popup, editorComponent, 4, 200);
    }

    private List<String> getValues() {
        return nonEmptyStrings(valuesProvider.getValues());
    }

    private List<String> getSecondaryValues() {
        return nonEmptyStrings(valuesProvider.getSecondaryValues());
    }

    @Override
    public void hidePopup() {
        if (popup != null) {
            if (popup.isVisible()) popup.cancel();
            Disposer.dispose(popup);
        }
    }

    @Override
    public String getDescription() {
        return valuesProvider.getDescription();
    }

    @Override
    public String getKeyShortcutDescription() {
        return KeymapUtil.getShortcutsText(getShortcuts());
    }

    @Override
    public Shortcut[] getShortcuts() {
        return Keyboard.getShortcuts(IdeActions.ACTION_CODE_COMPLETION);
    }

    @Nullable
    @Override
    public Icon getButtonIcon() {
        return Icons.DATA_EDITOR_LIST;
    }

    @Override
    public void dispose() {
        popup = replace(popup, null);
    }

    private class ValueSelectAction extends AnAction {
        private final String value;

        ValueSelectAction(String value) {
            this.value = value;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            TextFieldWithPopup editorComponent = getEditorComponent();
            editorComponent.setText(value);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setText(value, false);
        }
    }
}
