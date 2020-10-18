package com.dci.intellij.dbn.browser.model;

import com.dci.intellij.dbn.code.sql.color.SQLTextAttributesKeys;
import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.connection.ConnectionBundle;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.GenericDatabaseElement;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public final class SimpleBrowserTreeRoot extends BrowserTreeNodeBase implements BrowserTreeNode {
    private final List<ConnectionBundle> rootChildren = new ArrayList<>();
    private final ProjectRef project;

    SimpleBrowserTreeRoot(@NotNull Project project, @Nullable ConnectionBundle connectionBundle) {
        this.project = ProjectRef.of(project);
        if (connectionBundle != null) {
            this.rootChildren.add(connectionBundle);
        }
    }

    @Override
    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @Nullable
    @Override
    public GenericDatabaseElement getParentElement() {
        return null;
    }

    /**************************************************
     *              BrowserTreeNode            *
     **************************************************/
    @Override
    public boolean isTreeStructureLoaded() {
        return true;
    }

    @Override
    public void initTreeElement() {}

    @Override
    public boolean canExpand() {
        return true;
    }

    @Override
    public int getTreeDepth() {
        return 0;
    }

    @Override
    @Nullable
    public BrowserTreeNode getParent() {
        return null;
    }

    @Override
    public List<ConnectionBundle> getChildren() {
        return rootChildren;
    }

    @Override
    public void refreshTreeChildren(@NotNull DBObjectType... objectTypes) {
        for (ConnectionBundle connectionBundle : getChildren()) {
            connectionBundle.refreshTreeChildren(objectTypes);
        }
    }

    @Override
    public void rebuildTreeChildren() {
        for (ConnectionBundle connectionBundle : getChildren()) {
            connectionBundle.rebuildTreeChildren();
        }
    }

    @Override
    public BrowserTreeNode getChildAt(int index) {
        return getChildren().get(index);
    }

    @Override
    public int getChildCount() {
        return getChildren().size();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public int getIndex(BrowserTreeNode child) {
        return getChildren().indexOf(child);
    }

    @Override
    public Icon getIcon(int flags) {
        return Icons.WINDOW_DATABASE_BROWSER;
    }

    @Override
    public String getPresentableText() {
        return "Connection Managers";
    }

    @Override
    public String getPresentableTextDetails() {
        return null;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return null;
    }

    /**************************************************
     *              GenericDatabaseElement            *
     **************************************************/

    @NotNull
    @Override
    public ConnectionId getConnectionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnectionHandler() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GenericDatabaseElement getUndisposedElement() {
        return this;
    }

   /*********************************************************
    *                    ToolTipProvider                    *
    *********************************************************/
    @Override
    public String getToolTip() {
        return null;
    }

    /*********************************************************
     *                  NavigationItem                       *
     *********************************************************/
    @NotNull
    @Override
    public String getName() {
        return CommonUtil.nvl(getPresentableText(), "Database Objects");
    }
    
    @Override
    public void navigate(boolean b) {}

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public ItemPresentation getPresentation() {
        return this;
    }

    public FileStatus getFileStatus() {
        return FileStatus.NOT_CHANGED;
    }

    /*********************************************************
     *                 ItemPresentation                      *
     *********************************************************/
    @Override
    public String getLocationString() {
        return null;
    }

    @Override
    public Icon getIcon(boolean open) {
        return getIcon(0);
    }

    public TextAttributesKey getTextAttributesKey() {
        return SQLTextAttributesKeys.IDENTIFIER;
    }


    @Override
    protected void disposeInner() {
        rootChildren.clear();
    }
}
