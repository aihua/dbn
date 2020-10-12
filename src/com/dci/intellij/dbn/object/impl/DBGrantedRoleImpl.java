package com.dci.intellij.dbn.object.impl;

import com.dci.intellij.dbn.database.common.metadata.def.DBGrantedRoleMetadata;
import com.dci.intellij.dbn.object.DBGrantedRole;
import com.dci.intellij.dbn.object.DBRole;
import com.dci.intellij.dbn.object.DBRoleGrantee;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.DBObjectImpl;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

import static com.dci.intellij.dbn.object.common.property.DBObjectProperty.ADMIN_OPTION;
import static com.dci.intellij.dbn.object.common.property.DBObjectProperty.DEFAULT_ROLE;

public class DBGrantedRoleImpl extends DBObjectImpl<DBGrantedRoleMetadata> implements DBGrantedRole {
    private DBObjectRef<DBRole> roleRef;

    public DBGrantedRoleImpl(DBRoleGrantee grantee, DBGrantedRoleMetadata metadata) throws SQLException {
        super(grantee, metadata);
    }

    @Override
    protected String initObject(DBGrantedRoleMetadata metadata) throws SQLException {
        String name = metadata.getGrantedRoleName();
        this.roleRef = DBObjectRef.of(getConnectionHandler().getObjectBundle().getRole(name));
        set(ADMIN_OPTION, metadata.isAdminOption());
        set(DEFAULT_ROLE, metadata.isDefaultRole());
        return name;
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.GRANTED_ROLE;
    }

    @Override
    public DBRoleGrantee getGrantee() {
        return (DBRoleGrantee) getParentObject();
    }

    @Override
    public DBRole getRole() {
        return DBObjectRef.get(roleRef);
    }

    @Override
    public boolean isAdminOption() {
        return is(ADMIN_OPTION);
    }

    @Override
    public boolean isDefaultRole() {
        return is(DEFAULT_ROLE);
    }

    @Nullable
    @Override
    public DBObject getDefaultNavigationObject() {
        return getRole();
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    public boolean isLeaf() {
        return true;
    }


}
