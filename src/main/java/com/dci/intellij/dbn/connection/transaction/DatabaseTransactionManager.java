package com.dci.intellij.dbn.connection.transaction;

import com.dci.intellij.dbn.common.component.ProjectComponentBase;
import com.dci.intellij.dbn.common.component.ProjectManagerListener;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.load.ProgressMonitor;
import com.dci.intellij.dbn.common.option.InteractiveOptionBroker;
import com.dci.intellij.dbn.common.routine.ProgressRunnable;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.util.Editors;
import com.dci.intellij.dbn.common.util.InternalApi;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.connection.*;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.resource.ui.ResourceMonitorDialog;
import com.dci.intellij.dbn.connection.session.DatabaseSession;
import com.dci.intellij.dbn.connection.transaction.options.TransactionManagerSettings;
import com.dci.intellij.dbn.connection.transaction.ui.PendingTransactionsDetailDialog;
import com.dci.intellij.dbn.connection.transaction.ui.PendingTransactionsDialog;
import com.dci.intellij.dbn.options.ProjectSettingsManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dci.intellij.dbn.common.component.Components.projectService;
import static com.dci.intellij.dbn.common.message.MessageCallback.when;
import static com.dci.intellij.dbn.common.util.Commons.list;
import static com.dci.intellij.dbn.common.util.Lists.isLast;
import static com.dci.intellij.dbn.connection.transaction.TransactionAction.*;

public class DatabaseTransactionManager extends ProjectComponentBase implements ProjectManagerListener {

    public static final String COMPONENT_NAME = "DBNavigator.Project.TransactionManager";

    private DatabaseTransactionManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseTransactionManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseTransactionManager.class);
    }

    public void rollback(ConnectionHandler connection, @NotNull DBNConnection conn) {
        DatabaseSession session = connection.getSessionBundle().getSession(conn.getSessionId());
        Messages.showQuestionDialog(getProject(),
                "Rollback Session",
                "Are you sure you want to rollback the session \"" + session.getName() + "\" for connection\"" + connection.getName() + "\"" ,
                Messages.OPTIONS_YES_NO, 0,
                option -> when(option == 0, () ->
                        execute(connection,
                                conn,
                                actions(ROLLBACK),
                                false,
                                null)));
    }

    public void commit(ConnectionHandler connection, @NotNull DBNConnection conn) {
        DatabaseSession session = connection.getSessionBundle().getSession(conn.getSessionId());
        Messages.showQuestionDialog(ensureProject(),
                "Commit Session",
                "Are you sure you want to commit the session \"" + session.getName() + "\" for connection\"" + connection.getName() + "\"" ,
                Messages.OPTIONS_YES_NO, 0,
                option -> when(option == 0, () ->
                        execute(connection,
                                conn,
                                actions(COMMIT),
                                false,
                                null)));
    }

    public void execute(
            @NotNull ConnectionHandler connection,
            @Nullable DBNConnection conn,
            @NotNull List<TransactionAction> actions,
            boolean background,
            @Nullable Runnable callback) {

        if (conn == null) {
            List<DBNConnection> connections = connection.getConnections(ConnectionType.MAIN, ConnectionType.SESSION);
            for (DBNConnection dbnConnection : connections) {
                Runnable executionCallback = isLast(connections, dbnConnection) ? callback : null;
                execute(connection, dbnConnection, actions, background, executionCallback);
            }
        } else {
            Project project = connection.getProject();
            if (InternalApi.isAppDisposeInProgress()) {
                executeActions(connection, conn, actions, callback);
            } else {
                String connectionName = connection.getConnectionName(conn);
                String actionName = actions.get(0).getName();

                String title = "Performing \"" + actionName + "\" on connection " + connectionName;
                ProgressRunnable executor = progress -> executeActions(connection, conn, actions, callback);

                if (background)
                    Progress.background(project, title, false, executor); else
                    Progress.prompt(project, title, false, executor);
            }
        }
    }


    private void executeActions(
            @NotNull ConnectionHandler connection,
            @NotNull DBNConnection conn,
            @NotNull List<TransactionAction> actions,
            @Nullable Runnable callback) {
        try {
            Project project = getProject();
            for (TransactionAction action : actions) {
                executeAction(connection, conn, project, action);
            }
            if (callback != null) {
                callback.run();
            }
        } catch (ProcessCanceledException ignore) {}
    }

    private void executeAction(
            @NotNull ConnectionHandler connection,
            @NotNull DBNConnection conn,
            @NotNull Project project,
            @NotNull TransactionAction action) {


        String connectionName = connection.getConnectionName(conn);
        AtomicBoolean success = new AtomicBoolean(true);
        try {
            // notify pre-action
            ProjectEvents.notify(project,
                    TransactionListener.TOPIC,
                    (listener) -> listener.beforeAction(connection, conn, action));

            ProgressMonitor.setProgressDetail("Performing " + action.getName() + " on connection " + connectionName);

            action.execute(connection, conn);
            if (action.getNotificationType() != null) {
                sendNotification(
                        action.getNotificationType(),
                        action.getGroup(),
                        action.getSuccessNotificationMessage(),
                        connectionName);
            }
        } catch (SQLException ex) {
            sendNotification(
                    action.getFailureNotificationType(),
                    action.getGroup(),
                    action.getFailureNotificationMessage(),
                    connectionName,
                    ex);
            success.set(false);
        } finally {
            if (Failsafe.check(project)) {
                // notify post-action
                ProjectEvents.notify(project,
                        TransactionListener.TOPIC,
                        (listener) -> listener.afterAction(connection, conn, action, success.get()));

                if (action.isStatusChange()) {
                    ConnectionId connectionId = connection.getConnectionId();
                    ProjectEvents.notify(project,
                            ConnectionHandlerStatusListener.TOPIC,
                            (listener) -> listener.statusChanged(connectionId));
                }
            }
        }
    }

    public void commit(
            @NotNull ConnectionHandler connection,
            @Nullable DBNConnection conn,
            boolean fromEditor,
            boolean background,
            @Nullable Runnable callback) {

        List<TransactionAction> actions = actions(COMMIT);

        List<DBNConnection> connections = conn == null ?
                connection.getConnections(ConnectionType.MAIN, ConnectionType.SESSION) :
                Collections.singletonList(conn);

        for (DBNConnection c : connections) {
            Runnable commitCallback = isLast(connections, c) ? callback : null;

            PendingTransactionBundle dataChanges = c.getDataChanges();
            if (fromEditor && dataChanges != null && dataChanges.size() > 1) {
                Project project = connection.getProject();
                VirtualFile selectedFile = Editors.getSelectedFile(project);

                if (selectedFile != null) {
                    String connectionName = connection.getConnectionName(c);
                    String fileUrl = selectedFile.getPresentableUrl();

                    getSettings().getCommitMultipleChanges().resolve(
                            list(connectionName, fileUrl),
                            option -> {
                                switch (option) {
                                    case COMMIT: execute(connection, c, actions, background, commitCallback); break;
                                    case REVIEW_CHANGES: showPendingTransactionsDialog(connection, null); break;
                                }
                            });
                }
            } else {
                execute(connection, c, actions, background, commitCallback);
            }
        }

    }

    public void rollback(
            @NotNull ConnectionHandler connection,
            @Nullable DBNConnection conn,
            boolean fromEditor,
            boolean background,
            @Nullable Runnable callback) {

        List<TransactionAction> actions = actions(ROLLBACK);

        List<DBNConnection> connections = conn == null ?
                connection.getConnections(ConnectionType.MAIN, ConnectionType.SESSION) :
                Collections.singletonList(conn);

        for (DBNConnection c : connections) {
            Runnable rollbackCallback = isLast(connections, c) ? callback : null;

            PendingTransactionBundle dataChanges = c.getDataChanges();
            if (fromEditor && dataChanges != null && dataChanges.size() > 1) {
                Project project = connection.getProject();
                VirtualFile selectedFile = Editors.getSelectedFile(project);
                if (selectedFile != null) {
                    String connectionName = connection.getConnectionName(c);

                    getSettings().getRollbackMultipleChanges().resolve(
                            list(connectionName, selectedFile.getPresentableUrl()),
                            option -> {
                                switch (option) {
                                    case ROLLBACK: execute(connection, c, actions, background, rollbackCallback); break;
                                    case REVIEW_CHANGES: showPendingTransactionsDialog(connection, null); break;
                                }
                            });
                }
            } else {
                execute(connection, c, actions, background, rollbackCallback);
            }
        }
    }

    public void disconnect(ConnectionHandler connection, boolean background, @Nullable Runnable callback) {
        List<DBNConnection> connections = connection.getConnections();
        for (DBNConnection conn : connections) {
            Runnable disconnectCallback = isLast(connections, conn) ? callback : null;
            execute(connection, conn, actions(DISCONNECT), background, disconnectCallback);
        }
    }

    public TransactionManagerSettings getSettings() {
        ProjectSettingsManager projectSettingsManager = ProjectSettingsManager.getInstance(getProject());
        return projectSettingsManager.getOperationSettings().getTransactionManagerSettings();
    }


    public void showResourceMonitorDialog() {
        ResourceMonitorDialog resourceMonitorDialog = new ResourceMonitorDialog(getProject());
        resourceMonitorDialog.show();
    }

    public boolean showPendingTransactionsOverviewDialog(@Nullable TransactionAction additionalOperation) {
        PendingTransactionsDialog executionDialog = new PendingTransactionsDialog(getProject(), additionalOperation);
        executionDialog.show();
        return executionDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE;
    }

    public boolean showPendingTransactionsDialog(ConnectionHandler connection, @Nullable TransactionAction additionalOperation) {
        PendingTransactionsDetailDialog executionDialog = new PendingTransactionsDetailDialog(connection, additionalOperation, false);
        executionDialog.show();
        return executionDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE;
    }

    public void toggleAutoCommit(ConnectionHandler connection) {
        boolean autoCommit = connection.isAutoCommit();
        TransactionAction autoCommitAction = autoCommit ?
                TURN_AUTO_COMMIT_OFF :
                TURN_AUTO_COMMIT_ON;

        List<DBNConnection> connections = connection.getConnections(ConnectionType.MAIN, ConnectionType.SESSION);
        connection.setAutoCommit(!autoCommit);
        for (DBNConnection conn : connections) {
            if (!autoCommit && conn.hasDataChanges()) {
                String connectionName = connection.getConnectionName(conn);

                getSettings().getToggleAutoCommit().resolve(
                        list(connectionName),
                        option -> {
                            switch (option) {
                                case COMMIT:   execute(connection, conn, actions(COMMIT, autoCommitAction), true, null); break;
                                case ROLLBACK: execute(connection, conn, actions(ROLLBACK, autoCommitAction), true, null); break;
                                case REVIEW_CHANGES: showPendingTransactionsDialog(connection, autoCommitAction);
                            }});
            } else {
                execute(connection, conn, actions(autoCommitAction), false, null);
            }
        }
    }

    public void disconnect(ConnectionHandler connection) {
        List<DBNConnection> connections = connection.getConnections();
        for (DBNConnection conn : connections) {
            if (conn.hasDataChanges()) {
                String connectionName = connection.getConnectionName(conn);

                getSettings().getDisconnect().resolve(
                        list(connectionName),
                        option -> {
                            switch (option) {
                                case COMMIT:   execute(connection, conn, actions(COMMIT, DISCONNECT), false, null);break;
                                case ROLLBACK: execute(connection, conn, actions(DISCONNECT), false, null); break;
                                case REVIEW_CHANGES: showPendingTransactionsDialog(connection, DISCONNECT);
                            }
                        });
            } else {
                execute(connection, conn, actions(DISCONNECT), false, null);
            }
        }
    }

    public boolean canCloseProject() {
        Project project = getProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        if (!connectionManager.hasUncommittedChanges()) return true;

        boolean exitApp = InternalApi.isAppExitInProgress();

        TransactionManagerSettings transactionManagerSettings = getSettings();
        InteractiveOptionBroker<TransactionOption> closeProjectOptionHandler = transactionManagerSettings.getCloseProject();

        closeProjectOptionHandler.resolve(
                list(project.getName()),
                option -> {
                    switch (option) {
                        case COMMIT: {
                            commitAll(() -> closeProject(exitApp));
                            break;
                        }
                        case ROLLBACK: {
                            rollbackAll(() -> closeProject(exitApp));
                            break;
                        }
                        case REVIEW_CHANGES: {
                            showPendingTransactionsOverviewDialog(null);
                            break;
                        }
                    }
                });

        return false;
    }

    private void commitAll(@Nullable Runnable callback) {
        Project project = getProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        List<ConnectionHandler> connections = connectionManager.getConnections(
                connection -> connection.hasUncommittedChanges());

        for (ConnectionHandler connection : connections) {
            Runnable commitCallback = isLast(connections, connection) ? callback : null;
            commit(connection, null, false, false, commitCallback);
        }
    }

    private void rollbackAll(@Nullable Runnable callback) {
        Project project = getProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        List<ConnectionHandler> connections = connectionManager.getConnections(
                connection -> connection.hasUncommittedChanges());

        for (ConnectionHandler connection : connections) {
            Runnable rollbackCallback = isLast(connections, connection) ? callback : null;
            rollback(connection, null, false, false, rollbackCallback);
        }
    }
}