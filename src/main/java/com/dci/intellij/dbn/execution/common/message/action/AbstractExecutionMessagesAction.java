package com.dci.intellij.dbn.execution.common.message.action;

import com.dci.intellij.dbn.common.action.ContextAction;
import com.dci.intellij.dbn.common.ref.WeakRef;
import com.dci.intellij.dbn.execution.common.message.ui.tree.MessagesTree;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

abstract class AbstractExecutionMessagesAction extends ContextAction<MessagesTree> {
    private final WeakRef<MessagesTree> messagesTree;

    AbstractExecutionMessagesAction(MessagesTree messagesTree, String text, Icon icon) {
        super(text, null, icon);
        this.messagesTree = WeakRef.of(messagesTree);
    }

    @Nullable
    protected MessagesTree getTarget(@NotNull AnActionEvent e) {
        return WeakRef.get(messagesTree);
    }
}
