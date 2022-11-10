package com.dci.intellij.dbn.data.find.action;

import com.dci.intellij.dbn.data.find.DataSearchComponent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public class PrevOccurrenceAction extends DataSearchHeaderAction implements DumbAware {

    public PrevOccurrenceAction(DataSearchComponent searchComponent, JComponent component, boolean isSearchComponent) {
        super(searchComponent);

        ActionManager actionManager = ActionManager.getInstance();
        copyFrom(actionManager.getAction(IdeActions.ACTION_PREVIOUS_OCCURENCE));
        Set<Shortcut> shortcuts = new HashSet<>();
        ContainerUtil.addAll(shortcuts, actionManager.getAction(IdeActions.ACTION_FIND_PREVIOUS).getShortcutSet().getShortcuts());

        if (isSearchComponent) {
            ContainerUtil.addAll(shortcuts, actionManager.getAction(IdeActions.ACTION_EDITOR_MOVE_CARET_UP).getShortcutSet().getShortcuts());
            shortcuts.add(new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), null));
        }
        registerShortcutsToComponent(shortcuts, this, component);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        getSearchComponent().searchBackward();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean hasMatches = getSearchComponent().hasMatches();
        e.getPresentation().setEnabled(hasMatches);
    }
}
