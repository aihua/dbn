package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.common.property.PropertyHolderImpl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConnectionProperties extends PropertyHolderImpl<ConnectionProperty> {

    public ConnectionProperties(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        set(ConnectionProperty.RS_TYPE_SCROLL_INSENSITIVE, metaData.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE));
        set(ConnectionProperty.RS_TYPE_FORWARD_ONLY,       metaData.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY));
    }

    @Override
    protected ConnectionProperty[] properties() {
        return ConnectionProperty.values();
    }

    @Override
    public ConnectionProperties clone() {
        return (ConnectionProperties) super.clone();
    }
}
