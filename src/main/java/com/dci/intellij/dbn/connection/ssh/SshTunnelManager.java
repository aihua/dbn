package com.dci.intellij.dbn.connection.ssh;

import com.dci.intellij.dbn.common.component.ApplicationComponentBase;
import com.dci.intellij.dbn.common.database.DatabaseInfo;
import com.dci.intellij.dbn.connection.config.ConnectionDatabaseSettings;
import com.dci.intellij.dbn.connection.config.ConnectionSettings;
import com.dci.intellij.dbn.connection.config.ConnectionSshTunnelSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dci.intellij.dbn.common.component.Components.applicationService;
import static com.intellij.openapi.util.text.StringUtil.parseInt;

public class SshTunnelManager extends ApplicationComponentBase {
    private final Map<SshTunnelConfig, SshTunnelConnector> sshTunnelConnectors = new ConcurrentHashMap<>();

    public SshTunnelManager() {
        super("DBNavigator.SshTunnelManager");
    }

    public static SshTunnelManager getInstance() {
        return applicationService(SshTunnelManager.class);
    }

    public SshTunnelConnector ensureSshConnection(ConnectionSettings connectionSettings) throws Exception {
        ConnectionSshTunnelSettings sshSettings = connectionSettings.getSshTunnelSettings();
        if (!sshSettings.isActive()) return null;

        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        SshTunnelConfig config = createConfig(databaseSettings, sshSettings);
        SshTunnelConnector connector = sshTunnelConnectors.computeIfAbsent(config, c -> new SshTunnelConnector(c));

        if (!connector.isConnected()) connector.connect();
        return connector;
    }

    @NotNull
    private static SshTunnelConfig createConfig(ConnectionDatabaseSettings databaseSettings, ConnectionSshTunnelSettings sshSettings) {
        DatabaseInfo databaseInfo = databaseSettings.getDatabaseInfo();

        String proxyHost = sshSettings.getHost();
        int proxyPort = parseInt(sshSettings.getPort(), -1);

        String remoteHost = databaseInfo.getHost();
        int remotePort = parseInt(databaseInfo.getPort(), -1);

        return new SshTunnelConfig(
                proxyHost,
                proxyPort,
                sshSettings.getUser(),
                sshSettings.getAuthType(),
                sshSettings.getKeyFile(),
                sshSettings.getKeyPassphrase(),
                sshSettings.getPassword(),
                remoteHost,
                remotePort);
    }
}
