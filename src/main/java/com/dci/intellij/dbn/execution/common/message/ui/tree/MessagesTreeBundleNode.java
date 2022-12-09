package com.dci.intellij.dbn.execution.common.message.ui.tree;

import com.dci.intellij.dbn.common.dispose.Disposed;
import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.message.MessageType;
import com.dci.intellij.dbn.common.ui.tree.TreeEventType;
import com.dci.intellij.dbn.common.util.CollectionUtil;
import com.dci.intellij.dbn.connection.ConnectionId;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static com.dci.intellij.dbn.common.dispose.Disposer.replace;

public abstract class MessagesTreeBundleNode<P extends MessagesTreeNode, C extends MessagesTreeNode>
        extends MessagesTreeNodeBase<P, C> {

    private List<C> children = CollectionUtil.createConcurrentList();

    protected MessagesTreeBundleNode(P parent) {
        super(parent);
    }

    public void addChild(C child) {
        children.add(child);
    }

    protected void clearChildren() {
        children = replace(children, CollectionUtil.createConcurrentList());
    }

    @Override
    public List<C> getChildren() {
        return children;
    }

    /*********************************************************
     *                        TreeNode                       *
     *********************************************************/
    @Override
    public TreeNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return children.size() == 0;
    }

    @Override
    public void removeMessages(@NotNull ConnectionId connectionId) {
        List<C> removeCandidates = new ArrayList<>();
        for (C child : this.children) {
            child.removeMessages(connectionId);
            if (child.getConnectionId() == connectionId) {
                removeCandidates.add(child);
            }
        }

        for (C child : removeCandidates) {
            this.children.remove(child);
            Disposer.dispose(child);
        }

        if (!removeCandidates.isEmpty()) {
            getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        }
    }

    @Override
    public Enumeration<C> children() {
        return java.util.Collections.enumeration(children);
    }

    @Override
    public boolean hasMessageChildren(MessageType type) {
        for (C child : children) {
            if (child instanceof MessagesTreeLeafNode) {
                MessagesTreeLeafNode messageTreeNode = (MessagesTreeLeafNode) child;
                if (messageTreeNode.getMessage().getType() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    /*********************************************************
     *                      Disposable                       *
     *********************************************************/
    @Override
    public void disposeInner() {
        children = replace(children, Disposed.list());
        nullify();
    }


}
