package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.common.database.AuthenticationInfo;
import com.dci.intellij.dbn.common.notification.NotificationGroup;
import com.dci.intellij.dbn.common.notification.NotificationSupport;
import com.dci.intellij.dbn.common.thread.Timeout;
import com.dci.intellij.dbn.common.util.Classes;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.config.ConnectionDatabaseSettings;
import com.dci.intellij.dbn.connection.config.ConnectionSettings;
import com.dci.intellij.dbn.connection.config.ConnectionSshTunnelSettings;
import com.dci.intellij.dbn.connection.config.ConnectionSslSettings;
import com.dci.intellij.dbn.connection.config.file.DatabaseFile;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.ssh.SshTunnelConnector;
import com.dci.intellij.dbn.connection.ssh.SshTunnelManager;
import com.dci.intellij.dbn.connection.ssl.SslConnectionManager;
import com.dci.intellij.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.dci.intellij.dbn.common.exception.Exceptions.toSqlException;
import static com.dci.intellij.dbn.common.util.Commons.nvl;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dci.intellij.dbn.diagnostics.data.Activity.CONNECT;

@Getter
class Connector {
    private interface Property {
        String APPLICATION_NAME = "ApplicationName";
        String SESSION_PROGRAM = "v$session.program";

        String USER = "user";
        String PASSWORD = "password";

        String SSL = "ssl";
        String USE_SSL = "useSSL";
        String REQUIRE_SSL = "requireSSL";
    }

    private final SessionId sessionId;
    private final AuthenticationInfo authenticationInfo;
    private final ConnectionSettings connectionSettings;
    private final ConnectionHandlerStatusHolder connectionStatus;
    private final DatabaseAttachmentHandler databaseAttachmentHandler;
    private final boolean autoCommit;
    private SQLException exception;

    Connector(
            SessionId sessionId,
            AuthenticationInfo authenticationInfo,
            ConnectionSettings connectionSettings,
            ConnectionHandlerStatusHolder connectionStatus,
            DatabaseAttachmentHandler databaseAttachmentHandler,
            boolean autoCommit) {
        this.sessionId = sessionId;
        this.authenticationInfo = authenticationInfo;
        this.connectionSettings = connectionSettings;
        this.connectionStatus = connectionStatus;
        this.databaseAttachmentHandler = databaseAttachmentHandler;
        this.autoCommit = autoCommit;
    }


    private int getConnectTimeout() {
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        boolean driversLoaded = databaseSettings.driversLoaded();
        int connectTimeoutExtension = driversLoaded ? 0 : 20; // allow 20 seconds for drivers to load
        int connectTimeout = connectTimeoutExtension + connectionSettings.getDetailSettings().getConnectivityTimeoutSeconds();
        return connectTimeout;
    }



    @Nullable
    public DBNConnection connect() {
        int connectTimeout = getConnectTimeout();
        return Timeout.call(connectTimeout, null, true, () -> doConnect());
    }

    private DBNConnection doConnect() {
        //trace(this);
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        try {
            Properties properties = new Properties();

            // AUTHENTICATION
            AuthenticationInfo authenticationInfo = databaseSettings.getAuthenticationInfo();
            if (!authenticationInfo.isProvided() && this.authenticationInfo != null) {
                authenticationInfo = this.authenticationInfo;
            }

            AuthenticationType authenticationType = authenticationInfo.getType();
            if (authenticationType.isOneOf(AuthenticationType.USER, AuthenticationType.USER_PASSWORD)) {
                String user = authenticationInfo.getUser();
                if (Strings.isNotEmpty(user)) {
                    properties.put(Property.USER, user);
                }

                if (authenticationType == AuthenticationType.USER_PASSWORD) {
                    String password = authenticationInfo.getPassword();
                    if (Strings.isNotEmpty(password)) {
                        properties.put(Property.PASSWORD, password);
                    }
                }
            }

            DatabaseType databaseType = databaseSettings.getDatabaseType();
            if (databaseType == DatabaseType.GENERIC) {
                databaseType = DatabaseType.resolve(databaseSettings.getDriver());
            }

            // SESSION INFO
            ConnectionType connectionType = sessionId.getConnectionType();
            String appName = "Database Navigator - " + connectionType.getName();
            if (connectionSettings.isSigned()) {
                properties.put(Property.APPLICATION_NAME, appName);
            }

            if (databaseType == DatabaseType.ORACLE) {
                properties.put(Property.SESSION_PROGRAM, appName);
            }

            Map<String, String> configProperties = databaseSettings.getParent().getPropertiesSettings().getProperties();
            if (configProperties != null) {
                properties.putAll(configProperties);
            }

            // DRIVER
            Driver driver = ConnectionUtil.resolveDriver(databaseSettings);
            if (driver == null) {
                throw new SQLException("Could not resolve driver class.");
            }

            // SSL
            ConnectionSslSettings sslSettings = connectionSettings.getSslSettings();
            if (sslSettings.isActive()) {
                SslConnectionManager connectionManager = SslConnectionManager.getInstance();
                connectionManager.ensureSslConnection(connectionSettings);
                if (databaseType == DatabaseType.MYSQL) {
                    properties.setProperty(Property.USE_SSL, "true");
                    properties.setProperty(Property.REQUIRE_SSL, "true");
                } else if (databaseType == DatabaseType.POSTGRES) {
                    properties.setProperty(Property.SSL, "true");
                }
            }

            String connectionUrl = databaseSettings.getConnectionUrl();

            // SSH Tunnel
            ConnectionSshTunnelSettings sshTunnelSettings = connectionSettings.getSshTunnelSettings();
            if (sshTunnelSettings.isActive()) {
                SshTunnelManager sshTunnelManager = SshTunnelManager.getInstance();
                SshTunnelConnector sshTunnelConnector = sshTunnelManager.ensureSshConnection(connectionSettings);
                if (sshTunnelConnector != null) {
                    String localHost = sshTunnelConnector.getLocalHost();
                    String localPort = Integer.toString(sshTunnelConnector.getLocalPort());
                    connectionUrl = databaseSettings.getConnectionUrl(localHost, localPort);
                }
            }
            Diagnostics.databaseLag(CONNECT);

            Connection connection = connect(driver, connectionUrl, properties);
            if (connection == null) {
                throw new SQLException("Driver failed to create connection " + connectionUrl + ". No failure information provided by jdbc vendor.");
            }

            if (connectionStatus != null) {
                connectionStatus.setConnectionException(null);
                connectionStatus.setConnected(true);
                connectionStatus.setValid(true);
            }

            Project project = connectionSettings.getProject();
            if (databaseAttachmentHandler != null) {
                List<DatabaseFile> attachedFiles = databaseSettings.getDatabaseInfo().getAttachedFiles();
                for (DatabaseFile databaseFile : attachedFiles) {
                    String filePath = databaseFile.getPath();
                    try {
                        databaseAttachmentHandler.attachDatabase(connection, filePath, databaseFile.getSchema());
                    } catch (Exception e) {
                        conditionallyLog(e);
                        NotificationSupport.sendErrorNotification(
                                project,
                                NotificationGroup.CONNECTION,
                                "Unable to attach database file {0}: {1}", filePath, e);
                    }
                }
            }

            DatabaseMetaData metaData = connection.getMetaData();
            databaseType = ConnectionUtil.getDatabaseType(metaData);
            databaseSettings.setConfirmedDatabaseType(databaseType);
            databaseSettings.setDatabaseVersion(ConnectionUtil.getDatabaseVersion(metaData));
            databaseSettings.setConnectivityStatus(ConnectivityStatus.VALID);
            DBNConnection conn = DBNConnection.wrap(
                    project,
                    connection,
                    connectionSettings.getDatabaseSettings().getName(),
                    connectionType,
                    connectionSettings.getConnectionId(),
                    sessionId);

            Resources.setAutoCommit(conn, autoCommit);
            return conn;

        } catch (Throwable e) {
            conditionallyLog(e);
            String message = nvl(e.getMessage(), e.getClass().getSimpleName());
            if (connectionSettings.isSigned()) {
                // DBN-524 strongly asserted property names
                if (message.contains(Property.APPLICATION_NAME)) {
                    connectionSettings.setSigned(false);
                    return connect();
                }
            }

            DatabaseType databaseType = DatabaseType.resolve(databaseSettings.getDriver());
            databaseSettings.setConfirmedDatabaseType(databaseType);
            databaseSettings.setConnectivityStatus(ConnectivityStatus.INVALID);
            if (connectionStatus != null) {
                connectionStatus.setConnectionException(e);
                connectionStatus.setValid(false);
            }
            exception = toSqlException(e, "Connection error: " + message);
        }
        return null;
    }

    private static Connection connect(Driver driver, String url, Properties properties) throws SQLException {
        return Classes.withClassLoader(driver, d -> d.connect(url, properties));
    }
}
