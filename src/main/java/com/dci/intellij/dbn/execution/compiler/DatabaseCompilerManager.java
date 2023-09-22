package com.dci.intellij.dbn.execution.compiler;

import com.dci.intellij.dbn.common.component.ProjectComponentBase;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.routine.Consumer;
import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.operation.options.OperationSettings;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dci.intellij.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.code.SourceCodeEditor;
import com.dci.intellij.dbn.editor.code.SourceCodeManagerListener;
import com.dci.intellij.dbn.execution.ExecutionManager;
import com.dci.intellij.dbn.execution.compiler.ui.CompilerTypeSelectionDialog;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.common.property.DBObjectProperty;
import com.dci.intellij.dbn.object.common.status.DBObjectStatus;
import com.dci.intellij.dbn.object.common.status.DBObjectStatusHolder;
import com.dci.intellij.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dci.intellij.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

import static com.dci.intellij.dbn.common.Priority.HIGH;
import static com.dci.intellij.dbn.common.Priority.LOW;
import static com.dci.intellij.dbn.common.component.Components.projectService;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dci.intellij.dbn.execution.compiler.CompilerActionSource.BULK_COMPILE;
import static com.dci.intellij.dbn.object.common.status.DBObjectStatus.COMPILING;

public class DatabaseCompilerManager extends ProjectComponentBase {
    private DatabaseCompilerManager(@NotNull Project project) {
        super(project, "DBNavigator.Project.CompilerManager");

        ProjectEvents.subscribe(project, this, SourceCodeManagerListener.TOPIC, sourceCodeManagerListener());
    }

    public static DatabaseCompilerManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseCompilerManager.class);
    }

    @NotNull
    private SourceCodeManagerListener sourceCodeManagerListener() {
        return new SourceCodeManagerListener() {
            @Override
            public void sourceCodeSaved(@NotNull DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor) {
                Project project = getProject();
                DBSchemaObject object = sourceCodeFile.getObject();
                DBContentType contentType = sourceCodeFile.getContentType();

                if (DatabaseFeature.OBJECT_INVALIDATION.isSupported(object)) {
                    boolean isCompilable = object.is(DBObjectProperty.COMPILABLE);

                    if (isCompilable) {
                        CompileType compileType = getCompileType(object, contentType);

                        CompilerAction compilerAction = new CompilerAction(CompilerActionSource.SAVE, contentType, sourceCodeFile, fileEditor);
                        if (compileType == CompileType.DEBUG) {
                            compileObject(object, compileType, compilerAction);
                        }
                        ConnectionHandler connection = object.getConnection();
                        ProjectEvents.notify(project,
                                CompileManagerListener.TOPIC,
                                (listener) -> listener.compileFinished(connection, object));

                        createCompilerResult(object, compilerAction, null);
                    }
                }
            }
        };
    }

    private static CompilerResult createCompilerResult(DBSchemaObject object, CompilerAction compilerAction, @Nullable DBNConnection conn) {
        return new CompilerResult(compilerAction, object, conn);
    }

    private static CompilerResult createErrorCompilerResult(CompilerAction compilerAction, DBSchemaObject object, DBContentType contentType, Exception e) {
        return new CompilerResult(compilerAction, object, contentType, "Could not perform compile operation. \nCause: " + e.getMessage());
    }

    public CompileType getCompileType(@Nullable DBSchemaObject object, DBContentType contentType) {
        OperationSettings operationSettings = OperationSettings.getInstance(getProject());
        CompileType compileType = operationSettings.getCompilerSettings().getCompileType();
        switch (compileType) {
            case KEEP: return object != null && object.getStatus().is(contentType, DBObjectStatus.DEBUG) ? CompileType.DEBUG : CompileType.NORMAL;
            case DEBUG: return CompileType.DEBUG;
        }
        return CompileType.NORMAL;
    }

    public void compileObject(DBSchemaObject object, CompileType compileType, CompilerAction compilerAction) {
        assert compileType != CompileType.KEEP;
        Project project = object.getProject();
        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
        boolean allowed = debuggerManager.checkForbiddenOperation(object.getConnection());
        if (!allowed) return;

        doCompileObject(object, compileType, compilerAction);
        updateFilesContentState(object, compilerAction.getContentType());
    }

    private void updateFilesContentState(DBSchemaObject object, DBContentType contentType) {
        Background.run(getProject(), () -> {
            DBEditableObjectVirtualFile databaseFile = object.getCachedVirtualFile();
            if (databaseFile != null && databaseFile.isContentLoaded()) {
                if (contentType.isBundle()) {
                    for (DBContentType subContentType : contentType.getSubContentTypes()) {
                        DBSourceCodeVirtualFile sourceCodeFile = databaseFile.getContentFile(subContentType);
                        if (sourceCodeFile != null) {
                            sourceCodeFile.refreshContentState();
                        }
                    }
                } else {
                    DBSourceCodeVirtualFile sourceCodeFile = databaseFile.getContentFile(contentType);
                    if (sourceCodeFile != null) {
                        sourceCodeFile.refreshContentState();
                    }
                }
            }
        });
    }

    public void compileInBackground(DBSchemaObject object, CompileType compileType, CompilerAction compilerAction) {
        Project project = getProject();
        ConnectionAction.invoke("compiling the object", false, object,
                action -> promptCompileTypeSelection(compileType, object, type -> Background.run(project, () -> {
                    doCompileObject(object, type, compilerAction);

                    ConnectionHandler connection = object.getConnection();
                    ProjectEvents.notify(project,
                            CompileManagerListener.TOPIC,
                            (listener) -> listener.compileFinished(connection, object));

                    DBContentType contentType = compilerAction.getContentType();
                    updateFilesContentState(object, contentType);
                })),
                null,
                action -> {
                    ConnectionHandler connection = action.getConnection();
                    DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
                    return debuggerManager.checkForbiddenOperation(connection);
                });
    }

    private void doCompileObject(DBSchemaObject object, CompileType compileType, CompilerAction compilerAction) {
        DBContentType contentType = compilerAction.getContentType();
        DBObjectStatusHolder objectStatus = object.getStatus();
        if (objectStatus.is(contentType, COMPILING)) return;

        CompilerResult compilerResult = null;

        try {
            objectStatus.set(contentType, COMPILING, true);
            compilerResult = DatabaseInterfaceInvoker.load(compilerAction.isBulkCompile() ? LOW : HIGH,
                    object.getProject(),
                    object.getConnectionId(),
                    conn -> doCompileObject(object, compileType, compilerAction, objectStatus, conn));
        } catch (Exception e) {
            conditionallyLog(e);
            compilerResult = createErrorCompilerResult(compilerAction, object, contentType, e);
        }  finally{
            objectStatus.set(contentType, COMPILING, false);
            if (compilerResult != null) {
                ExecutionManager executionManager = ExecutionManager.getInstance(getProject());
                executionManager.addCompilerResult(compilerResult);
            }
        }
    }

    private static CompilerResult doCompileObject(DBSchemaObject object, CompileType compileType, CompilerAction compilerAction, DBObjectStatusHolder objectStatus, DBNConnection conn) throws SQLException {
        DBContentType contentType = compilerAction.getContentType();
        ConnectionHandler connection = object.getConnection();
        DatabaseMetadataInterface metadata = connection.getMetadataInterface();

        boolean debug = compileType == CompileType.DEBUG;

        if (compileType == CompileType.KEEP) {
            debug = objectStatus.is(DBObjectStatus.DEBUG);
        }

        if (contentType == DBContentType.CODE_SPEC || contentType == DBContentType.CODE) {
            metadata.compileObject(
                    object.getSchemaName(),
                    object.getName(),
                    object.getTypeName().toUpperCase(),
                    debug,
                    conn);
        } else if (contentType == DBContentType.CODE_BODY) {
            metadata.compileObjectBody(
                    object.getSchemaName(),
                    object.getName(),
                    object.getTypeName().toUpperCase(),
                    debug,
                    conn);

        } else if (contentType == DBContentType.CODE_SPEC_AND_BODY) {
            metadata.compileObject(
                    object.getSchemaName(),
                    object.getName(),
                    object.getTypeName().toUpperCase(),
                    debug,
                    conn);
            metadata.compileObjectBody(
                    object.getSchemaName(),
                    object.getName(),
                    object.getTypeName().toUpperCase(),
                    debug,
                    conn);
        }

        return createCompilerResult(object, compilerAction, conn);
    }

    public void compileInvalidObjects(@NotNull DBSchema schema, CompileType compileType) {
        Project project = getProject();
        ConnectionAction.invoke("compiling the invalid objects", false, schema,
                action -> promptCompileTypeSelection(compileType, null,
                        type -> Progress.prompt(project, schema, true,
                                "Compiling invalid objects",
                                "Compiling invalid objects in " + schema.getQualifiedNameWithType(),
                                progress -> {
                                    progress.setIndeterminate(false);
                                    doCompileInvalidObjects(schema.getPackages(), "packages", progress, type);
                                    doCompileInvalidObjects(schema.getFunctions(), "functions", progress, type);
                                    doCompileInvalidObjects(schema.getProcedures(), "procedures", progress, type);
                                    doCompileInvalidObjects(schema.getDatasetTriggers(), "dataset triggers", progress, type);
                                    doCompileInvalidObjects(schema.getDatabaseTriggers(), "database triggers", progress, type);
                                    ConnectionHandler connection = schema.getConnection();
                                    ProjectEvents.notify(project,
                                            CompileManagerListener.TOPIC,
                                            (listener) -> listener.compileFinished(connection, null));
                                })),
                null,
                action -> {
                    DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
                    ConnectionHandler connection = schema.getConnection();
                    return debuggerManager.checkForbiddenOperation(connection);
                });
    }

    private void doCompileInvalidObjects(List<? extends DBSchemaObject> objects, String description, ProgressIndicator progress, CompileType compileType) {
        if (progress.isCanceled()) return;

        progress.setText("Compiling invalid " + description + "...");
        int count = objects.size();
        for (int i=0; i< count; i++) {
            if (progress.isCanceled() || objects.size() == 0 /* may be disposed meanwhile*/) {
                break;
            } else {
                DBSchemaObject object = objects.get(i);
                progress.setFraction(Progress.progressOf(i, count));
                DBObjectStatusHolder objectStatus = object.getStatus();
                DBContentType objectContentType = object.getContentType();
                if (objectContentType.isBundle()) {
                    for (DBContentType contentType : objectContentType.getSubContentTypes()) {
                        if (objectStatus.isNot(contentType, DBObjectStatus.VALID)) {
                            CompilerAction compilerAction = new CompilerAction(BULK_COMPILE, contentType);
                            doCompileObject(object, compileType, compilerAction);
                            progress.setText("Compiling " + object.getQualifiedNameWithType());
                        }
                    }
                } else {
                    if (objectStatus.isNot(DBObjectStatus.VALID)) {
                        CompilerAction compilerAction = new CompilerAction(BULK_COMPILE, objectContentType);
                        doCompileObject(object, compileType, compilerAction);
                        progress.setText("Compiling " + object.getQualifiedNameWithType());
                    }
                }
            }
        }
    }

    private void buildCompilationErrors(List<? extends DBSchemaObject> objects, List<CompilerResult> compilerErrors) {
        for (DBSchemaObject object : objects) {
            DBObjectStatusHolder objectStatus = object.getStatus();
            if (objectStatus.is(DBObjectStatus.VALID)) continue;

            CompilerAction compilerAction = new CompilerAction(BULK_COMPILE, object.getContentType());
            CompilerResult compilerResult = new CompilerResult(compilerAction, object, null);
            if (compilerResult.isError()) {
                compilerErrors.add(compilerResult);
            }
        }
    }

    private void promptCompileTypeSelection(
            CompileType compileType,
            @Nullable DBSchemaObject program,
            @NotNull Consumer<CompileType> callback) {

        if (compileType == CompileType.ASK) {
            CompilerTypeSelectionDialog dialog = new CompilerTypeSelectionDialog(getProject(), program);
            dialog.show();
            if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                compileType = dialog.getSelection();
                if (dialog.isRememberSelection()) {
                    OperationSettings operationSettings = OperationSettings.getInstance(getProject());
                    operationSettings.getCompilerSettings().setCompileType(compileType);
                }
                callback.accept(compileType);
            }
        } else {
            callback.accept(compileType);
        }
    }
}
