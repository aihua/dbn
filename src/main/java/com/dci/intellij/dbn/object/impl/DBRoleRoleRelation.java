package com.dci.intellij.dbn.object.impl;

import com.dci.intellij.dbn.object.DBGrantedRole;
import com.dci.intellij.dbn.object.DBRole;
import com.dci.intellij.dbn.object.common.list.DBObjectRelationImpl;
import com.dci.intellij.dbn.object.type.DBObjectRelationType;

public class DBRoleRoleRelation extends DBObjectRelationImpl<DBRole, DBGrantedRole> {
    public DBRoleRoleRelation(DBRole role, DBGrantedRole grantedRole) {
        super(DBObjectRelationType.ROLE_ROLE, role, grantedRole);
    }

    public DBRole getRole() {
        return getSourceObject();
    }

    public DBGrantedRole getGrantedRole() {
        return getTargetObject();
    }
}