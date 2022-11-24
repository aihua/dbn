package com.dci.intellij.dbn.object.impl;

import com.dci.intellij.dbn.browser.DatabaseBrowserUtils;
import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.database.common.metadata.def.DBViewMetadata;
import com.dci.intellij.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dci.intellij.dbn.database.interfaces.queue.InterfaceTaskDefinition;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.language.common.DBLanguage;
import com.dci.intellij.dbn.language.sql.SQLLanguage;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.DBType;
import com.dci.intellij.dbn.object.DBView;
import com.dci.intellij.dbn.object.common.DBObjectBundle;
import com.dci.intellij.dbn.object.common.list.DBObjectNavigationList;
import com.dci.intellij.dbn.object.common.property.DBObjectProperty;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import static com.dci.intellij.dbn.common.Priority.HIGHEST;

public class DBViewImpl extends DBDatasetImpl<DBViewMetadata> implements DBView {
    private DBObjectRef<DBType> type;
    DBViewImpl(DBSchema schema, DBViewMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(DBViewMetadata metadata) throws SQLException {
        String name = metadata.getViewName();
        set(DBObjectProperty.SYSTEM_OBJECT, metadata.isSystemView());
        String typeOwner = metadata.getViewTypeOwner();
        String typeName = metadata.getViewType();
        if (typeOwner != null && typeName != null) {
            DBObjectBundle objectBundle = getObjectBundle();
            DBSchema typeSchema = objectBundle.getSchema(typeOwner);
            type = DBObjectRef.of(typeSchema == null ? null : typeSchema.getType(typeName));
        }
        return name;
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.VIEW;
    }

    @Override
    public DBType getType() {
        return DBObjectRef.get(type);
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    @NotNull
    public List<BrowserTreeNode> buildAllPossibleTreeChildren() {
        return DatabaseBrowserUtils.createList(
                columns,
                constraints,
                triggers);
    }

    @Override
    public boolean isEditable(DBContentType contentType) {
        return contentType == DBContentType.CODE;
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        DBType type = getType();
        if (type != null) {
            List<DBObjectNavigationList> navigationLists = new LinkedList<>();
            navigationLists.add(DBObjectNavigationList.create("Type", type));
            return navigationLists;
        }
        return null;
    }

    @Override
    public boolean isSystemView() {
        return is(DBObjectProperty.SYSTEM_OBJECT);
    }

    /*********************************************************
     *                  DBEditableCodeObject                 *
     ********************************************************/

    @Override
    public void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException {
        InterfaceTaskDefinition taskDefinition = InterfaceTaskDefinition.create(HIGHEST,
                "Updating source code",
                "Updating sources of " + getQualifiedNameWithType(),
                createInterfaceContext());

        DatabaseInterfaceInvoker.execute(taskDefinition, conn -> {
            ConnectionHandler connection = getConnection();
            DatabaseDataDefinitionInterface dataDefinition = connection.getDataDefinitionInterface();
            dataDefinition.updateView(getName(), newCode, conn);
        });
    }

    @Override
    public String getCodeParseRootId(DBContentType contentType) {
        return "subquery";
    }

    @Override
    public DBLanguage getCodeLanguage(DBContentType contentType) {
        return SQLLanguage.INSTANCE;
    }
}
