package com.dci.intellij.dbn.object.common;

import com.dci.intellij.dbn.browser.DatabaseBrowserManager;
import com.dci.intellij.dbn.browser.model.BrowserTreeEventListener;
import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.browser.model.BrowserTreeNodeBase;
import com.dci.intellij.dbn.browser.model.LoadInProgressTreeNode;
import com.dci.intellij.dbn.browser.ui.HtmlToolTipBuilder;
import com.dci.intellij.dbn.browser.ui.ToolTipProvider;
import com.dci.intellij.dbn.code.common.lookup.LookupItemBuilder;
import com.dci.intellij.dbn.code.sql.color.SQLTextAttributesKeys;
import com.dci.intellij.dbn.common.consumer.CancellableConsumer;
import com.dci.intellij.dbn.common.consumer.ListCollector;
import com.dci.intellij.dbn.common.content.DynamicContent;
import com.dci.intellij.dbn.common.content.DynamicContentType;
import com.dci.intellij.dbn.common.dispose.AlreadyDisposedException;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.SafeDisposer;
import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.filter.Filter;
import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.common.ui.tree.TreeEventType;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.common.util.Consumer;
import com.dci.intellij.dbn.common.util.Safe;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionHandlerRef;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.DatabaseType;
import com.dci.intellij.dbn.connection.GenericDatabaseElement;
import com.dci.intellij.dbn.connection.PooledConnection;
import com.dci.intellij.dbn.connection.ResourceUtil;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.connection.config.ConnectionDatabaseSettings;
import com.dci.intellij.dbn.connection.jdbc.DBNCallableStatement;
import com.dci.intellij.dbn.database.DatabaseCompatibilityInterface;
import com.dci.intellij.dbn.database.common.metadata.DBObjectMetadata;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.language.common.DBLanguage;
import com.dci.intellij.dbn.language.common.DBLanguageDialect;
import com.dci.intellij.dbn.language.common.QuotePair;
import com.dci.intellij.dbn.language.sql.SQLLanguage;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.DBUser;
import com.dci.intellij.dbn.object.common.list.DBObjectList;
import com.dci.intellij.dbn.object.common.list.DBObjectListContainer;
import com.dci.intellij.dbn.object.common.list.DBObjectNavigationList;
import com.dci.intellij.dbn.object.common.list.DBObjectRelationListContainer;
import com.dci.intellij.dbn.object.common.operation.DBOperationExecutor;
import com.dci.intellij.dbn.object.common.operation.DBOperationNotSupportedException;
import com.dci.intellij.dbn.object.common.property.DBObjectProperties;
import com.dci.intellij.dbn.object.common.property.DBObjectProperty;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.properties.ConnectionPresentableProperty;
import com.dci.intellij.dbn.object.properties.DBObjectPresentableProperty;
import com.dci.intellij.dbn.object.properties.PresentableProperty;
import com.dci.intellij.dbn.object.type.DBObjectRelationType;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.dci.intellij.dbn.vfs.file.DBObjectVirtualFile;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.dci.intellij.dbn.browser.DatabaseBrowserUtils.treeVisibilityChanged;
import static com.dci.intellij.dbn.common.util.CollectionUtil.compact;
import static com.dci.intellij.dbn.common.util.CollectionUtil.filter;

public abstract class DBObjectImpl<M extends DBObjectMetadata> extends BrowserTreeNodeBase implements DBObject, ToolTipProvider {

    private static final List<DBObject> EMPTY_OBJECT_LIST = java.util.Collections.unmodifiableList(new ArrayList<>(0));
    public static final List<BrowserTreeNode> EMPTY_TREE_NODE_LIST = java.util.Collections.unmodifiableList(new ArrayList<BrowserTreeNode>(0));

    private List<BrowserTreeNode> allPossibleTreeChildren;
    private List<BrowserTreeNode> visibleTreeChildren;

    protected DBObjectRef<?> objectRef;
    protected DBObjectRef<?> parentObjectRef;

    protected DBObjectProperties properties = new DBObjectProperties();
    private DBObjectListContainer childObjects;
    private DBObjectRelationListContainer childObjectRelations;

    private final ConnectionHandlerRef connectionHandler;

    private static final DBOperationExecutor NULL_OPERATION_EXECUTOR = operationType -> {
        throw new DBOperationNotSupportedException(operationType);
    };

    protected DBObjectImpl(@NotNull DBObject parentObject, M metadata) throws SQLException {
        this.connectionHandler = ConnectionHandlerRef.from(parentObject.getConnectionHandler());
        this.parentObjectRef = DBObjectRef.of(parentObject);
        init(metadata);
    }

    protected DBObjectImpl(@NotNull ConnectionHandler connectionHandler, M metadata) throws SQLException {
        this.connectionHandler = ConnectionHandlerRef.from(connectionHandler);
        init(metadata);
    }

    protected DBObjectImpl(@Nullable ConnectionHandler connectionHandler, DBObjectType objectType, String name) {
        this.connectionHandler = ConnectionHandlerRef.from(connectionHandler);
        objectRef = new DBObjectRef<>(this, objectType, name);
    }

    private void init(M metadata) throws SQLException {
        String name = initObject(metadata);
        objectRef = new DBObjectRef<>(this, name);

        initStatus(metadata);
        initProperties();
        initLists();
    }

    protected abstract String initObject(M metadata) throws SQLException;

    public void initStatus(M metadata) throws SQLException {}

    protected void initProperties() {}

    protected void initLists() {}

/*    @Override
    public PsiElement getParent() {
        PsiFile containingFile = getContainingFile();
        if (containingFile != null) {
            return containingFile.getParent();
        }
        return null;
    }*/

    @Override
    public boolean set(DBObjectProperty status, boolean value) {
        return properties.set(status, value);
    }

    @Override
    public boolean is(DBObjectProperty property) {
        return properties.is(property);
    }

    @Override
    public final DBContentType getContentType() {
        return getObjectType().getContentType();
    }

    @Override
    public DBObjectRef getRef() {
        return objectRef;
    }

    @Override
    public boolean isParentOf(DBObject object) {
        return this.equals(object.getParentObject());
    }

    @Override
    public DBOperationExecutor getOperationExecutor() {
        return NULL_OPERATION_EXECUTOR;
    }

    @Override
    public DBSchema getSchema() {
        DBObject object = this;
        while (object != null) {
            if (object instanceof DBSchema) {
                return (DBSchema) object;
            }
            object = object.getParentObject();
        }
        return null;
    }

    public SchemaId getSchemaIdentifier() {
        return SchemaId.from(getSchema());
    }

    @Override
    public DBObject getParentObject() {
        return DBObjectRef.get(parentObjectRef);
    }

    @Override
    @Nullable
    public DBObject getDefaultNavigationObject() {
        return null;
    }

    @Override
    public boolean isOfType(DBObjectType objectType) {
        return getObjectType().matches(objectType);
    }

    @Nullable
    @Override
    public GenericDatabaseElement getParentElement() {
        return getParentObject();
    }

    @Override
    public String getTypeName() {
        return getObjectType().getName();
    }

    @Override
    @NotNull
    public String getName() {
        return objectRef.getObjectName();
    }

    @Override
    public short getOverload() {
        return 0;
    }

    @Override
    public String getQuotedName(boolean quoteAlways) {
        String name = getName();
        if (quoteAlways || needsNameQuoting()) {
            ConnectionHandler connectionHandler = getConnectionHandler();
            ConnectionDatabaseSettings databaseSettings = connectionHandler.getSettings().getDatabaseSettings();
            if (databaseSettings.getDatabaseType() == DatabaseType.GENERIC) {
                String identifierQuotes = connectionHandler.getCompatibility().getIdentifierQuote();
                return identifierQuotes + name + identifierQuotes;
            } else {
                DatabaseCompatibilityInterface compatibilityInterface = DatabaseCompatibilityInterface.getInstance(this);
                QuotePair quotes = compatibilityInterface.getDefaultIdentifierQuotes();
                return quotes.beginChar() + name + quotes.endChar();
            }
        } else {
            return name;
        }
    }

    @Override
    public boolean needsNameQuoting() {
        String name = getName();
        return name.indexOf('-') > 0 ||
                name.indexOf('.') > 0 ||
                name.indexOf('#') > 0 ||
                getLanguageDialect(SQLLanguage.INSTANCE).isReservedWord(name) ||
                StringUtil.isMixedCase(name);
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return getObjectType().getIcon();
    }

    @NotNull
    @Override
    public String getQualifiedName() {
        return objectRef.getPath();
    }

    @Override
    public String getQualifiedNameWithType() {
        return objectRef.getQualifiedNameWithType();
    }

    @Override
    @Nullable
    public DBUser getOwner() {
        DBObject parentObject = getParentObject();
        return parentObject == null ? null : parentObject.getOwner();
    }

    @Override
    public Icon getOriginalIcon() {
        return getIcon();
    }

    @Override
    public String getNavigationTooltipText() {
        DBObject parentObject = getParentObject();
        if (parentObject == null) {
            return getTypeName();
        } else {
            return getTypeName() + " (" +
                    parentObject.getTypeName() + ' ' +
                    parentObject.getName() + ')';
        }
    }


    @Override
    public String getToolTip() {
        if (isDisposed()) {
            return null;
        }
        return new HtmlToolTipBuilder() {
            @Override
            public void buildToolTip() {
                DBObjectImpl.this.buildToolTip(this);
            }
        }.getToolTip();
    }

    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ConnectionHandler connectionHandler = getConnectionHandler();
        ttb.append(true, getQualifiedName(), false);
        ttb.append(true, "Connection: ", null, null, false );
        ttb.append(false, connectionHandler.getPresentableText(), false);
    }

    @Override
    public DBObjectAttribute[] getObjectAttributes(){return null;}
    @Override
    public DBObjectAttribute getNameAttribute(){return null;}

    @NotNull
    @Override
    public DBObjectBundle getObjectBundle() {
        ConnectionHandler connectionHandler = getConnectionHandler();
        return connectionHandler.getObjectBundle();
    }


    @NotNull
    @Override
    public ConnectionId getConnectionId() {
        return connectionHandler.getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnectionHandler() {
        return ConnectionHandlerRef.ensure(connectionHandler);
    }

    @NotNull
    @Override
    public EnvironmentType getEnvironmentType() {
        ConnectionHandler connectionHandler = getConnectionHandler();
        return connectionHandler.getEnvironmentType();
    }

    @Override
    public DBLanguageDialect getLanguageDialect(DBLanguage language) {
        ConnectionHandler connectionHandler = getConnectionHandler();
        return connectionHandler.getLanguageDialect(language);
    }

    @Override
    public DBObjectListContainer getChildObjects() {
        return childObjects;
    }

    @Override
    public void initChildren() {
        if (childObjects != null) childObjects.load();
    }

    @Override
    public DBObjectRelationListContainer getChildObjectRelations() {
        return childObjectRelations;
    }

    @NotNull
    public synchronized DBObjectListContainer initChildObjects() {
        if (childObjects == null) {
            childObjects = new DBObjectListContainer(this);
        }
        return childObjects;
    }

    @NotNull
    public synchronized DBObjectRelationListContainer initChildObjectRelations() {
        if (childObjectRelations == null) {
            childObjectRelations = new DBObjectRelationListContainer(this);
        }
        return childObjectRelations;

    }

    public static DBObject getObjectByName(List<? extends DBObject> objects, String name) {
        if (objects != null) {
            for (DBObject object : objects) {
                if (Objects.equals(object.getName(), name)) {
                    return object;
                }
            }
        }
        return null;
    }

    @Override
    public DBObject getChildObject(DBObjectType objectType, String name, boolean lookupHidden) {
        return getChildObject(objectType, name, (short) 0, lookupHidden);
    }

    @Override
    public List<String> getChildObjectNames(DBObjectType objectType) {
        if (childObjects != null) {
            DBObjectList objectList = childObjects.getObjectList(objectType);
            if (objectList != null) {
                List<String> objectNames = new ArrayList<>();
                List<DBObject> objects = objectList.getObjects();
                for (DBObject object : objects) {
                    objectNames.add(object.getName());
                }
                return objectNames;
            }
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public DBObject getChildObject(DBObjectType objectType, String name, short overload, boolean lookupHidden) {
        if (childObjects == null) {
            return null;
        } else {
            DBObject object = childObjects.getObject(objectType, name, overload);
            if (object == null && lookupHidden) {
                object = childObjects.getInternalObject(objectType, name, overload);
            }
            return object;
        }
    }

    @Override
    @Nullable
    public DBObject getChildObject(String name, boolean lookupHidden) {
        return getChildObject(name, (short) 0, lookupHidden);
    }

    @Override
    @Nullable
    public DBObject getChildObject(String name, short overload, boolean lookupHidden) {
        return childObjects == null ? null :
                childObjects.getObjectForParentType(this.getObjectType(), name, overload, lookupHidden);
    }

    public DBObject getChildObjectNoLoad(String name) {
        return getChildObjectNoLoad(name, (short) 0);
    }

    public DBObject getChildObjectNoLoad(String name, short overload) {
        return childObjects == null ? null : childObjects.getObjectNoLoad(name, overload);
    }

    @Override
    @NotNull
    public List<DBObject> getChildObjects(DBObjectType objectType) {
        ListCollector<DBObject> collector = ListCollector.basic();
        collectChildObjects(objectType, collector);
        return collector.elements();
    }

    @Override
    public void collectChildObjects(DBObjectType objectType, Consumer<? super DBObject> consumer) {
        if (objectType.getFamilyTypes().size() > 1) {
            for (DBObjectType childObjectType : objectType.getFamilyTypes()) {
                CancellableConsumer.checkCancelled(consumer);
                if (objectType != childObjectType) {
                    collectChildObjects(childObjectType, consumer);
                } else {
                    DBObjectList<?> objectList = childObjects == null ? null : childObjects.getObjectList(objectType);
                    if (objectList != null) {
                        objectList.collectObjects(consumer);
                    }
                }
            }
        } else if (childObjects != null) {
            if (objectType == DBObjectType.ANY) {
                for (DBObjectList<?> objectList : childObjects.getElements()) {
                    CancellableConsumer.checkCancelled(consumer);
                    if (!objectList.isInternal() && Failsafe.check(objectList)) {
                        objectList.collectObjects(consumer);
                    }
                }
            } else {
                DBObjectList<?> objectList = childObjects.getObjectList(objectType);
                if (objectList == null) {
                    objectList = childObjects.getInternalObjectList(objectType);
                }
                if (objectList != null) objectList.collectObjects(consumer);
            }
        }
    }



    @Nullable
    @Override
    public DBObjectList<? extends DBObject> getChildObjectList(DBObjectType objectType) {
        return childObjects == null ? null : childObjects.getObjectList(objectType);
    }

    @Override
    public List<DBObjectNavigationList> getNavigationLists() {
        // todo consider caching;
        return createNavigationLists();
    }

    protected List<DBObjectNavigationList> createNavigationLists() {
        return null;
    }

    @Override
    @NotNull
    public LookupItemBuilder getLookupItemBuilder(DBLanguage language) {
        DBObjectBundle objectBundle = Failsafe.nn(getObjectBundle());
        return objectBundle.getLookupItemBuilder(objectRef, language);
    }

    @Override
    @NotNull
    public DBObjectPsiFacade getPsiFacade() {
        DBObjectBundle objectBundle = Failsafe.nn(getObjectBundle());
        return objectBundle.getObjectPsiFacade(getRef());
    }

    @Override
    @NotNull
    public DBObjectVirtualFile<?> getVirtualFile() {
        DBObjectBundle objectBundle = Failsafe.nn(getObjectBundle());
        return objectBundle.getObjectVirtualFile(getRef());
    }

    @Override
    public String extractDDL() throws SQLException {
        ConnectionHandler connectionHandler = Failsafe.nn(getConnectionHandler());
        // TODO move to database interface (ORACLE)
        return PooledConnection.call(true,
                connectionHandler,
                connection -> {
                    DBNCallableStatement statement = null;
                    try {
                        statement = connection.prepareCall("{? = call DBMS_METADATA.GET_DDL(?, ?, ?)}");
                        statement.registerOutParameter(1, Types.CLOB);
                        statement.setString(2, getTypeName().toUpperCase());
                        statement.setString(3, getName());
                        statement.setString(4, getSchema().getName());

                        statement.execute();
                        String ddl = statement.getString(1);
                        return ddl == null ? null : ddl.trim();
                    } finally{
                        ResourceUtil.close(statement);
                    }
                });
    }

    @Override
    @Nullable
    public DBObject getUndisposedElement() {
        return objectRef.get();
    }

    @Override
    @Nullable
    public DynamicContent getDynamicContent(DynamicContentType dynamicContentType) {
        if(dynamicContentType instanceof DBObjectType && childObjects != null) {
            DBObjectType objectType = (DBObjectType) dynamicContentType;
            DynamicContent dynamicContent = childObjects.getObjectList(objectType);
            if (dynamicContent == null) dynamicContent = childObjects.getInternalObjectList(objectType);
            return dynamicContent;
        }

        else if (dynamicContentType instanceof DBObjectRelationType && childObjectRelations != null) {
            DBObjectRelationType objectRelationType = (DBObjectRelationType) dynamicContentType;
            return childObjectRelations.getObjectRelationList(objectRelationType);
        }

        return null;
    }

    @Override
    public final void reload() {
        if (childObjects != null) {
            childObjects.reload();
        }
    }

    @Override
    public final void refresh() {
        if (childObjects != null) {
            childObjects.refresh();
        }
    }

    public final void refresh(@NotNull DBObjectType childObjectType) {
        DBObjectList objectList = getChildObjectList(childObjectType);
        if (objectList != null) {
            objectList.refresh();
        }
    }

    /*********************************************************
     *                   NavigationItem                      *
     *********************************************************/
    public FileStatus getFileStatus() {
        return FileStatus.UNKNOWN;
    }

    @Override
    public ItemPresentation getPresentation() {
        return this;
    }

    public TextAttributesKey getTextAttributesKey() {
        return SQLTextAttributesKeys.IDENTIFIER;
    }

    @Override
    public String getLocationString() {
        return null;
    }

    @Override
    public Icon getIcon(boolean open) {
        return getIcon();
    }

    /*********************************************************
     *                  BrowserTreeNode                   *
     *********************************************************/
    @Override
    public void initTreeElement() {}

    @Override
    public boolean isTreeStructureLoaded() {
        return properties.is(DBObjectProperty.TREE_LOADED);
    }

    @Override
    public boolean canExpand() {
        return !isLeaf() && isTreeStructureLoaded() && getChildAt(0).isTreeStructureLoaded();
    }

    @Override
    public Icon getIcon(int flags) {
        return getIcon();
    }

    @Override
    public String getPresentableText() {
        return getName();
    }

    @Override
    public String getPresentableTextDetails() {
        return null;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return null;
    }

    @Override
    @NotNull
    public BrowserTreeNode getParent() {
        DBObjectType objectType = getObjectType();
        if (parentObjectRef != null){
            DBObject object = parentObjectRef.get();
            if (object != null) {
                DBObjectListContainer childObjects = object.getChildObjects();
                if (childObjects != null) {
                    DBObjectList parentObjectList = childObjects.getObjectList(objectType);
                    return Failsafe.nn(parentObjectList);
                }
            }
        } else {
            DBObjectBundle objectBundle = getObjectBundle();
            DBObjectListContainer objectListContainer = objectBundle.getObjectListContainer();
            DBObjectList parentObjectList = objectListContainer.getObjectList(objectType);
            return Failsafe.nn(parentObjectList);
        }
        throw AlreadyDisposedException.INSTANCE;
    }



    @Override
    public int getTreeDepth() {
        BrowserTreeNode treeParent = getParent();
        return treeParent.getTreeDepth() + 1;
    }


    @NotNull
    public synchronized List<BrowserTreeNode> getAllPossibleTreeChildren() {
        if (allPossibleTreeChildren == null) {
            allPossibleTreeChildren = buildAllPossibleTreeChildren();
            allPossibleTreeChildren = compact(allPossibleTreeChildren);
        }
        return allPossibleTreeChildren;
    }



    @Override
    public synchronized List<? extends BrowserTreeNode> getChildren() {
        if (visibleTreeChildren == null) {
            visibleTreeChildren = new ArrayList<>();
            visibleTreeChildren.add(new LoadInProgressTreeNode(this));

            Background.run(() -> buildTreeChildren());
        }
        return visibleTreeChildren;
    }

    private void buildTreeChildren() {
        checkDisposed();
        ConnectionHandler connectionHandler = getConnectionHandler();
        Filter<BrowserTreeNode> objectTypeFilter = connectionHandler.getObjectTypeFilter();

        List<BrowserTreeNode> treeChildren = filter(getAllPossibleTreeChildren(), false, true, objectTypeFilter);
        treeChildren = CommonUtil.nvl(treeChildren, Collections.emptyList());

        treeChildren.forEach(objectList -> {
            Background.run(() -> objectList.initTreeElement());
            checkDisposed();
        });

        if (visibleTreeChildren.size() == 1 && visibleTreeChildren.get(0) instanceof LoadInProgressTreeNode) {
            visibleTreeChildren.get(0).dispose();
        }

        visibleTreeChildren = treeChildren;
        visibleTreeChildren = compact(visibleTreeChildren);
        set(DBObjectProperty.TREE_LOADED, true);


        Project project = Failsafe.nn(getProject());
        ProjectEvents.notify(project,
                BrowserTreeEventListener.TOPIC,
                (listener) -> listener.nodeChanged(this, TreeEventType.STRUCTURE_CHANGED));
        DatabaseBrowserManager.scrollToSelectedElement(getConnectionHandler());
    }

    @Override
    public void refreshTreeChildren(@NotNull DBObjectType... objectTypes) {
        if (visibleTreeChildren != null) {
            visibleTreeChildren.forEach(treeNode -> treeNode.refreshTreeChildren(objectTypes));
        }

    }

    @Override
    public void rebuildTreeChildren() {
        if (visibleTreeChildren != null) {
            ConnectionHandler connectionHandler = getConnectionHandler();
            Filter<BrowserTreeNode> filter = connectionHandler.getObjectTypeFilter();

            if (treeVisibilityChanged(getAllPossibleTreeChildren(), visibleTreeChildren, filter)) {
                buildTreeChildren();
            }
            visibleTreeChildren.forEach(treeNode -> treeNode.rebuildTreeChildren());
        }


    }

    @NotNull
    public List<BrowserTreeNode> buildAllPossibleTreeChildren() {
        return EMPTY_TREE_NODE_LIST;
    }

    @Override
    public boolean isLeaf() {
        return Safe.call(true, () -> {
            if (visibleTreeChildren == null) {
                ConnectionHandler connectionHandler = getConnectionHandler();
                Filter<BrowserTreeNode> filter = connectionHandler.getObjectTypeFilter();
                for (BrowserTreeNode treeNode : getAllPossibleTreeChildren() ) {
                    if (treeNode != null && filter.accepts(treeNode)) {
                        return false;
                    }
                }
                return true;
            } else {
                return visibleTreeChildren.size() == 0;
            }
        });
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
    public int getIndex(BrowserTreeNode child) {
        return getChildren().indexOf(child);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof DBObject) {
            DBObject object = (DBObject) obj;
            return objectRef.equals(object.getRef());
        }
        return false;
    }


    public int hashCode() {
        return objectRef.hashCode();
    }

    @Override
    @NotNull
    public Project getProject() throws PsiInvalidElementAccessException {
        ConnectionHandler connectionHandler = Failsafe.nn(getConnectionHandler());
        return connectionHandler.getProject();
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof DBObject) {
            DBObject object = (DBObject) o;
            return objectRef.compareTo(object.getRef());
        }
        return -1;
    }

    public String toString() {
        return getName();
    }

    @Override
    public List<PresentableProperty> getPresentableProperties() {
        List<PresentableProperty> properties = new ArrayList<>();
        DBObject parent = getParentObject();
        while (parent != null) {
            properties.add(new DBObjectPresentableProperty(parent));
            parent = parent.getParentObject();
        }
        properties.add(new ConnectionPresentableProperty(getConnectionHandler()));

        return properties;
    }

    @Override
    public boolean isValid() {
        return !isDisposed();
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    /*********************************************************
    *               DynamicContentElement                    *
    *********************************************************/

    @Override
    public String getDescription() {
        return getQualifiedName();
    }

    /*********************************************************
    *                      Navigatable                      *
    *********************************************************/
    @Override
    public void navigate(boolean requestFocus) {
        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(getProject());
        browserManager.navigateToElement(this, requestFocus, true);
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    /*********************************************************
     *                   PsiElement                          *
     *********************************************************/

    //@Override
    public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
        return DBObjectPsiFacade.asPsiFile(this);
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public void disposeInner() {
        SafeDisposer.dispose(childObjects, false);
        SafeDisposer.dispose(childObjectRelations, false);
        nullify();
    }
}
