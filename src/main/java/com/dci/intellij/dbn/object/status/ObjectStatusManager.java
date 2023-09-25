package com.dci.intellij.dbn.object.status;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.browser.model.BrowserTreeEventListener;
import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.common.component.Components;
import com.dci.intellij.dbn.common.component.PersistentState;
import com.dci.intellij.dbn.common.component.ProjectComponentBase;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.load.ProgressMonitor;
import com.dci.intellij.dbn.common.notification.NotificationGroup;
import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.ui.tree.TreeEventType;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.DatabaseEntity;
import com.dci.intellij.dbn.connection.Resources;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dci.intellij.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.common.status.DBObjectStatus;
import com.dci.intellij.dbn.object.common.status.DBObjectStatusHolder;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static com.dci.intellij.dbn.common.Priority.LOW;
import static com.dci.intellij.dbn.database.DatabaseFeature.OBJECT_INVALIDATION;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dci.intellij.dbn.object.common.property.DBObjectProperty.DEBUGABLE;
import static com.dci.intellij.dbn.object.common.property.DBObjectProperty.INVALIDABLE;

@State(
    name = ObjectStatusManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ObjectStatusManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ObjectStatusManager";

    private ObjectStatusManager(final Project project) {
        super(project, COMPONENT_NAME);
    }

    public static ObjectStatusManager getInstance(@NotNull Project project) {
        return Components.projectService(project, ObjectStatusManager.class);
    }

    public void refreshObjectsStatus(DBSchema schema) throws SQLException {
        DatabaseInterfaceInvoker.schedule(LOW,
                "Refreshing object status",
                "Refreshing object status for " + schema.getQualifiedNameWithType(),
                getProject(),
                schema.getConnectionId(),
                conn -> refreshObjectsStatus(schema, conn));
    }

    public void refreshObjectsStatus(ConnectionHandler connection, @Nullable DBSchemaObject requester) {
        if (!OBJECT_INVALIDATION.isSupported(connection)) return;

        Background.run(getProject(), () -> {
            try {
                List<DBSchema> schemas = requester == null ?
                        connection.getObjectBundle().getSchemas() :
                        requester.getReferencingSchemas();

                DatabaseInterfaceInvoker.schedule(LOW,
                        "Refreshing object status",
                        "Refreshing object status for " + connection.getName(),
                        getProject(),
                        connection.getConnectionId(),
                        conn -> refreshObjectStatus(conn, schemas));
            } catch (SQLException e) {
                conditionallyLog(e);
                sendErrorNotification(
                        NotificationGroup.BROWSER,
                        "Error refreshing object status: {0}", e);
            }
        });
    }

    private void refreshObjectStatus(DBNConnection conn, List<DBSchema> schemas) throws SQLException {
        int size = schemas.size();
        for (int i = 0; i < size; i++) {
            DBSchema schema = schemas.get(i);
            ProgressMonitor.setProgressText("Refreshing object status on " + schema.getQualifiedNameWithType());
            ProgressMonitor.setProgressFraction(Progress.progressOf(i, size));
            refreshObjectsStatus(schema, conn);
        }
    }


    private void refreshObjectsStatus(DBSchema schema, DBNConnection conn) throws SQLException {
        Set<DatabaseEntity> entities = schema.resetObjectsStatus();
        refreshValidStatus(schema, entities, conn);
        refreshDebugStatus(schema, entities, conn);
        refreshBrowserNodes(entities);
    }

    private void refreshBrowserNodes(Set<DatabaseEntity> entities) {
        Project project = getProject();
        Background.run(project, () ->
                entities.forEach(n -> {
                    if (n instanceof BrowserTreeNode) {
                        BrowserTreeNode node = (BrowserTreeNode) n;
                        ProjectEvents.notify(project, BrowserTreeEventListener.TOPIC,
                                listener -> listener.nodeChanged(node, TreeEventType.NODES_CHANGED));
                    }
                }));
    }

    private void refreshValidStatus(DBSchema schema, Set<DatabaseEntity> entities, DBNConnection conn) throws SQLException {
        ResultSet resultSet = null;
        try {
            DatabaseMetadataInterface metadata = schema.getMetadataInterface();
            resultSet = metadata.loadInvalidObjects(schema.getName(), conn);
            while (resultSet != null && resultSet.next()) {
                String objectName = resultSet.getString("OBJECT_NAME");
                DBSchemaObject schemaObject = schema.getChildObjectNoLoad(objectName);
                if (schemaObject != null && schemaObject.is(INVALIDABLE)) {
                    DBObjectStatusHolder objectStatus = schemaObject.getStatus();
                    boolean statusChanged;

                    if (schemaObject.getContentType().isBundle()) {
                        String objectType = resultSet.getString("OBJECT_TYPE");
                        statusChanged = objectType.contains("BODY") ?
                                objectStatus.set(DBContentType.CODE_BODY, DBObjectStatus.VALID, false) :
                                objectStatus.set(DBContentType.CODE_SPEC, DBObjectStatus.VALID, false);
                    } else {
                        statusChanged = objectStatus.set(DBObjectStatus.VALID, false);
                    }
                    if (statusChanged) {
                        entities.add(schemaObject.getParent());
                    }
                }
            }
        } finally {
            Resources.close(resultSet);
        }
    }

    private void refreshDebugStatus(DBSchema schema, Set<DatabaseEntity> entities, DBNConnection conn) throws SQLException {
        ResultSet resultSet = null;
        try {
            DatabaseMetadataInterface metadata = schema.getMetadataInterface();
            resultSet = metadata.loadDebugObjects(schema.getName(), conn);
            while (resultSet != null && resultSet.next()) {
                String objectName = resultSet.getString("OBJECT_NAME");
                DBSchemaObject schemaObject = schema.getChildObjectNoLoad(objectName);
                if (schemaObject != null && schemaObject.is(DEBUGABLE)) {
                    DBObjectStatusHolder objectStatus = schemaObject.getStatus();
                    boolean statusChanged;

                    if (schemaObject.getContentType().isBundle()) {
                        String objectType = resultSet.getString("OBJECT_TYPE");
                        statusChanged = objectType.contains("BODY") ?
                                objectStatus.set(DBContentType.CODE_BODY, DBObjectStatus.DEBUG, true) :
                                objectStatus.set(DBContentType.CODE_SPEC, DBObjectStatus.DEBUG, true);
                    } else {
                        statusChanged = objectStatus.set(DBObjectStatus.DEBUG, true);
                    }
                    if (statusChanged) {
                        entities.add(schemaObject.getParent());
                    }
                }
            }
        } finally {
            Resources.close(resultSet);
        }
    }

    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull final Element element) {
    }

}
