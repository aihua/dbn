package com.dci.intellij.dbn.connection.session;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.component.PersistentState;
import com.dci.intellij.dbn.common.component.ProjectComponentBase;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.routine.Consumer;
import com.dci.intellij.dbn.common.util.Dialogs;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.dci.intellij.dbn.connection.SessionId;
import com.dci.intellij.dbn.connection.session.ui.CreateRenameSessionDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dci.intellij.dbn.common.component.Components.projectService;
import static com.dci.intellij.dbn.common.message.MessageCallback.when;
import static com.dci.intellij.dbn.common.options.setting.Settings.*;

@State(
    name = DatabaseSessionManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseSessionManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseSessionManager";

    private DatabaseSessionManager(final Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseSessionManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseSessionManager.class);
    }

    public void showCreateSessionDialog(
            @NotNull ConnectionHandler connection,
            @Nullable Consumer<DatabaseSession> callback) {

        showCreateRenameSessionDialog(connection, null, callback);
    }

    public void showRenameSessionDialog(
            @NotNull DatabaseSession session,
            @Nullable Consumer<DatabaseSession> callback) {

        showCreateRenameSessionDialog(session.getConnection(), session, callback);
    }


    private void showCreateRenameSessionDialog(
            @NotNull ConnectionHandler connection,
            @Nullable DatabaseSession session,
            @Nullable Consumer<DatabaseSession> consumer) {

        Dialogs.show(() -> new CreateRenameSessionDialog(connection, session), (dialog, exitCode) -> {
            if (consumer == null) return;
            consumer.accept(dialog.getSession());
        });
    }

    public DatabaseSession createSession(ConnectionHandler connection, String name) {
        DatabaseSession session = connection.getSessionBundle().createSession(name);
        ProjectEvents.notify(getProject(),
                SessionManagerListener.TOPIC,
                (listener) -> listener.sessionCreated(session));
        return session;
    }

    public void renameSession(DatabaseSession session, String newName) {
        ConnectionHandler connection = session.getConnection();
        String oldName = session.getName();
        connection.getSessionBundle().renameSession(oldName, newName);
        ProjectEvents.notify(getProject(),
                SessionManagerListener.TOPIC,
                (listener) -> listener.sessionChanged(session));
    }

    public void deleteSession(final DatabaseSession session, boolean confirm) {
        if (confirm) {
            Messages.showQuestionDialog(
                    getProject(),
                    "Delete Session",
                    "Are you sure you want to delete the session \"" + session.getName() + "\" for connection\"" + session.getConnection().getName() + "\"" ,
                    Messages.OPTIONS_YES_NO, 0,
                    option -> when(option == 0, () -> deleteSession(session)));
        } else {
            deleteSession(session);
        }
    }

    public void deleteSession(@NotNull DatabaseSession session) {
        ConnectionHandler connection = session.getConnection();
        connection.getSessionBundle().deleteSession(session.getId());
        ProjectEvents.notify(getProject(),
                SessionManagerListener.TOPIC,
                (listener) -> listener.sessionDeleted(session));
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        ConnectionManager connectionManager = ConnectionManager.getInstance(getProject());
        List<ConnectionHandler> connections = connectionManager.getConnectionBundle().getAllConnections();
        for (ConnectionHandler connection : connections) {
            Element connectionElement = newElement(element, "connection");
            connectionElement.setAttribute("id", connection.getConnectionId().id());

            List<DatabaseSession> sessions = connection.getSessionBundle().getSessions();
            for (DatabaseSession session : sessions) {
                if (session.isCustom()) {
                    Element sessionElement = newElement(connectionElement, "session");
                    sessionElement.setAttribute("name", session.getName());
                    sessionElement.setAttribute("id", session.getId().id());
                }
            }
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        for (Element connectionElement : element.getChildren()) {
            ConnectionId connectionId = connectionIdAttribute(connectionElement, "id");
            ConnectionHandler connection = ConnectionHandler.get(connectionId);

            if (connection != null) {
                DatabaseSessionBundle sessionBundle = connection.getSessionBundle();
                for (Element sessionElement : connectionElement.getChildren()) {
                    String sessionName = stringAttribute(sessionElement, "name");
                    SessionId sessionId = SessionId.get(stringAttribute(sessionElement, "id"));
                    sessionBundle.addSession(sessionId, sessionName);
                }
            }
        }
    }
}
