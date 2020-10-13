package com.dci.intellij.dbn.object.dependency.ui;

import com.dci.intellij.dbn.common.dispose.AlreadyDisposedException;
import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.ui.tree.TreeEventType;
import com.dci.intellij.dbn.common.ui.tree.TreeUtil;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.dependency.ObjectDependencyType;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ObjectDependencyTreeModel extends StatefulDisposable.Base implements TreeModel {
    private final Set<TreeModelListener> listeners = new HashSet<>();

    private ObjectDependencyTreeNode root;
    private final ObjectDependencyType dependencyType;
    private final DBObjectRef<DBSchemaObject> object;

    private WeakRef<ObjectDependencyTree> tree;

    ObjectDependencyTreeModel(DBSchemaObject object, ObjectDependencyType dependencyType) {
        this.object = DBObjectRef.of(object);
        this.root = new ObjectDependencyTreeNode(this, object);
        this.dependencyType = dependencyType;

        Disposer.register(this, root);
    }

    public DBSchemaObject getObject() {
        return DBObjectRef.get(object);
    }

    public void setTree(ObjectDependencyTree tree) {
        this.tree = WeakRef.of(tree);
    }

    @NotNull
    public ObjectDependencyTree getTree() {
        return tree.ensure();
    }

    @Nullable
    public Project getProject() {
        return getTree().getProject();
    }

    ObjectDependencyType getDependencyType() {
        return dependencyType;
    }

    @Override
    @NotNull
    public ObjectDependencyTreeNode getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        List<ObjectDependencyTreeNode> children = getChildren(parent);
        if (children.size() <= index) throw AlreadyDisposedException.INSTANCE;
        return children.get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return getChildren(parent).size();
    }

    @Override
    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return getChildren(parent).indexOf(child);
    }

    private List<ObjectDependencyTreeNode> getChildren(Object parent) {
        ObjectDependencyTreeNode parentNode = (ObjectDependencyTreeNode) parent;
        return parentNode.getChildren(true);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {

    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    void refreshLoadInProgressNode(ObjectDependencyTreeNode node) {
        TreePath treePath = new TreePath(node.getTreePath());
        TreeUtil.notifyTreeModelListeners(node, listeners, treePath, TreeEventType.STRUCTURE_CHANGED);
    }

    void notifyNodeLoaded(ObjectDependencyTreeNode node) {
        TreePath treePath = new TreePath(node.getTreePath());
        TreeUtil.notifyTreeModelListeners(node, listeners, treePath, TreeEventType.STRUCTURE_CHANGED);
    }

    @Override
    public void disposeInner() {
        root = null;
        listeners.clear();
        nullify();
    }
}
