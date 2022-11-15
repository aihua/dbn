package com.dci.intellij.dbn.database.interfaces;

public interface DatabaseInterfacesProvider {

    default DatabaseMetadataInterface getMetadataInterface() {
        return getInterfaces().getMetadataInterface();
    }

    default DatabaseCompatibilityInterface getCompatibilityInterface() {
        return getInterfaces().getCompatibilityInterface();
    }

    default DatabaseDebuggerInterface getDebuggerInterface() {
        return getInterfaces().getDebuggerInterface();
    }

    default DatabaseMessageParserInterface getMessageParserInterface() {
        return getInterfaces().getMessageParserInterface();
    }

    default DatabaseDataDefinitionInterface getDataDefinitionInterface() {
        return getInterfaces().getDataDefinitionInterface();
    }

    DatabaseInterfaces getInterfaces();
}
