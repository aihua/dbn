package com.dci.intellij.dbn.common.ui.dialog;

import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.Presentable;
import com.dci.intellij.dbn.common.ui.util.Listeners;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

@Getter
@Setter
public class OptionsDialog<O extends Presentable> extends DBNDialog<OptionsDialogForm>{
    private final O[] options;
    private final String[] actionNames;
    private final String optionLabel;
    private Action[] actions;
    private O selectedOption;

    private final Listeners<OptionDialogActionListener<O>> listeners = Listeners.create(this);

    protected OptionsDialog(Project project, String dialogTitle, String optionLabel, O[] options, O selectedOption, String[] actionNames) {
        super(project, dialogTitle, false);
        this.optionLabel = optionLabel;
        this.options = options;
        this.actionNames = actionNames;
        this.selectedOption = selectedOption;

        setDefaultSize(600, 300);
        //setResizable(false);
        init();
        setActionsEnabled(selectedOption != null);
    }

    @Override
    protected String getDimensionServiceKey() {
        //return super.getDimensionServiceKey() + "." + options[0].getClass().getSimpleName();
        return null;
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        actions = new Action[this.actionNames.length + 1];
        String[] strings = this.actionNames;
        for (int i = 0; i < strings.length; i++) {
            String action = strings[i];
            actions[i] = createAction(action, i);
        }
        actions[this.actionNames.length] = getCancelAction();
        return actions;
    }

    @NotNull
    private Action createAction(String name, int index) {
        AbstractAction action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                listeners.notify(l -> l.actionPerformed(index, selectedOption));
                close(index);
            }
        };
        return action;
    }

    public void setActionsEnabled(boolean enabled) {
        for (Action action : actions) {
            if (action == getCancelAction()) continue;
            action.setEnabled(enabled);
        }
    }

    public void addActionListener(OptionDialogActionListener<O> actionListener) {
        listeners.add(actionListener);
    }

    @NotNull
    @Override
    protected OptionsDialogForm<O> createForm() {
        return new OptionsDialogForm<>(this);
    }

    public static <O extends Presentable> void open(
            Project project,
            String dialogTitle,
            String optionLabel,
            O[] options,
            O selectedOption,
            String[] actions,
            OptionDialogActionListener<O> actionListener) {
        Dispatch.run(() -> {
            OptionsDialog<O> optionsDialog = new OptionsDialog<>(project, dialogTitle, optionLabel, options, selectedOption, actions);
            optionsDialog.addActionListener(actionListener);
            optionsDialog.show();
        });
    }

}

