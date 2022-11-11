package com.dci.intellij.dbn.database.generic;

import com.dci.intellij.dbn.database.DatabaseMessageParserInterface;
import com.dci.intellij.dbn.database.DatabaseObjectIdentifier;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;

public class GenericMessageParserInterface implements DatabaseMessageParserInterface {
    @Override
    @Nullable
    public DatabaseObjectIdentifier identifyObject(SQLException exception) {
         return null;
    }

    @Override
    public boolean isTimeoutException(SQLException e) {
        return e instanceof SQLTimeoutException;
    }

    @Override
    public boolean isAuthenticationException(SQLException e) {
        return false;
    }

    @Override
    public boolean isModelException(SQLException e) {
        return e instanceof SQLFeatureNotSupportedException || e instanceof SQLSyntaxErrorException;
    }

    @Override
    public boolean isSuccessException(SQLException exception) {
        return false;
    }
}
