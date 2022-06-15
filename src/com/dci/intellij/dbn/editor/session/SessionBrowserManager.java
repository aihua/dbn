package com.dci.intellij.dbn.editor.session;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.SafeDisposer;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.notification.NotificationGroup;
import com.dci.intellij.dbn.common.option.InteractiveOptionBroker;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.thread.Read;
import com.dci.intellij.dbn.common.util.Lists;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.common.util.TimeUtil;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.Resources;
import com.dci.intellij.dbn.connection.jdbc.DBNResultSet;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.database.DatabaseInterface;
import com.dci.intellij.dbn.database.DatabaseMessageParserInterface;
import com.dci.intellij.dbn.database.DatabaseMetadataInterface;
import com.dci.intellij.dbn.editor.session.model.SessionBrowserModel;
import com.dci.intellij.dbn.editor.session.options.SessionBrowserSettings;
import com.dci.intellij.dbn.editor.session.options.SessionInterruptionOption;
import com.dci.intellij.dbn.options.ProjectSettingsManager;
import com.dci.intellij.dbn.vfs.file.DBSessionBrowserVirtualFile;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.dci.intellij.dbn.common.util.Commons.list;

@State(
    name = SessionBrowserManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class SessionBrowserManager extends AbstractProjectComponent implements PersistentStateComponent<Element> {

    public static final String COMPONENT_NAME = "DBNavigator.Project.SessionEditorManager";

    private final List<DBSessionBrowserVirtualFile> openFiles = ContainerUtil.createConcurrentList();
    private Timer timestampUpdater;

    private SessionBrowserManager(Project project) {
        super(project);

        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener);
    }

    public static SessionBrowserManager getInstance(@NotNull Project project) {
        return Failsafe.getComponent(project, SessionBrowserManager.class);
    }

    public SessionBrowserSettings getSessionBrowserSettings() {
        return ProjectSettingsManager.getInstance(getProject()).getOperationSettings().getSessionBrowserSettings();
    }

    public void openSessionBrowser(ConnectionHandler connection) {
        ConnectionAction.invoke("opening the session browser", false, connection,
                (action) -> {
                    Project project = getProject();
                    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                    DBSessionBrowserVirtualFile sessionBrowserFile = connection.getSessionBrowserFile();
                    fileEditorManager.openFile(sessionBrowserFile, true);
                });
    }

    public SessionBrowserModel loadSessions(DBSessionBrowserVirtualFile sessionBrowserFile) {
        ConnectionHandler connection = sessionBrowserFile.getConnection();

        try {
            return DatabaseInterface.call(true,
                    connection,
                    (provider, conn) -> {
                        DBNResultSet resultSet = null;
                        try {
                            resultSet = (DBNResultSet) provider.getMetadataInterface().loadSessions(conn);
                            return new SessionBrowserModel(connection, resultSet);
                        } finally {
                            Resources.close(resultSet);
                        }
                    });

        } catch (SQLException e) {
            SessionBrowserModel model = new SessionBrowserModel(connection);
            model.setLoadError(e.getMessage());
            return model;
        }
    }

    public String loadSessionCurrentSql(ConnectionHandler connection, Object sessionId) {
        if (DatabaseFeature.SESSION_CURRENT_SQL.isSupported(connection)) {
            try {
                return DatabaseInterface.call(true,
                        connection,
                        (provider, conn) -> {
                            ResultSet resultSet = null;
                            try {
                                resultSet = provider.getMetadataInterface().loadSessionCurrentSql(sessionId, conn);
                                if (resultSet.next()) {
                                    return resultSet.getString(1);
                                }
                            } finally {
                                Resources.close(resultSet);
                            }
                            return "";
                        });
            } catch (SQLException e) {
                sendWarningNotification(
                        NotificationGroup.SESSION_BROWSER,
                        "Could not load current session SQL: {0}", e);
            }
        }
        return "";
    }

    public void interruptSessions(@NotNull SessionBrowser sessionBrowser, Map<Object, Object> sessionIds, SessionInterruptionType type) {
        ConnectionHandler connection = Failsafe.nn(sessionBrowser.getConnection());
        if (DatabaseFeature.SESSION_INTERRUPTION_TIMING.isSupported(connection)) {

            SessionBrowserSettings sessionBrowserSettings = getSessionBrowserSettings();
            InteractiveOptionBroker<SessionInterruptionOption> disconnect =
                    type == SessionInterruptionType.TERMINATE ? sessionBrowserSettings.getKillSession() :
                    type == SessionInterruptionType.DISCONNECT  ? sessionBrowserSettings.getDisconnectSession() : null;

            if (disconnect != null) {
                String subject = sessionIds.size() > 1 ? "selected sessions" : "session with id \"" + sessionIds.keySet().iterator().next().toString() + "\"";
                disconnect.resolve(
                        list(subject, connection.getName()),
                        option -> {
                            if (option != SessionInterruptionOption.CANCEL && option != SessionInterruptionOption.ASK) {
                                doInterruptSessions(sessionBrowser, sessionIds, type, option);
                            }
                        });
            }
        } else {
            doInterruptSessions(sessionBrowser, sessionIds, SessionInterruptionType.TERMINATE, SessionInterruptionOption.NORMAL);
        }
    }

    private void doInterruptSessions(@NotNull SessionBrowser sessionBrowser, Map<Object, Object> sessionIds, SessionInterruptionType type, SessionInterruptionOption option) {
        int sessionCount = sessionIds.size();
        String disconnectedAction = type == SessionInterruptionType.TERMINATE ? "killed" : "disconnected";
        String disconnectingAction = type == SessionInterruptionType.TERMINATE ? "killing" : "disconnecting";
        String taskAction = (type == SessionInterruptionType.TERMINATE ? "Killing" : "Disconnecting") + (sessionCount == 1 ? " Session" : " Sessions");

        Project project = getProject();
        Progress.prompt(project, taskAction, true, progress -> {
            try {
                ConnectionHandler connection = sessionBrowser.getConnection();

                DatabaseInterface.run(true,
                        connection,
                        (provider, conn) -> {
                            progress.setIndeterminate(false);
                            Map<Object, SQLException> errors = new HashMap<>();
                            DatabaseMetadataInterface metadataInterface = provider.getMetadataInterface();
                            int index = 0;
                            for (val entry : sessionIds.entrySet()) {
                                Object sessionId = entry.getKey();
                                Object serialNumber = entry.getValue();

                                checkDisposed();
                                progress.checkCanceled();
                                progress.setText(Strings.capitalize(disconnectingAction) + " session id " + sessionId + " (serial " + serialNumber + ")");
                                progress.setFraction(Progress.progressOf(index, sessionCount));

                                try {
                                    boolean immediate = option == SessionInterruptionOption.IMMEDIATE;
                                    boolean postTransaction = option == SessionInterruptionOption.POST_TRANSACTION;
                                    switch (type) {
                                        case DISCONNECT: metadataInterface.disconnectSession(sessionId, serialNumber, postTransaction, immediate, conn); break;
                                        case TERMINATE:  metadataInterface.terminateSession(sessionId, serialNumber, immediate, conn);  break;
                                    }
                                } catch (SQLException e) {
                                    errors.put(sessionId, e);
                                }
                                index++;
                            }

                            DatabaseMessageParserInterface messageParserInterface = connection.getInterfaceProvider().getMessageParserInterface();
                            if (sessionCount == 1) {
                                Object sessionId = sessionIds.keySet().iterator().next();
                                if (errors.size() == 0) {
                                    Messages.showInfoDialog(project, "Info", "Session " + sessionId + " " + disconnectedAction + ".");
                                } else {
                                    SQLException exception = errors.get(sessionId);
                                    if (messageParserInterface.isSuccessException(exception)) {
                                        Messages.showInfoDialog(project, "Info", "Session " + sessionId + " " + disconnectingAction + " requested.\n" + exception.getMessage());
                                    } else {
                                        Messages.showErrorDialog(project, "Error " + disconnectingAction + " session " + sessionId + ".", exception);
                                    }

                                }
                            } else {
                                if (errors.size() == 0) {
                                    Messages.showInfoDialog(project, "Info", sessionCount + " sessions " + disconnectedAction + ".");
                                } else {
                                    StringBuilder message = new StringBuilder();
                                    boolean success = Lists.allMatch(errors.values(), error -> messageParserInterface.isSuccessException(error));
                                    if (success) {
                                        message.append(sessionCount);
                                        message.append(" sessions ");
                                        message.append(disconnectingAction);
                                        message.append(" requested:");
                                    } else {
                                        message.append("Error ");
                                        message.append(disconnectingAction);
                                        message.append(" one or more of the selected sessions:");
                                    }
                                    for (Object sessionId : sessionIds.keySet()) {
                                        SQLException exception = errors.get(sessionId);
                                        message.append("\n - session id ").append(sessionId).append(": ");
                                        if (exception == null) {
                                            message.append(disconnectedAction);
                                        } else {
                                            message.append(exception.getMessage().trim());
                                        }
                                    }
                                    if (success) {
                                        Messages.showInfoDialog(project, "Info", message.toString());
                                    } else {
                                        Messages.showErrorDialog(project, message.toString());
                                    }
                                }

                            }
                        });
            } catch (SQLException e) {
                Messages.showErrorDialog(project, "Error performing operation", e);
            } finally {
                sessionBrowser.loadSessions(false);
            }
        });
    }

    private class UpdateTimestampTask extends TimerTask {
        @Override
        public void run() {
            if (openFiles.size() > 0) {
                Read.run(() -> {
                    Project project = getProject();
                    if (!project.isDisposed()) {
                        List<SessionBrowser> sessionBrowsers = new ArrayList<>();
                        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                        FileEditor[] editors = fileEditorManager.getAllEditors();
                        for (FileEditor editor : editors) {
                            if (editor instanceof SessionBrowser) {
                                SessionBrowser sessionBrowser = (SessionBrowser) editor;
                                sessionBrowsers.add(sessionBrowser);
                            }
                        }

                        if (sessionBrowsers.size() > 0) {
                            Dispatch.run(() -> {
                                for (SessionBrowser sessionBrowser : sessionBrowsers) {
                                    sessionBrowser.refreshLoadTimestamp();
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    /****************************************
    *             ProjectComponent          *
    *****************************************/
    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    private final FileEditorManagerListener fileEditorManagerListener = new FileEditorManagerAdapter() {
        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (file instanceof DBSessionBrowserVirtualFile) {
                boolean schedule = openFiles.size() == 0;
                DBSessionBrowserVirtualFile sessionBrowserFile = (DBSessionBrowserVirtualFile) file;
                openFiles.add(sessionBrowserFile);

                if (schedule) {
                    timestampUpdater = new Timer("DBN - Session Browser (timestamp update timer)");
                    timestampUpdater.schedule(new UpdateTimestampTask(), TimeUtil.Millis.ONE_SECOND, TimeUtil.Millis.ONE_SECOND);
                }
            }
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (file instanceof DBSessionBrowserVirtualFile) {
                DBSessionBrowserVirtualFile sessionBrowserFile = (DBSessionBrowserVirtualFile) file;
                openFiles.remove(sessionBrowserFile);

                if (openFiles.size() == 0 && timestampUpdater != null) {
                    SafeDisposer.dispose(timestampUpdater);
                }
            }
        }
    };

    @Override
    public void disposeInner() {
        SafeDisposer.dispose(timestampUpdater);
        super.disposeInner();
    }


    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getState() {
        return new Element("state");
    }

    @Override
    public void loadState(@NotNull Element element) {
    }
}
