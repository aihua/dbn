package com.dci.intellij.dbn.credentials;

import com.dci.intellij.dbn.common.thread.ReadActionRunner;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.BaseComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DatabaseCredentialManager implements BaseComponent {
    public static boolean USE = false;

    public static DatabaseCredentialManager getInstance() {
        return ApplicationManager.getApplication().getComponent(DatabaseCredentialManager.class);
    }


    public void removePassword(ConnectionId connectionId, String userName) {
        setPassword(connectionId, userName, null);
    }

    public void setPassword(ConnectionId connectionId, String userName, @Nullable String password) {
        try {
            String key = getPasswordKey(connectionId, userName);
            PasswordSafe passwordSafe = PasswordSafe.getInstance();

            if (StringUtil.isEmpty(password)) {
                passwordSafe.removePassword(null, this.getClass(), key);
            } else {
                passwordSafe.storePassword(null, this.getClass(), key, password);
            }
            System.out.println("Setting: " + key + " " + password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public String getPassword(ConnectionId connectionId, String userName) {
        return ReadActionRunner.invoke(false, () -> {
            try {
                String key = getPasswordKey(connectionId, userName);
                PasswordSafe passwordSafe = PasswordSafe.getInstance();
                String password = passwordSafe.getPassword(null, DatabaseCredentialManager.this.getClass(), key);
                System.out.println("Getting: " + key + " " + password);
                return password;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private boolean isMemoryStorage() {
        return false;//PasswordSafe.getInstance().isMemoryOnly();
    }

    @NotNull
    protected String getPasswordKey(ConnectionId connectionId, String userName) {
        return "DBNavigator.Connection." + connectionId + "." + userName;
    }

    @Override
    public void initComponent() {}

    @Override
    public void disposeComponent() { }

    @NotNull
    @Override
    public String getComponentName() {
        return "DBNavigator.DatabaseCredentialManager";
    }
}
