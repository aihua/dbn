package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.cache.Cache;
import com.dci.intellij.dbn.common.database.AuthenticationInfo;
import com.dci.intellij.dbn.common.database.DatabaseInfo;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.filter.Filter;
import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.notification.NotificationGroup;
import com.dci.intellij.dbn.common.notification.NotificationSupport;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.common.util.Exceptions;
import com.dci.intellij.dbn.common.util.TimeUtil;
import com.dci.intellij.dbn.connection.config.ConnectionDatabaseSettings;
import com.dci.intellij.dbn.connection.config.ConnectionDetailSettings;
import com.dci.intellij.dbn.connection.config.ConnectionSettings;
import com.dci.intellij.dbn.connection.console.DatabaseConsoleBundle;
import com.dci.intellij.dbn.connection.info.ConnectionInfo;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.session.DatabaseSession;
import com.dci.intellij.dbn.connection.session.DatabaseSessionBundle;
import com.dci.intellij.dbn.database.DatabaseCompatibility;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaceQueue;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaceQueueImpl;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaces;
import com.dci.intellij.dbn.execution.statement.StatementExecutionQueue;
import com.dci.intellij.dbn.language.common.DBLanguage;
import com.dci.intellij.dbn.language.common.DBLanguageDialect;
import com.dci.intellij.dbn.language.common.QuotePair;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.dci.intellij.dbn.navigation.psi.DBConnectionPsiDirectory;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.DBObjectBundle;
import com.dci.intellij.dbn.object.common.DBObjectBundleImpl;
import com.dci.intellij.dbn.vfs.file.DBSessionBrowserVirtualFile;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;

@Slf4j
public class ConnectionHandlerImpl extends StatefulDisposable.Base implements ConnectionHandler, NotificationSupport {

    private ConnectionSettings connectionSettings;

    private final ConnectionRef ref;
    private final WeakRef<ConnectionBundle> connectionBundle;
    private final ConnectionHandlerStatusHolder connectionStatus;
    private final ConnectionPool connectionPool;
    private final DatabaseConsoleBundle consoleBundle;
    private final DatabaseSessionBundle sessionBundle;
    private final ConnectionInstructions instructions = new ConnectionInstructions();

    private final Latent<DatabaseInterfaces> interfaces = Latent.mutable(
            () -> getDatabaseType(),
            () -> DatabaseInterfacesBundle.get(this));

    private final Latent<DatabaseInterfaces> derivedInterfaces = Latent.mutable(
            () -> getDerivedDatabaseType(),
            () -> DatabaseInterfacesBundle.get(getDerivedDatabaseType()));

    private final Latent<DatabaseInterfaceQueue> interfaceQueue = Latent.basic(
            () -> new DatabaseInterfaceQueueImpl(this));

    private boolean enabled;
    private ConnectionInfo connectionInfo;
    private DatabaseCompatibility compatibility = DatabaseCompatibility.allFeatures();

    private final Latent<DBSessionBrowserVirtualFile> sessionBrowserFile = Latent.basic(
            () -> new DBSessionBrowserVirtualFile(this));

    private final Latent<Cache> metaDataCache = Latent.basic(
            () -> new Cache(TimeUtil.Millis.ONE_MINUTE));

    private final Latent<AuthenticationInfo> temporaryAuthenticationInfo = Latent.basic(
            () -> {
                ConnectionDatabaseSettings databaseSettings = getSettings().getDatabaseSettings();
                return new AuthenticationInfo(databaseSettings, true);
            });

    private final Latent<DBConnectionPsiDirectory> psiDirectory = Latent.basic(
            () -> new DBConnectionPsiDirectory(this));

    private final Latent<DBObjectBundle> objectBundle = Latent.basic(
            () -> new DBObjectBundleImpl(this, getConnectionBundle()));

    private final Map<SessionId, StatementExecutionQueue> executionQueues = new ConcurrentHashMap<>();

    ConnectionHandlerImpl(ConnectionBundle connectionBundle, ConnectionSettings connectionSettings) {
        this.connectionBundle = WeakRef.of(connectionBundle);
        this.connectionSettings = connectionSettings;
        this.enabled = connectionSettings.isActive();
        ref = ConnectionRef.of(this);

        connectionStatus = new ConnectionHandlerStatusHolder(this);
        connectionPool = new ConnectionPool(this);
        consoleBundle = new DatabaseConsoleBundle(this);
        sessionBundle = new DatabaseSessionBundle(this);
    }

    @NotNull
    @Override
    public List<DBNConnection> getConnections(ConnectionType... connectionTypes) {
        return getConnectionPool().getConnections(connectionTypes);
    }

    @Override
    public ConnectionInstructions getInstructions() {
        return instructions;
    }

    @Override
    public void setTemporaryAuthenticationInfo(AuthenticationInfo temporaryAuthenticationInfo) {
        temporaryAuthenticationInfo.setTemporary(true);
        this.temporaryAuthenticationInfo.set(temporaryAuthenticationInfo);
    }

    @Override
    @Nullable
    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    @Override
    public Cache getMetaDataCache() {
        return metaDataCache.get();
    }

    @Override
    @NotNull
    public String getConnectionName(@Nullable DBNConnection connection) {
        if (connection == null || sessionBundle == null) {
            return getName();
        } else {
            DatabaseSession session = sessionBundle.getSession(connection.getSessionId());
            return getName() + " (" + session.getName() + ")";
        }
    }

    @Override
    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    @Override
    @NotNull
    public AuthenticationInfo getTemporaryAuthenticationInfo() {
        AuthenticationInfo authenticationInfo = temporaryAuthenticationInfo.get();
        if (authenticationInfo.isProvided()) {
            int passwordExpiryTime = getSettings().getDetailSettings().getCredentialExpiryMinutes() * 60000;
            long lastAccessTimestamp = getConnectionPool().getLastAccessTimestamp();
            if (lastAccessTimestamp > 0 && authenticationInfo.isOlderThan(passwordExpiryTime) && TimeUtil.isOlderThan(lastAccessTimestamp, passwordExpiryTime)) {
                temporaryAuthenticationInfo.reset();
            }
        }
        return temporaryAuthenticationInfo.get();
    }

    @Override
    public boolean canConnect() {
        ConnectionSettings connectionSettings = getSettings();
        if (isDisposed() || !connectionSettings.isActive()) {
            return false;
        }

        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        if (!databaseSettings.isDatabaseInitialized() && !instructions.isAllowAutoInit()) {
            return false;
        }

        ConnectionDetailSettings detailSettings = connectionSettings.getDetailSettings();
        if (detailSettings.isConnectAutomatically() || instructions.isAllowAutoConnect()) {
            return isAuthenticationProvided();
        }
        return false;
    }

    @Override
    public boolean isAuthenticationProvided() {
        return getAuthenticationInfo().isProvided() || getTemporaryAuthenticationInfo().isProvided();
    }

    @Override
    public boolean isDatabaseInitialized() {
        return getSettings().getDatabaseSettings().isDatabaseInitialized();
    }

    @Override
    @NotNull
    public ConnectionBundle getConnectionBundle() {
        return connectionBundle.ensure();
    }

    @NotNull
    @Override
    public ConnectionSettings getSettings() {
        return Failsafe.nn(connectionSettings);
    }

    @Override
    public void setSettings(ConnectionSettings connectionSettings) {
        this.connectionSettings = connectionSettings;
        this.enabled = connectionSettings.isActive();
    }

    @Override
    @NotNull
    public ConnectionHandlerStatusHolder getConnectionStatus() {
        return connectionStatus;
    }

    @NotNull
    @Override
    public DatabaseConsoleBundle getConsoleBundle() {
        return consoleBundle;
    }

    @NotNull
    @Override
    public DatabaseSessionBundle getSessionBundle() {
        return sessionBundle;
    }

    @Override
    @NotNull
    public DBSessionBrowserVirtualFile getSessionBrowserFile() {
        return sessionBrowserFile.get();
    }

    @Override
    @NotNull
    public StatementExecutionQueue getExecutionQueue(SessionId sessionId) {
        return executionQueues.computeIfAbsent(sessionId, id -> new StatementExecutionQueue(this));
    }

    @Override
    @NotNull
    public PsiDirectory getPsiDirectory() {
        return psiDirectory.get();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public DatabaseType getDatabaseType() {
        return getSettings().getDatabaseSettings().getDatabaseType();
    }

    public DatabaseType getDerivedDatabaseType() {
        return getSettings().getDatabaseSettings().getDerivedDatabaseType();
    }

    @Override
    public double getDatabaseVersion() {
        return getSettings().getDatabaseSettings().getDatabaseVersion();
    }

    @Override
    public Filter<BrowserTreeNode> getObjectTypeFilter() {
        return getSettings().getFilterSettings().getObjectTypeFilterSettings().getElementFilter();
    }

    @NotNull
    @Override
    public EnvironmentType getEnvironmentType() {
        return getSettings().getDetailSettings().getEnvironmentType();
    }

    @Override
    public boolean isConnected() {
        return connectionStatus.isConnected();
    }

    @Override
    public boolean isConnected(SessionId sessionId) {
        return connectionPool.isConnected(sessionId);
    }

    public String toString() {
        return getPresentableText();
    }

    @Override
    @NotNull
    public Project getProject() {
        return getConnectionBundle().getProject();
    }

    @Override
    public ConnectionRef ref() {
        return ref;
    }

    @Override
    public DatabaseInfo getDatabaseInfo() {
        return getSettings().getDatabaseSettings().getDatabaseInfo();
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo() {
        return getSettings().getDatabaseSettings().getAuthenticationInfo();
    }

    @Override
    public boolean isValid() {
        return connectionStatus.isValid();
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public boolean isAutoCommit() {
        return getSettings().getPropertiesSettings().isEnableAutoCommit();
    }

    @Override
    public boolean isLoggingEnabled() {
        return getSettings().getDetailSettings().isEnableDatabaseLogging();
    }

    @Override
    public boolean hasPendingTransactions(@NotNull DBNConnection conn) {
        try {
            return ConnectionLocalContext.surround(context(), () -> getMetadataInterface().hasPendingTransactions(conn));
        } catch (SQLException e) {
            sendErrorNotification(
                    NotificationGroup.TRANSACTION,
                    "Failed to check connection transactional status: {0}", e);
            return false;

        }

    }

    @Override
    public void setLoggingEnabled(boolean loggingEnabled) {
        getSettings().getDetailSettings().setEnableDatabaseLogging(loggingEnabled);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) {
        getSettings().getPropertiesSettings().setEnableAutoCommit(autoCommit);
    }

    @Override
    public void disconnect() {
        // explicit disconnect (reset auto-connect data)
        temporaryAuthenticationInfo.reset();
        instructions.setAllowAutoConnect(false);
        connectionStatus.setConnected(false);
        getConnectionPool().closeConnections();
    }

    @Override
    public ConnectionId getConnectionId() {
        return getSettings().getConnectionId();
    }

    @Override
    public String getUserName() {
        return Commons.nvl(getSettings().getDatabaseSettings().getAuthenticationInfo().getUser(), "");
    }

    @Override
    @NotNull
    public DBObjectBundle getObjectBundle() {
        return objectBundle.get();
    }

    @Override
    public SchemaId getUserSchema() {
        String userName = getUserName().toUpperCase();
        DBSchema schema = getObjectBundle().getSchema(userName);
        return SchemaId.from(schema);
    }

    @Override
    public SchemaId getDefaultSchema() {
        SchemaId schemaId = getUserSchema();
        if (schemaId == null) {
            String databaseName = getSettings().getDatabaseSettings().getDatabaseInfo().getDatabase();
            if (isNotEmpty(databaseName)) {
                DBSchema schema = getObjectBundle().getSchema(databaseName);
                schemaId = SchemaId.from(schema);
            }
            if (schemaId == null) {
                List<DBSchema> schemas = getObjectBundle().getSchemas();
                if (!schemas.isEmpty()) {
                    schemaId = SchemaId.from(schemas.get(0));
                }
            }
        }
        return schemaId;
    }

    @NotNull
    @Override
    public List<SchemaId> getSchemaIds() {
        return getObjectBundle().getSchemaIds();
    }

    @Nullable
    @Override
    public SchemaId getSchemaId(String name) {
        DBSchema schema = getObjectBundle().getSchema(name);
        return schema == null ? null : schema.getIdentifier();
    }

    @Override
    public DBSchema getSchema(SchemaId schema) {
        return getObjectBundle().getSchema(schema.id());
    }

    @Override
    public DBNConnection getTestConnection() throws SQLException {
        assertCanConnect();
        return getConnectionPool().ensureTestConnection();
    }

    @Override
    @NotNull
    public DBNConnection getMainConnection() throws SQLException {
        assertCanConnect();
        return getConnectionPool().ensureMainConnection();
    }

    @Override
    @NotNull
    public DBNConnection getDebugConnection(@Nullable SchemaId schemaId) throws SQLException {
        assertCanConnect();
        DBNConnection connection = getConnectionPool().ensureDebugConnection();
        setCurrentSchema(connection, schemaId);
        return connection;
    }

    @Override
    @NotNull
    public DBNConnection getDebuggerConnection() throws SQLException {
        assertCanConnect();
        return getConnectionPool().ensureDebuggerConnection();
    }

    @Override
    @NotNull
    public DBNConnection getPoolConnection(boolean readonly) throws SQLException {
        assertCanConnect();
        return getConnectionPool().allocateConnection(readonly);
    }

    @Override
    @NotNull
    public DBNConnection getMainConnection(@Nullable SchemaId schemaId) throws SQLException {
        DBNConnection connection = getMainConnection();
        setCurrentSchema(connection, schemaId);
        return connection;
    }

    @Override
    @NotNull
    public DBNConnection getPoolConnection(@Nullable SchemaId schemaId, boolean readonly) throws SQLException {
        DBNConnection connection = getPoolConnection(readonly);
        setCurrentSchema(connection, schemaId);
        return connection;
    }

    @Override
    @NotNull
    public DBNConnection getConnection(@NotNull SessionId sessionId, @Nullable SchemaId schemaId) throws SQLException {
        DBNConnection connection = getConnection(sessionId);
        setCurrentSchema(connection, schemaId);
        return connection;
    }

    @Override
    @NotNull
    public DBNConnection getConnection(@NotNull SessionId sessionId) throws SQLException {
        if (sessionId == SessionId.MAIN) return getMainConnection();
        if (sessionId == SessionId.POOL) return getPoolConnection(false);
        return getConnectionPool().ensureSessionConnection(sessionId);
    }

    @Override
    public void setCurrentSchema(DBNConnection conn, @Nullable SchemaId schema) throws SQLException {
        if (schema == null) return;
        //if (schema.isPublic()) return;
        if (!DatabaseFeature.CURRENT_SCHEMA.isSupported(this)) return;
        if (Objects.equals(schema, conn.getCurrentSchema())) return;

        ConnectionLocalContext.surround(context(), () -> {
            String schemaName = schema.getName();

            DatabaseCompatibilityInterface compatibility = getCompatibilityInterface();
            QuotePair quotePair = compatibility.getDefaultIdentifierQuotes();

            getMetadataInterface().setCurrentSchema(quotePair.quote(schemaName), conn);
            conn.setCurrentSchema(schema);
        });
    }


    private void assertCanConnect() throws SQLException {
        if (!canConnect()) {
            throw Exceptions.DBN_NOT_CONNECTED_EXCEPTION;
        }
    }

    @Override
    public void closeConnection(DBNConnection connection) {
        getConnectionPool().closeConnection(connection);
    }

    @Override
    public void freePoolConnection(DBNConnection connection) {
        if (!isDisposed()) {
            ConnectionPool connectionPool = getConnectionPool();
            connectionPool.releaseConnection(connection);
        }
    }

    @Override
    @NotNull
    public ConnectionPool getConnectionPool() {
        return Failsafe.nn(connectionPool);
    }

    @NotNull
    @Override
    public DatabaseInterfaces getInterfaces() {
        return interfaces.get();
    }

    private DatabaseInterfaces getDerivedInterfaces() {
        return derivedInterfaces.get();
    }

    public DatabaseInterfaceQueue getInterfaceQueue() {
        return interfaceQueue.get();
    }

    @Override
    public DBLanguageDialect resolveLanguageDialect(Language language) {
        if (language instanceof DBLanguageDialect) {
            return (DBLanguageDialect) language;
        } else if (language instanceof DBLanguage) {
            return getLanguageDialect((DBLanguage<?>) language);
        }
        return null;
    }

    @Override
    public DatabaseCompatibility getCompatibility() {
        return compatibility;
    }

    @Override
    public void resetCompatibilityMonitor() {
        compatibility = DatabaseCompatibility.allFeatures();
    }

    @Override
    public DBLanguageDialect getLanguageDialect(DBLanguage language) {
        DatabaseInterfaces interfaces = getInterfaces();
        DatabaseType databaseType = interfaces.getDatabaseType();
        if (databaseType == DatabaseType.GENERIC) {
            return getDerivedInterfaces().getLanguageDialect(language);
        }

        return interfaces.getLanguageDialect(language);
    }

    public static Comparator<ConnectionHandler> getComparator(boolean asc) {
        return asc ? ASC_COMPARATOR : DESC_COMPARATOR;
    }

    private static final Comparator<ConnectionHandler> ASC_COMPARATOR = Comparator.comparing(connection -> connection.getPresentableText().toLowerCase());
    private static final Comparator<ConnectionHandler> DESC_COMPARATOR = (connection1, connection2) -> connection2.getPresentableText().toLowerCase().compareTo(connection1.getPresentableText().toLowerCase());

    /*********************************************************
     *                       TreeElement                     *
     *********************************************************/
    @Override
    public String getQualifiedName() {
        return getPresentableText();
    }

    @Override
    @NotNull
    public String getName() {
        return getSettings().getDatabaseSettings().getName();
    }

    @Override
    public String getDescription() {
        return getSettings().getDatabaseSettings().getDescription();
    }

    @Override
    public String getPresentableText(){
        return getSettings().getDatabaseSettings().getName();
    }

    @Override
    public Icon getIcon(){
        if (connectionStatus.isConnected()) {
            return
                connectionStatus.isBusy() ? Icons.CONNECTION_BUSY :
                connectionStatus.isActive() ? Icons.CONNECTION_ACTIVE :
                    Icons.CONNECTION_CONNECTED;
        } else {
            return connectionStatus.isValid() ?
                    Icons.CONNECTION_INACTIVE :
                    Icons.CONNECTION_INVALID;
        }
    }

    @Nullable
    @Override
    public ConnectionHandler getConnection() {
        return this;
    }

    @Override
    @Deprecated
    public boolean hasUncommittedChanges() {
        List<DBNConnection> connections = getConnections(ConnectionType.MAIN, ConnectionType.SESSION);
        for (DBNConnection connection : connections) {
            if (connection.hasDataChanges()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void disposeInner() {
        nullify();
    }
}
