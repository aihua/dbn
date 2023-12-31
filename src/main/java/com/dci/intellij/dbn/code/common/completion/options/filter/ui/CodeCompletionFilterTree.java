package com.dci.intellij.dbn.code.common.completion.options.filter.ui;

import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.ui.tree.DBNTreeTransferHandler;
import com.dci.intellij.dbn.common.ui.tree.Trees;
import com.intellij.ui.CheckboxTree;

import javax.swing.tree.TreeNode;

public class CodeCompletionFilterTree extends CheckboxTree {

    public CodeCompletionFilterTree(CodeCompletionFilterTreeModel model) {
        super(new CodeCompletionFilterTreeCellRenderer(), null);
        setModel(model);
        setRootVisible(true);
        TreeNode expandedTreeNode = (TreeNode) getModel().getChild(getModel().getRoot(), 5);
        setExpandedState(Trees.createTreePath(expandedTreeNode), true);
        installSpeedSearch();
        setTransferHandler(DBNTreeTransferHandler.INSTANCE);
        setBackground(Colors.getTextFieldBackground());
    }
}
