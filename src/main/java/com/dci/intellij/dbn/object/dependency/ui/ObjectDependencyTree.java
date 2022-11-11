package com.dci.intellij.dbn.object.dependency.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.SafeDisposer;
import com.dci.intellij.dbn.common.load.LoadInProgressRegistry;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.util.UserInterface;
import com.dci.intellij.dbn.common.ui.util.Mouse;
import com.dci.intellij.dbn.common.ui.component.DBNComponent;
import com.dci.intellij.dbn.common.ui.tree.DBNTree;
import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.common.util.TimeUtil;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.DBObjectSelectionHistory;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.common.property.DBObjectProperty;
import com.dci.intellij.dbn.object.dependency.ObjectDependencyManager;
import com.dci.intellij.dbn.object.dependency.ObjectDependencyType;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.vfs.DatabaseFileSystem;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPopupMenu;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

public class ObjectDependencyTree extends DBNTree{
    private final DBObjectSelectionHistory selectionHistory =  new DBObjectSelectionHistory();
    private final ObjectDependencyTreeSpeedSearch speedSearch;

    private final LoadInProgressRegistry<ObjectDependencyTreeNode> loadInProgressRegistry =
            LoadInProgressRegistry.create(this,
                    node -> getModel().refreshLoadInProgressNode(node));

    ObjectDependencyTree(@NotNull DBNComponent parent, @NotNull DBSchemaObject schemaObject) {
        super(parent);
        Project project = getProject();
        ObjectDependencyTreeModel model = createModel(project, schemaObject);

        setModel(model);
        selectionHistory.add(schemaObject);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        setCellRenderer(new ObjectDependencyTreeCellRenderer());
        addTreeSelectionListener((TreeSelectionEvent e) -> UserInterface.repaint(ObjectDependencyTree.this));

        speedSearch = new ObjectDependencyTreeSpeedSearch(this);
        addMouseListener(Mouse.listener().onRelease(e -> releaseEvent(e)));
    }

    private void releaseEvent(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            TreePath path = getPathForLocation(e.getX(), e.getY());
            if (path != null) {
                ObjectDependencyTreeNode node = (ObjectDependencyTreeNode) path.getLastPathComponent();
                if (node != null) {
                    DefaultActionGroup actionGroup = new DefaultActionGroup();
                    ObjectDependencyTreeNode rootNode = node.getModel().getRoot();
                    DBObject object = node.getObject();
                    if (object instanceof DBSchemaObject && !Commons.match(rootNode.getObject(), object)) {
                        actionGroup.add(new SelectObjectAction((DBSchemaObject) object));
                        DBSchemaObject schObject = (DBSchemaObject) object;
                        if (schObject.is(DBObjectProperty.EDITABLE)) {
                            actionGroup.add(new EditObjectAction((DBSchemaObject) object));
                        }
                        ActionPopupMenu actionPopupMenu = Actions.createActionPopupMenu(ObjectDependencyTree.this, "", actionGroup);
                        JPopupMenu popupMenu = actionPopupMenu.getComponent();
                        Dispatch.run(() -> {
                            if (isShowing()) {
                                popupMenu.show(ObjectDependencyTree.this, e.getX(), e.getY());
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void setModel(TreeModel model) {
        if (model instanceof ObjectDependencyTreeModel) {
            ObjectDependencyTreeModel treeModel = (ObjectDependencyTreeModel) model;
            treeModel.setTree(this);
            super.setModel(treeModel);
        }
    }

    @NotNull
    private static ObjectDependencyTreeModel createModel(Project project, DBSchemaObject schemaObject) {
        ObjectDependencyManager dependencyManager = ObjectDependencyManager.getInstance(project);
        ObjectDependencyType dependencyType = dependencyManager.getLastUserDependencyType();
        return new ObjectDependencyTreeModel(schemaObject, dependencyType);
    }

    private long selectionTimestamp = System.currentTimeMillis();
    @Override
    protected void processMouseEvent(MouseEvent e) {
        int button = e.getButton();
        int clickCount = e.getClickCount();
        if (button == MouseEvent.BUTTON1) {
            if (e.isControlDown()) {
                if (clickCount == 1) {
                    DBObject object = getMouseEventObject(e);
                    if (object != null) {
                        object.navigate(true);
                        e.consume();
                    }
                }
            } else if (clickCount == 2) {
                DBObject object = getMouseEventObject(e);
                if (object != null && TimeUtil.isOlderThan(selectionTimestamp, TimeUtil.Millis.ONE_SECOND)) {
                    selectionTimestamp = System.currentTimeMillis();

                    if (object instanceof DBSchemaObject) {
                        DBSchemaObject schemaObject = (DBSchemaObject) object;
                        if (schemaObject.is(DBObjectProperty.EDITABLE)) {
                            DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
                            databaseFileSystem.connectAndOpenEditor(schemaObject, null, true, true);
                        }
                    }

                    //setRootObject((DBSchemaObject) object, true);
                    e.consume();
                }
            }
        }

        if (!e.isConsumed()) {
            super.processMouseEvent(e);
        }
    }

    private DBObject getMouseEventObject(MouseEvent e) {
        TreePath path = getPathForLocation(e.getX(), e.getY());
        Object lastPathComponent = path == null ? null : path.getLastPathComponent();
        if (lastPathComponent instanceof ObjectDependencyTreeNode) {
            ObjectDependencyTreeNode dependencyTreeNode = (ObjectDependencyTreeNode) lastPathComponent;
            return dependencyTreeNode.getObject();
        }
        return null;
    }

    DBObjectSelectionHistory getSelectionHistory() {
        return selectionHistory;
    }

    public void selectElement(ObjectDependencyTreeNode treeNode) {
        TreePath treePath = new TreePath(treeNode.getTreePath());
        TreeUtil.selectPath(this, treePath);
    }

    void registerLoadInProgressNode(ObjectDependencyTreeNode loadInProgressNode) {
        loadInProgressRegistry.register(loadInProgressNode);
    }

    public class SelectObjectAction extends DumbAwareAction {
        private final DBObjectRef<DBSchemaObject> objectRef;
        SelectObjectAction(DBSchemaObject object) {
            super("Select");
            objectRef = DBObjectRef.of(object);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DBSchemaObject schemaObject = DBObjectRef.get(objectRef);
            if (schemaObject != null) {
                setRootObject(schemaObject, true);
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setText("Select");
        }
    }

    public class EditObjectAction extends DumbAwareAction {
        private final DBObjectRef<DBSchemaObject> objectRef;

        EditObjectAction(DBSchemaObject object) {
            super("Edit", null, Icons.ACTION_EDIT);
            objectRef = DBObjectRef.of(object);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DBSchemaObject schemaObject = DBObjectRef.get(objectRef);
            if (schemaObject != null) {
                DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
                databaseFileSystem.connectAndOpenEditor(schemaObject, null, true, true);
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setText("Edit");
            presentation.setIcon(Icons.ACTION_EDIT);
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        boolean navigable = false;
        if (e.isControlDown() && e.getID() != MouseEvent.MOUSE_DRAGGED && !e.isConsumed()) {
            DBObject object = getMouseEventObject(e);
            if (object != null && !object.equals(getModel().getObject())) {
                navigable = true;
            }
        }

        if (navigable) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            super.processMouseMotionEvent(e);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public ObjectDependencyTreeModel getModel() {
        TreeModel model = super.getModel();
        return model instanceof ObjectDependencyTreeModel ? (ObjectDependencyTreeModel) model : null;
    }

    void setDependencyType(ObjectDependencyType dependencyType) {
        ObjectDependencyTreeModel oldModel = getModel();
        Project project = Failsafe.nn(oldModel.getProject());
        ObjectDependencyManager dependencyManager = ObjectDependencyManager.getInstance(project);
        dependencyManager.setLastUserDependencyType(dependencyType);

        DBSchemaObject object = oldModel.getObject();
        if (object != null) {
            setModel(new ObjectDependencyTreeModel(object, dependencyType));
            SafeDisposer.dispose(oldModel, false);
        }
    }

    void setRootObject(DBSchemaObject object, boolean addHistory) {
        ObjectDependencyTreeModel oldModel = getModel();
        if (addHistory) {
            selectionHistory.add(object);
        }

        ObjectDependencyType dependencyType = oldModel.getDependencyType();
        setModel(new ObjectDependencyTreeModel(object, dependencyType));
        SafeDisposer.dispose(oldModel, false);
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(selectionHistory, false);
        Disposer.dispose(speedSearch, false);
        SafeDisposer.dispose(getModel(), false);
        super.disposeInner();
    }
}
