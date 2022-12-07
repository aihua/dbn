package com.dci.intellij.dbn.execution.common.message.ui.tree;

import com.dci.intellij.dbn.common.ref.WeakRef;
import com.dci.intellij.dbn.common.ui.tree.TreeEventType;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.execution.common.message.ui.tree.node.CompilerMessagesNode;
import com.dci.intellij.dbn.execution.common.message.ui.tree.node.ExplainPlanMessagesNode;
import com.dci.intellij.dbn.execution.common.message.ui.tree.node.StatementExecutionMessagesNode;
import com.dci.intellij.dbn.execution.compiler.CompilerMessage;
import com.dci.intellij.dbn.execution.explain.result.ExplainPlanMessage;
import com.dci.intellij.dbn.execution.statement.StatementExecutionMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.List;

public class MessagesTreeRootNode extends MessagesTreeBundleNode<MessagesTreeNode, MessagesTreeBundleNode> {
    private final WeakRef<MessagesTreeModel> treeModel;

    MessagesTreeRootNode(MessagesTreeModel treeModel) {
        super(null);
        this.treeModel = WeakRef.of(treeModel);
    }

    TreePath addExecutionMessage(StatementExecutionMessage executionMessage) {
        StatementExecutionMessagesNode execMessagesNode = null;
        for (TreeNode treeNode : getChildren()) {
            if (treeNode instanceof StatementExecutionMessagesNode) {
                execMessagesNode = (StatementExecutionMessagesNode) treeNode;
                break;
            }
        }
        if (execMessagesNode == null) {
            execMessagesNode = new StatementExecutionMessagesNode(this);
            addChild(execMessagesNode);
            getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        }

        return execMessagesNode.addExecutionMessage(executionMessage);
    }

    TreePath addExplainPlanMessage(ExplainPlanMessage explainPlanMessage) {
        ExplainPlanMessagesNode explainPlanMessagesNode = null;
        for (TreeNode treeNode : getChildren()) {
            if (treeNode instanceof ExplainPlanMessagesNode) {
                explainPlanMessagesNode = (ExplainPlanMessagesNode) treeNode;
                break;
            }
        }
        if (explainPlanMessagesNode == null) {
            explainPlanMessagesNode = new ExplainPlanMessagesNode(this);
            addChild(explainPlanMessagesNode);
            getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        }

        return explainPlanMessagesNode.addExplainPlanMessage(explainPlanMessage);
    }

    TreePath addCompilerMessage(CompilerMessage compilerMessage) {
        CompilerMessagesNode compilerMessagesNode = getCompilerMessagesNode();
        if (compilerMessagesNode == null) {
            compilerMessagesNode = new CompilerMessagesNode(this);
            addChild(compilerMessagesNode);
            getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        }
        return compilerMessagesNode.addCompilerMessage(compilerMessage);
    }

    @Nullable
    private CompilerMessagesNode getCompilerMessagesNode() {
        for (TreeNode node : getChildren()) {
            if (node instanceof CompilerMessagesNode) {
                return (CompilerMessagesNode) node;
            }
        }
        return null;
    }

    @Nullable
    private StatementExecutionMessagesNode getStatementExecutionMessagesNode() {
        for (TreeNode node : getChildren()) {
            if (node instanceof StatementExecutionMessagesNode) {
                return (StatementExecutionMessagesNode) node;
            }
        }
        return null;
    }

    @Nullable
    public TreePath getTreePath(CompilerMessage compilerMessage) {
        CompilerMessagesNode node = getCompilerMessagesNode();
        if (node != null) {
            return node.getTreePath(compilerMessage);
        }
        return null;
    }

    @Nullable
    public TreePath getTreePath(StatementExecutionMessage executionMessage) {
        StatementExecutionMessagesNode node = getStatementExecutionMessagesNode();
        if (node != null) {
            return node.getTreePath(executionMessage);
        }
        return null;
    }

    @Override
    public MessagesTreeModel getTreeModel() {
        return treeModel.ensure();
    }

    @Override
    public void removeMessages(@NotNull ConnectionId connectionId) {
        super.removeMessages(connectionId);
        List<MessagesTreeBundleNode> children = getChildren();
        boolean childrenRemoved = children.removeIf(c -> c.isLeaf());
        if (childrenRemoved) {
            getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        }
    }
}
