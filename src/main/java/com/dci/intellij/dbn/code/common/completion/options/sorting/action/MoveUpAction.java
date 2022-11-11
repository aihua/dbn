package com.dci.intellij.dbn.code.common.completion.options.sorting.action;

import com.dci.intellij.dbn.code.common.completion.options.sorting.CodeCompletionSortingSettings;
import com.dci.intellij.dbn.common.Icons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.ListUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MoveUpAction extends AnAction {
    private final CodeCompletionSortingSettings settings;
    private final JList list;

    public MoveUpAction(JList list, CodeCompletionSortingSettings settings)  {
        super("Move Up", null, Icons.ACTION_MOVE_UP);
        this.list = list;
        this.settings = settings;
    }

    @Override
    public void update(AnActionEvent e) {
        int[] indices = list.getSelectedIndices();
        boolean enabled =
                list.isEnabled() &&
                indices.length > 0 &&
                indices[0] > 0;
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ListUtil.moveSelectedItemsUp(list);
        settings.setModified(true);
    }
}
