package com.dci.intellij.dbn.database;

import com.dci.intellij.dbn.connection.context.ConnectionProvider;
import com.dci.intellij.dbn.database.interfaces.DatabaseCompatibilityInterface;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public enum DatabaseFeature {
    OBJECT_REPLACING("Replacing existing objects via DDL"),
    OBJECT_DEPENDENCIES("Object dependencies"),
    OBJECT_DDL_EXTRACTION("Object DDL extraction"),
    OBJECT_INVALIDATION("Object invalidation"),
    OBJECT_DISABLING("Disabling objects"),
    OBJECT_SOURCE_EDITING("Editing object sources"),
    OBJECT_CHANGE_MONITORING("Monitoring objects changes"),
    AUTHID_METHOD_EXECUTION("AUDHID method execution (execution on different schema)"),
    FUNCTION_OUT_ARGUMENTS("OUT arguments for functions"),
    DEBUGGING("Program execution debugging"),
    EXPLAIN_PLAN("Statement explain plan"),
    DATABASE_LOGGING("Database logging"),
    SESSION_CURRENT_SQL("Session current SQL"),
    SESSION_BROWSING("Session browsing"),
    SESSION_KILL("Kill session"),
    SESSION_DISCONNECT("Disconnect session"),
    SESSION_INTERRUPTION_TIMING("Session interruption timing"),
    CONNECTION_ERROR_RECOVERY("Recover connection transaction after error"),
    UPDATABLE_RESULT_SETS("Updatable result sets"),
    CURRENT_SCHEMA("Current schema initializing"),
    CONSTRAINT_MANIPULATION("Constraint manipulation"),
    READONLY_CONNECTIVITY("Readonly connectivity"),
    ;

    private final String description;

    DatabaseFeature(String description) {
        this.description = description;
    }

    public boolean isNotSupported(@Nullable ConnectionProvider connectionProvider) {
        return !isSupported(connectionProvider);
    }

    public boolean isSupported(@Nullable ConnectionProvider connectionProvider) {
        if (connectionProvider == null) return false;

        DatabaseCompatibilityInterface compatibility = connectionProvider.getCompatibilityInterface();
        return compatibility.supportsFeature(this);
    }
}
