package com.dci.intellij.dbn.object.common;

import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.browser.model.BrowserTreeNodeBase;
import com.dci.intellij.dbn.code.common.lookup.LookupItemBuilder;
import com.dci.intellij.dbn.common.lookup.ConsumerStoppedException;
import com.dci.intellij.dbn.common.lookup.LookupConsumer;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.GenericDatabaseElement;
import com.dci.intellij.dbn.connection.VirtualConnectionHandler;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.type.DBNativeDataType;
import com.dci.intellij.dbn.database.DatabaseObjectIdentifier;
import com.dci.intellij.dbn.language.common.DBLanguage;
import com.dci.intellij.dbn.object.DBCharset;
import com.dci.intellij.dbn.object.DBPrivilege;
import com.dci.intellij.dbn.object.DBRole;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.DBSystemPrivilege;
import com.dci.intellij.dbn.object.DBUser;
import com.dci.intellij.dbn.object.common.list.DBObjectListContainer;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.dci.intellij.dbn.vfs.file.DBObjectVirtualFile;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class DBVirtualObjectBundle extends BrowserTreeNodeBase implements DBObjectBundle{
    private VirtualConnectionHandler connectionHandler;

    public DBVirtualObjectBundle(@NotNull VirtualConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    @Override
    public List<DBSchema> getSchemas() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public List<DBUser> getUsers() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public List<DBRole> getRoles() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public List<DBSystemPrivilege> getSystemPrivileges() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public List<DBCharset> getCharsets() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<DBNativeDataType> getNativeDataTypes() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public DBNativeDataType getNativeDataType(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBSchema getSchema(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBSchema getPublicSchema() {
        return null;
    }

    @Nullable
    @Override
    public DBSchema getUserSchema() {
        return null;
    }

    @Nullable
    @Override
    public DBUser getUser(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBRole getRole(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBPrivilege getPrivilege(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBSystemPrivilege getSystemPrivilege(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBCharset getCharset(String name) {
        return null;
    }

    @NotNull
    @Override
    public List<DBDataType> getCachedDataTypes() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public DBObject getObject(DatabaseObjectIdentifier objectIdentifier) {
        return null;
    }

    @Nullable
    @Override
    public DBObject getObject(DBObjectType objectType, String name) {
        return null;
    }

    @Nullable
    @Override
    public DBObject getObject(DBObjectType objectType, String name, short overload) {
        return null;
    }

    @Override
    public void lookupObjectsOfType(LookupConsumer consumer, DBObjectType objectType) throws ConsumerStoppedException {

    }

    @Override
    public void lookupChildObjectsOfType(LookupConsumer consumer, DBObject parentObject, DBObjectType objectType, ObjectTypeFilter filter, DBSchema currentSchema) throws ConsumerStoppedException {

    }

    @Override
    public void refreshObjectsStatus(DBSchemaObject requester) {

    }

    @Override
    public DBObjectListContainer getObjectListContainer() {
        return null;
    }

    @Override
    public LookupItemBuilder getLookupItemBuilder(DBObjectRef<?> objectRef, DBLanguage<?> language) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DBObjectPsiFacade getObjectPsiFacade(DBObjectRef<?> objectRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DBObjectVirtualFile<?> getObjectVirtualFile(DBObjectRef<?> objectRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @NotNull
    @Override
    public ConnectionId getConnectionId() {
        return connectionHandler.getConnectionId();
    }

    @NotNull
    @Override
    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    @Override
    public void initTreeElement() {

    }

    @Override
    public boolean canExpand() {
        return false;
    }

    @Override
    public int getTreeDepth() {
        return 0;
    }

    @Override
    public boolean isTreeStructureLoaded() {
        return false;
    }

    @Override
    public BrowserTreeNode getChildAt(int index) {
        return null;
    }

    @Nullable
    @Override
    public BrowserTreeNode getParent() {
        return null;
    }

    @Override
    public List<? extends BrowserTreeNode> getChildren() {
        return null;
    }

    @Override
    public void refreshTreeChildren(@NotNull DBObjectType... objectTypes) {

    }

    @Override
    public void rebuildTreeChildren() {

    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public int getIndex(BrowserTreeNode child) {
        return 0;
    }

    @Override
    public Icon getIcon(int flags) {
        return null;
    }

    @Override
    public String getPresentableText() {
        return null;
    }

    @Nullable
    @Override
    public String getLocationString() {
        return null;
    }

    @Nullable
    @Override
    public Icon getIcon(boolean unused) {
        return null;
    }

    @Override
    public String getPresentableTextDetails() {
        return null;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return null;
    }

    @NotNull
    @Override
    public Project getProject() {
        return connectionHandler.getProject();
    }

    @Nullable
    @Override
    public GenericDatabaseElement getParentElement() {
        return null;
    }

    @Override
    public GenericDatabaseElement getUndisposedElement() {
        return null;
    }

    @NotNull
    @Override
    public String getName() {
        return "";
    }

    @Nullable
    @Override
    public ItemPresentation getPresentation() {
        return null;
    }

    @Override
    public void navigate(boolean requestFocus) {

    }

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public String getToolTip() {
        return null;
    }

    @Override
    public PsiFile getFakeObjectFile() {
        return null;
    }

    @Override
    protected void disposeInner() {

    }
}
