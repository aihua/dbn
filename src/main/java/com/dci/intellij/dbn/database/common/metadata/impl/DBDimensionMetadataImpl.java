package com.dci.intellij.dbn.database.common.metadata.impl;

import com.dci.intellij.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dci.intellij.dbn.database.common.metadata.def.DBDimensionMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBDimensionMetadataImpl extends DBObjectMetadataBase implements DBDimensionMetadata {
    public DBDimensionMetadataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public String getDimensionName() throws SQLException {
        return getString("DIMENSION_NAME");
    }
}
