package com.dci.intellij.dbn.database.common.metadata.def;

import java.sql.SQLException;

public interface DBPackageMetadata extends DBProgramMetadata{

    String getPackageName() throws SQLException;
}
