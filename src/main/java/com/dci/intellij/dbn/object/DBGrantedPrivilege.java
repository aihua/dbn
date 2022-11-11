package com.dci.intellij.dbn.object;

public interface DBGrantedPrivilege extends DBCastedObject{
    DBPrivilegeGrantee getGrantee();
    DBPrivilege getPrivilege();
    boolean isAdminOption();
}
