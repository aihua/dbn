package com.dci.intellij.dbn.object.impl;

import com.dci.intellij.dbn.browser.DatabaseBrowserUtils;
import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.browser.ui.HtmlToolTipBuilder;
import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.content.DynamicContent;
import com.dci.intellij.dbn.common.content.loader.DynamicContentResultSetLoader;
import com.dci.intellij.dbn.common.content.loader.DynamicSubcontentLoader;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.type.DBDataTypeDefinition;
import com.dci.intellij.dbn.data.type.DBNativeDataType;
import com.dci.intellij.dbn.database.common.metadata.def.*;
import com.dci.intellij.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.object.*;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.common.list.DBObjectList;
import com.dci.intellij.dbn.object.common.list.DBObjectListContainer;
import com.dci.intellij.dbn.object.common.list.DBObjectNavigationList;
import com.dci.intellij.dbn.object.common.property.DBObjectProperty;
import com.dci.intellij.dbn.object.common.status.DBObjectStatus;
import com.dci.intellij.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.properties.DBDataTypePresentableProperty;
import com.dci.intellij.dbn.object.properties.PresentableProperty;
import com.dci.intellij.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.dci.intellij.dbn.object.type.DBObjectType.*;

public class DBTypeImpl
        extends DBProgramImpl<DBTypeMetadata, DBTypeProcedure, DBTypeFunction>
        implements DBType {

    private static final List<DBTypeAttribute> EMPTY_ATTRIBUTE_LIST = Collections.unmodifiableList(new ArrayList<>(0));

    protected DBObjectList<DBTypeAttribute> attributes;
    protected DBObjectList<DBType> subTypes;

    private String superTypeOwner;
    private String superTypeName;
    private DBObjectRef<DBType> superType;

    private DBDataTypeDefinition collectionElementTypeRef;
    private DBDataType collectionElementType;

    private DBNativeDataType nativeDataType;

    DBTypeImpl(DBSchemaObject parent, DBTypeMetadata metadata) throws SQLException {
        // type functions are not editable independently
        super(parent, metadata);
        assert this.getClass() != DBTypeImpl.class;
    }

    DBTypeImpl(DBSchema schema, DBTypeMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(DBTypeMetadata metadata) throws SQLException {
        String name = metadata.getTypeName();
        superTypeOwner = metadata.getSupertypeOwner();
        superTypeName = metadata.getSupertypeName();

        String typeCode = metadata.getTypeCode();
        boolean collection = metadata.isCollection();
        set(DBObjectProperty.COLLECTION, collection);

        nativeDataType = getObjectBundle().getNativeDataType(typeCode);
        if (collection) {
            DBDataTypeMetadata collectionMetadata = metadata.getDataType().collection();
            collectionElementTypeRef = new DBDataTypeDefinition(collectionMetadata);
        }
        return name;
    }


    @Override
    protected void initLists() {
        super.initLists();
        if (!isCollection()) {
            DBObjectListContainer childObjects = ensureChildObjects();
            DBSchema schema = getSchema();
            attributes = childObjects.createSubcontentObjectList(TYPE_ATTRIBUTE, this, schema);
            procedures = childObjects.createSubcontentObjectList(TYPE_PROCEDURE, this, schema);
            functions =  childObjects.createSubcontentObjectList(TYPE_FUNCTION, this, schema);
            subTypes =   childObjects.createSubcontentObjectList(TYPE, this, schema);
        }
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return TYPE;
    }

    @Override
    @Nullable
    public Icon getIcon() {
        if (getStatus().is(DBObjectStatus.VALID)) {
            if (getStatus().is(DBObjectStatus.DEBUG))  {
                return Icons.DBO_TYPE_DEBUG;
            } else {
                return isCollection() ? Icons.DBO_TYPE_COLLECTION : Icons.DBO_TYPE;
            }
        } else {
            return isCollection() ? Icons.DBO_TYPE_COLLECTION_ERR : Icons.DBO_TYPE_ERR;
        }
    }

    @Override
    public Icon getOriginalIcon() {
        return isCollection() ? Icons.DBO_TYPE_COLLECTION : Icons.DBO_TYPE;
    }

    @Override
    public List<DBTypeAttribute> getAttributes() {
        return attributes == null ? EMPTY_ATTRIBUTE_LIST : attributes.getObjects();
    }

    @Override
    public DBType getSuperType() {
        if (superType == null && superTypeOwner != null && superTypeName != null) {
            DBSchema schema = getObjectBundle().getSchema(superTypeOwner);
            DBType type = schema == null ? null : schema.getType(superTypeName);
            superType = DBObjectRef.of(type);
            superTypeOwner = null;
            superTypeName = null;
        }
        return DBObjectRef.get(superType);
    }

    @Override
    public DBDataType getCollectionElementType() {
        if (collectionElementType == null && collectionElementTypeRef != null) {
            collectionElementType = getObjectBundle().getDataTypes().getDataType(collectionElementTypeRef);
            collectionElementTypeRef = null;
        }
        return collectionElementType;
    }

    @Override
    @Nullable
    public DBObject getDefaultNavigationObject() {
        if (isCollection()) {
            DBDataType dataType = getCollectionElementType();
            if (dataType != null && dataType.isDeclared()) {
                return dataType.getDeclaredType();
            }

        }
        return null;
    }

    @Override
    public List<PresentableProperty> getPresentableProperties() {
        List<PresentableProperty> properties = super.getPresentableProperties();
        DBDataType collectionElementType = getCollectionElementType();
        if (collectionElementType != null) {
            properties.add(0, new DBDataTypePresentableProperty("Collection element type", collectionElementType));
        }
        return properties;
    }

    @Override
    public List<DBType> getSubTypes() {
        return subTypes.getObjects();
    }

    @Override
    public DBNativeDataType getNativeDataType() {
        return nativeDataType;
    }

    @Override
    public boolean isCollection() {
        return is(DBObjectProperty.COLLECTION);
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    @NotNull
    public List<BrowserTreeNode> buildPossibleTreeChildren() {
        return isCollection() ?
                EMPTY_TREE_NODE_LIST :
                DatabaseBrowserUtils.createList(attributes, procedures, functions);
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        if (isCollection()) return false;

        ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
        return
            settings.isVisible(ATTRIBUTE) ||
            settings.isVisible(PROCEDURE) ||
            settings.isVisible(FUNCTION);
    }

    /*********************************************************
     *                         Loaders                       *
     *********************************************************/
    static {
        DynamicSubcontentLoader.create(TYPE, TYPE_ATTRIBUTE,
                new DynamicContentResultSetLoader<DBTypeAttribute, DBTypeAttributeMetadata>(TYPE, TYPE_ATTRIBUTE, false, true) {

                    @Override
                    public ResultSet createResultSet(DynamicContent<DBTypeAttribute> dynamicContent, DBNConnection connection) throws SQLException {
                        DatabaseMetadataInterface metadata = dynamicContent.getMetadataInterface();
                        DBType type = dynamicContent.getParentEntity();
                        return metadata.loadTypeAttributes(
                                getSchemaName(type),
                                getObjectName(type),
                                connection);
                    }

                    @Override
                    public DBTypeAttribute createElement(DynamicContent<DBTypeAttribute> content, DBTypeAttributeMetadata metadata, LoaderCache cache) throws SQLException {
                        DBTypeImpl type = content.getParentEntity();
                        return new DBTypeAttributeImpl(type, metadata);
                    }
                });

        DynamicSubcontentLoader.create(TYPE, TYPE_FUNCTION,
                new DynamicContentResultSetLoader<DBTypeFunction, DBFunctionMetadata>(TYPE, TYPE_FUNCTION, false, true) {
                    @Override
                    public ResultSet createResultSet(DynamicContent<DBTypeFunction> dynamicContent, DBNConnection connection) throws SQLException {
                        DatabaseMetadataInterface metadata = dynamicContent.getMetadataInterface();
                        DBType type = dynamicContent.getParentEntity();
                        return metadata.loadTypeFunctions(
                                getSchemaName(type),
                                getObjectName(type),
                                connection);
                    }

                    @Override
                    public DBTypeFunction createElement(DynamicContent<DBTypeFunction> content, DBFunctionMetadata metadata, LoaderCache cache) throws SQLException {
                        DBType type = content.getParentEntity();
                        return new DBTypeFunctionImpl(type, metadata);
                    }
                });

        DynamicSubcontentLoader.create(TYPE, TYPE_PROCEDURE,
                new DynamicContentResultSetLoader<DBTypeProcedure, DBProcedureMetadata>(TYPE, TYPE_PROCEDURE, false, true) {
                    @Override
                    public ResultSet createResultSet(DynamicContent<DBTypeProcedure> dynamicContent, DBNConnection connection) throws SQLException {
                        DatabaseMetadataInterface metadata = dynamicContent.getMetadataInterface();
                        DBType type = dynamicContent.getParentEntity();
                        return metadata.loadTypeProcedures(
                                getSchemaName(type),
                                getObjectName(type),
                                connection);
                    }

                    @Override
                    public DBTypeProcedure createElement(DynamicContent<DBTypeProcedure> content, DBProcedureMetadata metadata, LoaderCache cache) throws SQLException {
                        DBType type = content.getParentEntity();
                        return new DBTypeProcedureImpl(type, metadata);
                    }
                });

        DynamicSubcontentLoader.create(TYPE, TYPE, null/*TODO*/);
    }

    @Override
    public String getCodeParseRootId(DBContentType contentType) {
        if (getParentObject() instanceof DBSchema) {
            return contentType == DBContentType.CODE_SPEC ? "type_spec" :
                   contentType == DBContentType.CODE_BODY ? "type_body" : null;
        }
        return null;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof DBType) {
            DBType that = (DBType) o;
            if (Objects.equals(this.getParentObject(), that.getParentObject())) {
                return this.isCollection() == that.isCollection() ?
                        super.compareTo(o) :
                        that.isCollection() ? -1 : 1;
            }
        }
        return super.compareTo(o);
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        List<DBObjectNavigationList> navigationLists = new LinkedList<>();

        DBType superType = getSuperType();
        if (superType != null) {
            navigationLists.add(DBObjectNavigationList.create("Super Type", superType));
        }
        if (subTypes != null && subTypes.size() > 0) {
            navigationLists.add(DBObjectNavigationList.create("Sub Types", subTypes.getObjects()));
        }
        if (isCollection()) {
            DBDataType dataType = getCollectionElementType();
            if (dataType != null && dataType.isDeclared()) {
                DBType collectionElementType = dataType.getDeclaredType();
                if (collectionElementType != null) {
                    navigationLists.add(DBObjectNavigationList.create("Collection element type", collectionElementType));
                }
            }
        }

        return navigationLists;
    }
}
