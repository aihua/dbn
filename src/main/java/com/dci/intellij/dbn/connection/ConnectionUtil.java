package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.common.database.AuthenticationInfo;
import com.dci.intellij.dbn.common.thread.Timeout;
import com.dci.intellij.dbn.connection.config.ConnectionDatabaseSettings;
import com.dci.intellij.dbn.connection.config.ConnectionPropertiesSettings;
import com.dci.intellij.dbn.connection.config.ConnectionSettings;
import com.dci.intellij.dbn.connection.info.ConnectionInfo;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dci.intellij.dbn.database.interfaces.DatabaseMessageParserInterface;
import com.dci.intellij.dbn.diagnostics.DiagnosticsManager;
import com.dci.intellij.dbn.diagnostics.data.DiagnosticBundle;
import com.dci.intellij.dbn.driver.DatabaseDriverManager;
import com.dci.intellij.dbn.driver.DriverBundle;
import com.dci.intellij.dbn.driver.DriverSource;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;

import static com.dci.intellij.dbn.common.util.TimeUtil.millisSince;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;

public class ConnectionUtil {
    private ConnectionUtil() {}


    public static DBNConnection connect(ConnectionHandler connection, SessionId sessionId) throws SQLException {
        AuthenticationInfo authenticationInfo = ensureAuthenticationInfo(connection);

        return ConnectionContext.surround(connection.createConnectionContext(), () -> {
            long start = System.currentTimeMillis();
            ConnectionHandlerStatusHolder connectionStatus = connection.getConnectionStatus();

            DiagnosticsManager diagnosticsManager = DiagnosticsManager.getInstance(connection.getProject());
            DiagnosticBundle<SessionId> diagnostics = diagnosticsManager.getConnectivityDiagnostics(connection.getConnectionId());
            try {
                DatabaseCompatibilityInterface compatibility = connection.getCompatibilityInterface();
                DatabaseAttachmentHandler attachmentHandler = compatibility.getDatabaseAttachmentHandler();
                ConnectionSettings connectionSettings = connection.getSettings();
                ConnectionPropertiesSettings propertiesSettings = connectionSettings.getPropertiesSettings();


                DBNConnection conn = connect(
                        connectionSettings,
                        connectionStatus,
                        connection.getTemporaryAuthenticationInfo(),
                        sessionId,
                        propertiesSettings.isEnableAutoCommit(),
                        attachmentHandler);
                ConnectionInfo connectionInfo = new ConnectionInfo(conn.getMetaData());
                connection.setConnectionInfo(connectionInfo);
                connectionStatus.setAuthenticationError(null);
                connection.getCompatibility().read(conn.getMetaData());
                diagnostics.log(sessionId, false, false, millisSince(start));
                return conn;
            } catch (SQLTimeoutException e) {
                conditionallyLog(e);
                diagnostics.log(sessionId, false, true, millisSince(start));
                throw e;
            } catch (SQLException e) {
                conditionallyLog(e);
                diagnostics.log(sessionId, true, false, millisSince(start));
                DatabaseMessageParserInterface messageParser = connection.getMessageParserInterface();
                if (messageParser.isAuthenticationException(e)) {
                    authenticationInfo.setPassword(null);
                    connectionStatus.setAuthenticationError(new AuthenticationError(authenticationInfo, e));
                }
                throw e;
            }
        });
    }

    @NotNull
    private static AuthenticationInfo ensureAuthenticationInfo(ConnectionHandler connection) throws SQLException {
        // do not retry connection on authentication error unless
        // credentials changed (account can be locked on several invalid attempts)

        ConnectionHandlerStatusHolder statusHolder = connection.getConnectionStatus();
        AuthenticationError authenticationError = statusHolder.getAuthenticationError();
        AuthenticationInfo authenticationInfo = initAuthenticationInfo(connection);

        if (authenticationError != null && authenticationError.getAuthenticationInfo().isSame(authenticationInfo) && !authenticationError.isExpired()) {
            throw authenticationError.getException();
        }
        return authenticationInfo;
    }

    @NotNull
    private static AuthenticationInfo initAuthenticationInfo(ConnectionHandler connection) {
        ConnectionSettings connectionSettings = connection.getSettings();
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        AuthenticationInfo authenticationInfo = databaseSettings.getAuthenticationInfo();
        if (!authenticationInfo.isProvided()) {
            authenticationInfo = connection.getTemporaryAuthenticationInfo();
        }
        return authenticationInfo;
    }

    @NotNull
    public static DBNConnection connect(
            ConnectionSettings connectionSettings,
            @Nullable ConnectionHandlerStatusHolder connectionStatus,
            @Nullable AuthenticationInfo temporaryAuthenticationInfo,
            @NotNull SessionId sessionId,
            boolean autoCommit,
            @Nullable DatabaseAttachmentHandler attachmentHandler) throws SQLException {
        Connector connector = new Connector(
                sessionId,
                temporaryAuthenticationInfo,
                connectionSettings,
                connectionStatus,
                attachmentHandler,
                autoCommit);

        int connectTimeout = connectionSettings.getDetailSettings().getConnectivityTimeoutSeconds();
        DBNConnection connection = Timeout.call(connectTimeout, null, true, () -> connector.connect());

        SQLException exception = connector.getException();
        if (exception != null) {
            throw exception;
        }

        if (connection == null) {
            throw new SQLTimeoutException("Could not connect to database. Communication timeout");
        }

        return connection;
    }

    @Nullable
    static Driver resolveDriver(ConnectionDatabaseSettings databaseSettings) throws Exception {
        DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
        DriverSource driverSource = databaseSettings.getDriverSource();
        String driverClassName = databaseSettings.getDriver();
        if (StringUtil.isEmpty(driverClassName)) return null;

        DriverBundle drivers = null;
        if (driverSource == DriverSource.EXTERNAL) {
            File driverLibrary = databaseSettings.getDriverLibraryFile();
            if (driverLibrary == null) return null;

            drivers = driverManager.getDrivers(driverLibrary);

        } else if (driverSource == DriverSource.BUNDLED) {
            DatabaseType databaseType = databaseSettings.getDatabaseType();
            if (databaseType == null) return null;

            drivers = driverManager.getBundledDrivers(databaseType);
        }

        if (drivers == null || drivers.isEmpty()) return null;
        ConnectionId connectionId = databaseSettings.getConnectionId();

        return drivers.getDriver(driverClassName, connectionId);
    }

    public static double getDatabaseVersion(DatabaseMetaData databaseMetaData) throws SQLException {
        int majorVersion = databaseMetaData.getDatabaseMajorVersion();
        int minorVersion = databaseMetaData.getDatabaseMinorVersion();
        return Double.parseDouble(majorVersion + "." + minorVersion);
    }

    public static DatabaseType getDatabaseType(DatabaseMetaData databaseMetaData) throws SQLException {
        String productName = databaseMetaData.getDatabaseProductName();
        return DatabaseType.resolve(productName);
    }
}
