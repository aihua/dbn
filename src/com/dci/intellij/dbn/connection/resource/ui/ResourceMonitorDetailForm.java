package com.dci.intellij.dbn.connection.resource.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.form.DBNHeaderForm;
import com.dci.intellij.dbn.common.ui.component.DBNComponent;
import com.dci.intellij.dbn.common.ui.table.DBNTable;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.dci.intellij.dbn.connection.ConnectionPool;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.session.DatabaseSession;
import com.dci.intellij.dbn.connection.session.DatabaseSessionManager;
import com.dci.intellij.dbn.connection.session.SessionManagerListener;
import com.dci.intellij.dbn.connection.transaction.DatabaseTransactionManager;
import com.dci.intellij.dbn.connection.transaction.TransactionAction;
import com.dci.intellij.dbn.connection.transaction.TransactionListener;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.DumbAwareActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;

import static com.dci.intellij.dbn.common.message.MessageCallback.when;
import static com.dci.intellij.dbn.connection.transaction.TransactionAction.actions;


public class ResourceMonitorDetailForm extends DBNFormBase {
    private final DBNTable<ResourceMonitorSessionsTableModel> sessionsTable;
    private final DBNTable<ResourceMonitorTransactionsTableModel> transactionsTable;
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel sessionsPanel;
    private JBScrollPane transactionsTableScrollPane;
    private JLabel openTransactionsLabel;
    private JLabel sessionLabel;
    private JButton commitButton;
    private JButton rollbackButton;

    private final ConnectionRef connection;

    ResourceMonitorDetailForm(@NotNull DBNComponent parent, ConnectionHandler connection) {
        super(parent);
        this.connection = connection.ref();

        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        ResourceMonitorSessionsTableModel sessionsTableModel = new ResourceMonitorSessionsTableModel(connection);
        sessionsTable = new ResourceMonitorSessionsTable(this, sessionsTableModel);
        sessionsTable.getSelectionModel().addListSelectionListener(e -> {
            DBNConnection conn = getSelectedConnection();
            refreshTransactionsData(conn);
            updateTransactionActions();
        });

        ToolbarDecorator toolbar = ToolbarDecorator.createDecorator(sessionsTable);
        toolbar.addExtraAction(disconnectAction);
        toolbar.addExtraAction(renameSessionAction);
        toolbar.addExtraAction(deleteSessionAction);
        toolbar.setPreferredSize(new Dimension(-1, 400));
        sessionsPanel.add(toolbar.createPanel(), BorderLayout.CENTER);
        sessionsTable.getParent().setBackground(sessionsTable.getBackground());
        sessionLabel.setText("");

        // transactions table
        ResourceMonitorTransactionsTableModel transactionsTableModel = new ResourceMonitorTransactionsTableModel(connection, null);
        transactionsTable = new ResourceMonitorTransactionsTable(this, transactionsTableModel);
        transactionsTableScrollPane.setViewportView(transactionsTable);
        transactionsTableScrollPane.getViewport().setBackground(Colors.getTableBackground());

        ActionListener actionListener = e -> {
            Project project = connection.getProject();
            DBNConnection conn = getSelectedConnection();
            if (conn != null) {
                DatabaseTransactionManager transactionManager = getTransactionManager();
                Object source = e.getSource();
                if (source == commitButton) {
                    transactionManager.commit(connection, conn);
                } else if (source == rollbackButton) {
                    transactionManager.rollback(connection, conn);
                }
            }
        };

        commitButton.addActionListener(actionListener);
        commitButton.setIcon(Icons.CONNECTION_COMMIT);

        rollbackButton.addActionListener(actionListener);
        rollbackButton.setIcon(Icons.CONNECTION_ROLLBACK);
        updateTransactionActions();

        Project project = ensureProject();
        ProjectEvents.subscribe(project, this, TransactionListener.TOPIC, transactionListener);
        ProjectEvents.subscribe(project, this, SessionManagerListener.TOPIC, sessionManagerListener);
    }

    private void updateTransactionActions() {
        DBNConnection connection = getSelectedConnection();
        boolean enabled = connection != null && connection.hasDataChanges();
        commitButton.setEnabled(enabled);
        rollbackButton.setEnabled(enabled);
    }

    private final AnActionButton disconnectAction = new DumbAwareActionButton("Disconnect", null, Icons.ACTION_DISCONNECT_SESSION) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DatabaseSession session = getSelectedSession();
            if (session != null) {
                ConnectionHandler connection = getConnection();
                Messages.showQuestionDialog(getProject(),
                        "Disconnect Session",
                        "Are you sure you want to disconnect the session \"" + session.getName() + "\" for connection\"" + connection.getName() + "\"" ,
                        Messages.OPTIONS_YES_NO, 0,
                        option -> when(option == 0, () -> {
                            DBNConnection conn = getSelectedConnection();
                            if (conn != null) {
                                DatabaseTransactionManager transactionManager = getTransactionManager();
                                transactionManager.execute(
                                        connection,
                                        conn,
                                        actions(TransactionAction.DISCONNECT),
                                        false,
                                        null);
                            }
                        }));
            }
        }

        @Override
        public void updateButton(AnActionEvent e) {
            DBNConnection connection = getSelectedConnection();
            e.getPresentation().setEnabled(connection != null && !connection.isClosed());
        }

    };

    private final AnActionButton deleteSessionAction = new DumbAwareActionButton("Delete Session", null, Icons.ACTION_DELETE) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DatabaseSession session = getSelectedSession();
            if (session != null) {
                Messages.showQuestionDialog(getProject(),
                        "Delete Session",
                        "Are you sure you want to delete the session \"" + session.getName() + "\" for connection\"" + getConnection().getName() + "\"" ,
                        Messages.OPTIONS_YES_NO, 0,
                        option -> when(option == 0, () -> {
                            Project project = ensureProject();
                            DatabaseSessionManager sessionManager = DatabaseSessionManager.getInstance(project);
                            sessionManager.deleteSession(session);
                        }));
            }
        }

        @Override
        public void updateButton(AnActionEvent e) {
            DatabaseSession session = getSelectedSession();
            DBNConnection connection = getSelectedConnection();
            e.getPresentation().setEnabled(
                    session != null &&
                    session.isCustom() &&
                    (connection == null || !connection.hasDataChanges()));
        }
    };

    private final AnActionButton renameSessionAction = new DumbAwareActionButton("Rename Session", null, Icons.ACTION_EDIT) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DatabaseSession session = getSelectedSession();
            if (session != null) {
                DatabaseSessionManager sessionManager = DatabaseSessionManager.getInstance(ensureProject());
                sessionManager.showRenameSessionDialog(session, null);
            }
        }

        @Override
        public void updateButton(AnActionEvent e) {
            DatabaseSession session = getSelectedSession();
            DBNConnection connection = getSelectedConnection();
            e.getPresentation().setEnabled(
                    session != null &&
                            session.isCustom() &&
                            (connection == null || !connection.hasDataChanges()));
        }
    };

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    /********************************************************
     *                Transaction Listener                  *
     ********************************************************/
    private final TransactionListener transactionListener = new TransactionListener() {
        @Override
        public void afterAction(@NotNull ConnectionHandler connection, DBNConnection conn, TransactionAction action, boolean succeeded) {
            if (connection == getConnection() && transactionsTable.getModel().getConn() == conn && succeeded) {
                refreshTransactionsData(conn);
            }
        }
    };

    private final SessionManagerListener sessionManagerListener = new SessionManagerListener() {
        @Override
        public void sessionCreated(DatabaseSession session) {
            if (session.getConnection() == getConnection()) {
                refreshSessionData();
            }
        }

        @Override
        public void sessionDeleted(DatabaseSession session) {
            if (session.getConnection() == getConnection()) {
                refreshSessionData();
            }
        }

        @Override
        public void sessionChanged(DatabaseSession session) {
            if (session.getConnection() == getConnection()) {
                refreshSessionData();
            }
        }
    };

    private void refreshSessionData() {
        Dispatch.run(() -> {
            checkDisposed();
            ConnectionHandler connection = getConnection();

            ResourceMonitorSessionsTableModel sessionsTableModel = new ResourceMonitorSessionsTableModel(connection);
            sessionsTable.setModel(sessionsTableModel);
        });
    }

    private void refreshTransactionsData(DBNConnection conn) {
        Dispatch.run(() -> {
            checkDisposed();
            ConnectionHandler connection = getConnection();

            ResourceMonitorTransactionsTableModel transactionsTableModel = new ResourceMonitorTransactionsTableModel(connection, conn);
            transactionsTable.setModel(transactionsTableModel);
            DatabaseSession session = getSelectedSession();
            sessionLabel.setText(session == null ? "" : session.getName() + " (" + connection.getName() + ")");
            sessionLabel.setIcon(session == null ? null : session.getIcon());
            updateTransactionActions();
        });
    }

    @Nullable
    private DatabaseSession getSelectedSession() {
        ResourceMonitorSessionsTableModel sessionsTableModel = sessionsTable.getModel();
        return sessionsTableModel.getSession(sessionsTable.getSelectedRow());
    }

    @Nullable
    private DBNConnection getSelectedConnection() {
        DatabaseSession session = getSelectedSession();
        if (session != null) {
            ConnectionHandler connection = getConnection();
            ConnectionPool connectionPool = connection.getConnectionPool();
            return connectionPool.getSessionConnection(session.getId());
        }
        return null;
    }

    @NotNull
    private DatabaseTransactionManager getTransactionManager() {
        return DatabaseTransactionManager.getInstance(ensureProject());
    }

}
