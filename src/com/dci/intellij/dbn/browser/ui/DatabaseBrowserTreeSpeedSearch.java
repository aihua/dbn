package com.dci.intellij.dbn.browser.ui;

import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.connection.ConnectionBundle;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.DBObjectBundle;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.SpeedSearchBase;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class DatabaseBrowserTreeSpeedSearch extends SpeedSearchBase<JTree> implements StatefulDisposable {
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private final Latent<Object[]> elements = Latent.basic(() -> {
        List<BrowserTreeNode> nodes = new ArrayList<>();
        BrowserTreeNode root = getComponent().getModel().getRoot();
        loadElements(nodes, root);
        return nodes.toArray();
    });

    DatabaseBrowserTreeSpeedSearch(DatabaseBrowserTree tree) {
        super(tree);
        getComponent().getModel().addTreeModelListener(treeModelListener);

        Disposer.register(tree, this);
    }

    @Override
    protected int getSelectedIndex() {
        Object[] elements = getAllElements();
        BrowserTreeNode treeNode = getSelectedTreeElement();
        if (treeNode != null) {
            for (int i=0; i<elements.length; i++) {
                if (treeNode == elements[i]) {
                    return i;
                }
            }
        }
        return -1;
    }

    private BrowserTreeNode getSelectedTreeElement() {
        TreePath selectionPath = getComponent().getSelectionPath();
        if (selectionPath != null) {
            return (BrowserTreeNode) selectionPath.getLastPathComponent();
        }
        return null;
    }

    @NotNull
    @Override
    protected Object[] getAllElements() {
        return elements.get();
    }

    @Override
    protected int getElementCount() {
        return elements.get().length;
    }

    @Override
    protected Object getElementAt(int viewIndex) {
        return elements.get()[viewIndex];
    }

    private static void loadElements(List<BrowserTreeNode> nodes, BrowserTreeNode browserTreeNode) {
        if (browserTreeNode.isTreeStructureLoaded()) {
            if (browserTreeNode instanceof ConnectionBundle) {
                ConnectionBundle connectionBundle = (ConnectionBundle) browserTreeNode;
                for (ConnectionHandler connectionHandler : connectionBundle.getConnectionHandlers()){
                    DBObjectBundle objectBundle = connectionHandler.getObjectBundle();
                    loadElements(nodes, objectBundle);
                }
            }
            else {
                for (BrowserTreeNode treeNode : browserTreeNode.getChildren()) {
                    if (treeNode instanceof DBObject) {
                        nodes.add(treeNode);
                    }
                    loadElements(nodes, treeNode);
                }
            }
        }
    }

    @Override
    public DatabaseBrowserTree getComponent() {
        return (DatabaseBrowserTree) super.getComponent();
    }

    @Override
    protected String getElementText(Object o) {
        BrowserTreeNode treeNode = (BrowserTreeNode) o;
        return treeNode.getPresentableText();
    }

    @Override
    protected void selectElement(Object o, String s) {
        BrowserTreeNode treeNode = (BrowserTreeNode) o;
        getComponent().selectElement(treeNode, false);

/*
        TreePath treePath = DatabaseBrowserUtils.createTreePath(treeNode);
        tree.setSelectionPath(treePath);
        tree.scrollPathToVisible(treePath);
*/
    }

    private final TreeModelListener treeModelListener = new TreeModelListener() {

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            elements.reset();
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            elements.reset();
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            elements.reset();
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            elements.reset();
        }
    };

    @Getter
    private boolean disposed;


    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            getComponent().getModel().removeTreeModelListener(treeModelListener);
            elements.set(EMPTY_ARRAY);
            nullify();
        }
    }
}
