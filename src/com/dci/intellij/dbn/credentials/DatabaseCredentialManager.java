package com.dci.intellij.dbn.credentials;

import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.BaseComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class DatabaseCredentialManager implements BaseComponent {

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
        } catch (Exception e) {

        }
    }

    @Nullable
    public String getPassword(ConnectionId connectionId, String userName) {
        try {
            String key = getPasswordKey(connectionId, userName);
            PasswordSafe passwordSafe = PasswordSafe.getInstance();
            return passwordSafe.getPassword(null, this.getClass(), key);
        } catch (Exception e) {

            return null;
        }
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
