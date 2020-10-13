package com.dci.intellij.dbn.execution.common.message.ui.tree;

import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.message.MessageType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.List;

public interface MessagesTreeNode<P extends MessagesTreeNode, C extends MessagesTreeNode> extends TreeNode, StatefulDisposable {
    P getParent();

    default MessagesTreeModel getTreeModel() {
        return getParent().getTreeModel();
    }

    @Nullable
    default VirtualFile getVirtualFile() {return null;}

    default List<C> getChildren() {return Collections.emptyList();}

    default boolean hasMessageChildren(MessageType type) {return false;}
}
