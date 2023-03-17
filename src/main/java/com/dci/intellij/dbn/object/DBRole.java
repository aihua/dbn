package com.dci.intellij.dbn.object;

import com.dci.intellij.dbn.object.common.DBRootObject;

import java.util.List;

public interface DBRole extends DBRoleGrantee, DBPrivilegeGrantee, DBRootObject {
    List<DBGrantedPrivilege> getPrivileges();
    boolean hasPrivilege(DBPrivilege privilege);
    boolean hasRole(DBRole role);

    List<DBGrantedRole> getGrantedRoles();
    List<DBUser> getUserGrantees();
    List<DBRole> getRoleGrantees();
}