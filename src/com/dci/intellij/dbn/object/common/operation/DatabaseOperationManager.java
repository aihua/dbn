package com.dci.intellij.dbn.object.common.operation;

import com.dci.intellij.dbn.common.component.Components;
import com.dci.intellij.dbn.common.component.ProjectComponentBase;
import com.dci.intellij.dbn.database.DatabaseInterface;
import com.dci.intellij.dbn.database.DatabaseMetadataInterface;
import com.dci.intellij.dbn.object.DBConstraint;
import com.dci.intellij.dbn.object.DBTrigger;
import com.dci.intellij.dbn.object.common.status.DBObjectStatus;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class DatabaseOperationManager extends ProjectComponentBase {

    public static final String COMPONENT_NAME = "DBNavigator.Project.OperationManager";

    private DatabaseOperationManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseOperationManager getInstance(@NotNull Project project) {
        return Components.projectService(project, DatabaseOperationManager.class);
    }

    public void enableConstraint(DBConstraint constraint) throws SQLException {
        DatabaseInterface.run(true,
                constraint.getConnection(),
                (provider, connection) -> {
                    DatabaseMetadataInterface metadataInterface = provider.getMetadataInterface();
                    metadataInterface.enableConstraint(
                            constraint.getSchema().getName(),
                            constraint.getDataset().getName(),
                            constraint.getName(),
                            connection);
                    constraint.getStatus().set(DBObjectStatus.ENABLED, true);
                });
    }

    public void disableConstraint(DBConstraint constraint) throws SQLException {
        DatabaseInterface.run(true,
                constraint.getConnection(),
                (provider, connection) -> {
                    DatabaseMetadataInterface metadataInterface = provider.getMetadataInterface();
                    metadataInterface.disableConstraint(
                            constraint.getSchema().getName(),
                            constraint.getDataset().getName(),
                            constraint.getName(),
                            connection);
                    constraint.getStatus().set(DBObjectStatus.ENABLED, true);
                });
    }

    public void enableTrigger(DBTrigger trigger) throws SQLException {
        DatabaseInterface.run(true,
                trigger.getConnection(),
                (provider, connection) -> {
                    DatabaseMetadataInterface metadataInterface = provider.getMetadataInterface();
                    metadataInterface.enableTrigger(
                            trigger.getSchema().getName(),
                            trigger.getName(),
                            connection);
                    trigger.getStatus().set(DBObjectStatus.ENABLED, true);
                });
    }

    public void disableTrigger(DBTrigger trigger) throws SQLException {
        DatabaseInterface.run(true,
                trigger.getConnection(),
                (provider, connection) -> {
                    DatabaseMetadataInterface metadataInterface = provider.getMetadataInterface();
                    metadataInterface.disableTrigger(
                            trigger.getSchema().getName(),
                            trigger.getName(),
                            connection);
                    trigger.getStatus().set(DBObjectStatus.ENABLED, true);
                });
    }
}
