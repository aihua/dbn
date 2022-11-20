package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.connection.config.ConnectionDatabaseSettings;
import com.dci.intellij.dbn.database.generic.GenericDatabaseInterfaces;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaces;
import com.dci.intellij.dbn.database.mysql.MySqlDatabaseInterfaces;
import com.dci.intellij.dbn.database.oracle.OracleDatabaseInterfaces;
import com.dci.intellij.dbn.database.postgres.PostgresDatabaseInterfaces;
import com.dci.intellij.dbn.database.sqlite.SqliteDatabaseInterfaces;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class DatabaseInterfacesBundle {
    private static final Map<DatabaseType, DatabaseInterfaces> REGISTRY = new EnumMap<>(DatabaseType.class);

    // fixme replace with generic data dictionary
    static final DatabaseInterfaces GENERIC_INTERFACES = new GenericDatabaseInterfaces();
    private static final DatabaseInterfaces ORACLE_INTERFACES = new OracleDatabaseInterfaces();
    private static final DatabaseInterfaces MYSQL_INTERFACES = new MySqlDatabaseInterfaces();
    private static final DatabaseInterfaces POSTGRES_INTERFACES = new PostgresDatabaseInterfaces();
    private static final DatabaseInterfaces SQLITE_INTERFACES = new SqliteDatabaseInterfaces();

    static {
        REGISTRY.put(DatabaseType.GENERIC, GENERIC_INTERFACES);
        REGISTRY.put(DatabaseType.ORACLE, ORACLE_INTERFACES);
        REGISTRY.put(DatabaseType.MYSQL, MYSQL_INTERFACES);
        REGISTRY.put(DatabaseType.POSTGRES, POSTGRES_INTERFACES);
        REGISTRY.put(DatabaseType.SQLITE, SQLITE_INTERFACES);
    }

    public static DatabaseInterfaces get(@NotNull ConnectionHandler connection) {
        DatabaseType databaseType;
        if (connection.isVirtual()) {
            databaseType = connection.getDatabaseType();
        } else {
            ConnectionDatabaseSettings databaseSettings = connection.getSettings().getDatabaseSettings();
            databaseType = databaseSettings.getDatabaseType();
/*
            TODO use resolvedDatabaseType to guess better interface provider ??
            if (databaseType == DatabaseType.UNKNOWN) {
                DBNConnection testConnection = null;
                try {
                    testConnection = connection.getTestConnection();
                    databaseType = ResourceUtil.getDatabaseType(testConnection);
                    databaseSettings.setDatabaseType(databaseType);
                }  finally {
                    ResourceUtil.close(testConnection);
                }
            }
*/
        }

        return get(databaseType);
    }

    @NotNull
    public static DatabaseInterfaces get(DatabaseType databaseType) {
        DatabaseInterfaces interfaces = REGISTRY.get(databaseType);
        return interfaces == null ? GENERIC_INTERFACES : interfaces;
    }

    public static void reset() {
        GENERIC_INTERFACES.reset();
        ORACLE_INTERFACES.reset();
        MYSQL_INTERFACES.reset();
        POSTGRES_INTERFACES.reset();
        SQLITE_INTERFACES.reset();
    }
}
