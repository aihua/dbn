package com.dci.intellij.dbn.editor.code;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.component.PersistentState;
import com.dci.intellij.dbn.common.component.ProjectComponentBase;
import com.dci.intellij.dbn.common.component.ProjectManagerListener;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.editor.BasicTextEditor;
import com.dci.intellij.dbn.common.editor.document.OverrideReadonlyFragmentModificationHandler;
import com.dci.intellij.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.load.ProgressMonitor;
import com.dci.intellij.dbn.common.navigation.NavigationInstructions;
import com.dci.intellij.dbn.common.notification.NotificationGroup;
import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.util.*;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.Resources;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dci.intellij.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.EditorProviderId;
import com.dci.intellij.dbn.editor.code.content.SourceCodeContent;
import com.dci.intellij.dbn.editor.code.diff.MergeAction;
import com.dci.intellij.dbn.editor.code.diff.SourceCodeDiffManager;
import com.dci.intellij.dbn.editor.code.options.CodeEditorConfirmationSettings;
import com.dci.intellij.dbn.editor.code.options.CodeEditorSettings;
import com.dci.intellij.dbn.editor.console.SQLConsoleEditorListener;
import com.dci.intellij.dbn.execution.statement.DataDefinitionChangeListener;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.dci.intellij.dbn.language.editor.DBLanguageFileEditorListener;
import com.dci.intellij.dbn.language.psql.PSQLFile;
import com.dci.intellij.dbn.object.DBDatasetTrigger;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.dci.intellij.dbn.vfs.file.DBContentVirtualFile;
import com.dci.intellij.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dci.intellij.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.text.DateFormatUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import static com.dci.intellij.dbn.common.Priority.HIGH;
import static com.dci.intellij.dbn.common.Priority.HIGHEST;
import static com.dci.intellij.dbn.common.component.Components.projectService;
import static com.dci.intellij.dbn.common.message.MessageCallback.when;
import static com.dci.intellij.dbn.common.navigation.NavigationInstruction.*;
import static com.dci.intellij.dbn.common.util.Commons.list;
import static com.dci.intellij.dbn.common.util.Messages.*;
import static com.dci.intellij.dbn.common.util.Naming.unquote;
import static com.dci.intellij.dbn.database.DatabaseFeature.OBJECT_CHANGE_MONITORING;
import static com.dci.intellij.dbn.vfs.VirtualFileStatus.*;

@State(
    name = SourceCodeManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class SourceCodeManager extends ProjectComponentBase implements PersistentState, ProjectManagerListener {
    public static final String COMPONENT_NAME = "DBNavigator.Project.SourceCodeManager";

    public static SourceCodeManager getInstance(@NotNull Project project) {
        return projectService(project, SourceCodeManager.class);
    }

    private SourceCodeManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(OverrideReadonlyFragmentModificationHandler.INSTANCE);

        ProjectEvents.subscribe(project, this, DataDefinitionChangeListener.TOPIC, dataDefinitionChangeListener());
        ProjectEvents.subscribe(project, this, EnvironmentManagerListener.TOPIC, environmentManagerListener());
        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, new DBLanguageFileEditorListener());
        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, new SQLConsoleEditorListener());
    }


    @NotNull
    private DataDefinitionChangeListener dataDefinitionChangeListener() {
        return new DataDefinitionChangeListener() {
            @Override
            public void dataDefinitionChanged(DBSchema schema, DBObjectType objectType) {
            }

            @Override
            public void dataDefinitionChanged(@NotNull DBSchemaObject schemaObject) {
                DBEditableObjectVirtualFile databaseFile = schemaObject.getCachedVirtualFile();
                if (databaseFile != null) {
                    if (databaseFile.isModified()) {
                        showQuestionDialog(
                                getProject(), "Unsaved changes",
                                "The " + schemaObject.getQualifiedNameWithType() + " has been updated in database. You have unsaved changes in the object editor.\n" +
                                        "Do you want to discard the changes and reload the updated database version?",
                                new String[]{"Reload", "Keep changes"}, 0,
                                option -> when(option == 0, () ->
                                        reloadAndUpdateEditors(databaseFile, false)));
                    } else {
                        reloadAndUpdateEditors(databaseFile, true);
                    }

                }
            }
        };
    }

    @NotNull
    private EnvironmentManagerListener environmentManagerListener() {
        return new EnvironmentManagerListener() {
            @Override
            public void editModeChanged(Project project, DBContentVirtualFile databaseContentFile) {
                if (databaseContentFile instanceof DBSourceCodeVirtualFile) {
                    DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) databaseContentFile;
                    if (sourceCodeFile.is(MODIFIED)) {
                        loadSourceCode(sourceCodeFile, true);
                    }
                }
            }
        };
    }

    @NotNull
    private FileEditorManagerListener fileEditorManagerListener() {
        return new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                FileEditor newEditor = event.getNewEditor();
                if (newEditor instanceof SourceCodeEditor) {
                    SourceCodeEditor sourceCodeEditor = (SourceCodeEditor) newEditor;
                    DBEditableObjectVirtualFile databaseFile = sourceCodeEditor.getVirtualFile().getMainDatabaseFile();
                    for (DBSourceCodeVirtualFile sourceCodeFile : databaseFile.getSourceCodeFiles()) {
                        if (!sourceCodeFile.isLoaded()) {
                            loadSourceCode(sourceCodeFile, false);
                        }
                    }

                }

            }
        };
    }

    private void reloadAndUpdateEditors(DBEditableObjectVirtualFile databaseFile, boolean startInBackground) {
        if (!databaseFile.isContentLoaded()) return;

        if (startInBackground) {
            Background.run(getProject(), () -> reloadAndUpdateEditors(databaseFile));
        } else {
            DBSchemaObject object = databaseFile.getObject();
            Progress.prompt(getProject(), object, false,
                    "Loading source code",
                    "Reloading object source code for " + object.getQualifiedNameWithType(),
                    progress -> reloadAndUpdateEditors(databaseFile));
        }
    }

    private void reloadAndUpdateEditors(DBEditableObjectVirtualFile databaseFile) {
        List<DBSourceCodeVirtualFile> sourceCodeFiles = databaseFile.getSourceCodeFiles();
        for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
            loadSourceCode(sourceCodeFile, true);
        }
    }

    public void ensureSourcesLoaded(@NotNull DBSchemaObject schemaObject, boolean notifyError) {
        DBEditableObjectVirtualFile editableObjectFile = schemaObject.getEditableVirtualFile();
        List<DBSourceCodeVirtualFile> sourceCodeFiles = editableObjectFile.getSourceCodeFiles();
        for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
            if (!sourceCodeFile.isLoaded()) {
                loadSourceFromDatabase(sourceCodeFile, false, notifyError);
            }
        }
    }

    private void loadSourceFromDatabase(@NotNull DBSourceCodeVirtualFile sourceCodeFile, boolean force, boolean notifyError) {
        boolean initialLoad = !sourceCodeFile.isLoaded();
        if (sourceCodeFile.isNot(LOADING) && (initialLoad || force)) {
            sourceCodeFile.set(LOADING, true);
            Editors.setEditorsReadonly(sourceCodeFile, true);
            Project project = getProject();
            DBSchemaObject object = sourceCodeFile.getObject();

            ProjectEvents.notify(project,
                    SourceCodeManagerListener.TOPIC,
                    (listener) -> listener.sourceCodeLoading(sourceCodeFile));
            try {
                sourceCodeFile.loadSourceFromDatabase();
            } catch (SQLException e) {
                sourceCodeFile.setSourceLoadError(e.getMessage());
                sourceCodeFile.set(MODIFIED, false);
                if (notifyError) {
                    String objectDesc = object.getQualifiedNameWithType();
                    sendErrorNotification(
                            NotificationGroup.SOURCE_CODE,
                            "Could not load sourcecode for {0} from database: {1}", objectDesc, e);
                }
            } finally {
                sourceCodeFile.set(LOADING, false);
                ProjectEvents.notify(project,
                        SourceCodeManagerListener.TOPIC,
                        (listener) -> listener.sourceCodeLoaded(sourceCodeFile, initialLoad));
            }
        }
    }

    private void saveSourceToDatabase(@NotNull DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor, @Nullable Runnable successCallback) {
        if (!sourceCodeFile.isNot(SAVING)) return;

        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(getProject());
        if (!debuggerManager.checkForbiddenOperation(sourceCodeFile.getConnection())) return;

        sourceCodeFile.set(SAVING, true);
        Project project = getProject();
        try {
            Document document = Failsafe.nn(Documents.getDocument(sourceCodeFile));
            Documents.saveDocument(document);

            if (!isValidObjectHeader(sourceCodeFile)) return;

            DBSchemaObject object = sourceCodeFile.getObject();
            ProgressMonitor.setProgressDetail("Checking for third party changes on " + object.getQualifiedNameWithType());

            boolean changedInDatabase = sourceCodeFile.isChangedInDatabase(true);
            if (changedInDatabase && sourceCodeFile.isMergeRequired()) {
                String presentableChangeTime =
                        OBJECT_CHANGE_MONITORING.isSupported(object) ?
                                DateFormatUtil.formatPrettyDateTime(sourceCodeFile.getDatabaseChangeTimestamp()).toLowerCase() : "";
                String message =
                        "The " + object.getQualifiedNameWithType() +
                                " was changed in database by another user " + presentableChangeTime + "." +
                                "\nYou must merge the changes before saving.";

                showWarningDialog(project, "Version conflict", message,
                        options("Merge Changes", "Cancel"), 0,
                        option -> {
                            if (option == 0) {
                                Progress.prompt(project, object, false,
                                        "Loading source code",
                                        "Loading database source code for " + object.getQualifiedNameWithType(),
                                        progress -> openCodeMergeDialog(sourceCodeFile, fileEditor));
                            } else {
                                sourceCodeFile.set(SAVING, false);
                            }
                        });

            } else {
                storeSourceToDatabase(sourceCodeFile, fileEditor, successCallback);
            }

        } catch (Exception ex) {
            showErrorDialog(project, "Could not save changes to database.", ex);
            sourceCodeFile.set(SAVING, false);
        }
    }

    private void openCodeMergeDialog(@NotNull DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor) {
        Project project = getProject();
        try {
            DBSchemaObject object = sourceCodeFile.getObject();
            DBContentType contentType = sourceCodeFile.getContentType();
            SourceCodeContent sourceCodeContent = loadSourceFromDatabase(object, contentType);
            String databaseContent = sourceCodeContent.getText().toString();
            SourceCodeDiffManager diffManager = SourceCodeDiffManager.getInstance(project);
            diffManager.openCodeMergeDialog(databaseContent, sourceCodeFile, fileEditor, MergeAction.SAVE);
        } catch (SQLException e) {
            showErrorDialog(project, "Could not load database sources.", e);
        }
    }

    private boolean isValidObjectHeader(@NotNull DBSourceCodeVirtualFile sourceCodeFile) {
        DBSchemaObject object = sourceCodeFile.getObject();
        DBContentType contentType = sourceCodeFile.getContentType();
        DBLanguagePsiFile psiFile = sourceCodeFile.getPsiFile();

        if (psiFile != null && psiFile.getFirstChild() != null && !isValidObjectTypeAndName(psiFile, object, contentType)) {
            String message = "You are not allowed to change the name or the type of the object";
            sourceCodeFile.set(SAVING, false);
            showErrorDialog(getProject(), "Illegal action", message);
            return false;
        }
        return true;
    }

    public SourceCodeContent loadSourceFromDatabase(@NotNull DBSchemaObject object, DBContentType contentType) throws SQLException {
        String sourceCode = DatabaseInterfaceInvoker.load(HIGH,
                "Loading source code",
                "Loading source code of " + object.getQualifiedNameWithType(),
                object.getProject(),
                object.getConnectionId(),
                conn -> loadSourceFromDatabase(object, contentType, conn));

        SourceCodeContent sourceCodeContent = new SourceCodeContent(sourceCode);

        String objectName = object.getName();
        DBObjectType objectType = object.getObjectType();

        DatabaseDataDefinitionInterface dataDefinition = object.getDataDefinitionInterface();
        dataDefinition.computeSourceCodeOffsets(sourceCodeContent, objectType.getTypeId(), objectName);
        return sourceCodeContent;
    }

    @NotNull
    private static String loadSourceFromDatabase(@NotNull DBSchemaObject object, DBContentType contentType, DBNConnection conn) throws SQLException {
        boolean optionalContent = contentType == DBContentType.CODE_BODY;
        ResultSet resultSet = null;
        try {
            DatabaseMetadataInterface metadata = object.getMetadataInterface();
            resultSet = loadSourceFromDatabase(
                    object,
                    contentType,
                    metadata,
                    conn);

            StringBuilder buffer = new StringBuilder();
            while (resultSet != null && resultSet.next()) {
                String codeLine = resultSet.getString("SOURCE_CODE");
                buffer.append(codeLine);
            }

            if (buffer.length() == 0 && !optionalContent)
                throw new SQLException("Source lookup returned empty");

            return Strings.removeCharacter(buffer.toString(), '\r');
        } finally {
            Resources.close(resultSet);
        }
    }

    @Nullable
    private static ResultSet loadSourceFromDatabase(
            @NotNull DBSchemaObject object,
            DBContentType contentType,
            DatabaseMetadataInterface metadata,
            @NotNull DBNConnection connection) throws SQLException {

        DBObjectType objectType = object.getObjectType();
        String schemaName = object.getSchemaName();
        String objectName = object.getName();
        short objectOverload = object.getOverload();

        switch (objectType) {
            case VIEW:
                return metadata.loadViewSourceCode(
                        schemaName,
                        objectName,
                        connection);

            case MATERIALIZED_VIEW:
                return metadata.loadMaterializedViewSourceCode(
                        schemaName,
                        objectName,
                        connection);

            case DATABASE_TRIGGER:
                return metadata.loadDatabaseTriggerSourceCode(
                        schemaName,
                        objectName,
                        connection);

            case DATASET_TRIGGER:
                DBDatasetTrigger trigger = (DBDatasetTrigger) object;
                String datasetSchemaName = trigger.getDataset().getSchemaName();
                String datasetName = trigger.getDataset().getName();
                return metadata.loadDatasetTriggerSourceCode(
                        datasetSchemaName,
                        datasetName,
                        schemaName,
                        objectName,
                        connection);

            case FUNCTION:
                return metadata.loadObjectSourceCode(
                        schemaName,
                        objectName,
                        "FUNCTION",
                        objectOverload,
                        connection);

            case PROCEDURE:
                return metadata.loadObjectSourceCode(
                        schemaName,
                        objectName,
                        "PROCEDURE",
                        objectOverload,
                        connection);

            case TYPE:
                String typeContent =
                        contentType == DBContentType.CODE_SPEC ? "TYPE" :
                        contentType == DBContentType.CODE_BODY ? "TYPE BODY" : null;

                return metadata.loadObjectSourceCode(
                        schemaName,
                        objectName,
                        typeContent,
                        connection);

            case PACKAGE:
                String packageContent =
                        contentType == DBContentType.CODE_SPEC ? "PACKAGE" :
                        contentType == DBContentType.CODE_BODY ? "PACKAGE BODY" : null;

                return metadata.loadObjectSourceCode(
                        schemaName,
                        objectName,
                        packageContent,
                        connection);

            default:
                return null;
        }
    }

    @NotNull
    public ChangeTimestamp loadChangeTimestamp(@NotNull DBSchemaObject object, DBContentType contentType) throws SQLException{
        if (OBJECT_CHANGE_MONITORING.isNotSupported(object)) return ChangeTimestamp.now();

        Timestamp timestamp = DatabaseInterfaceInvoker.load(HIGHEST,
                "Loading object details",
                "Loading change timestamp for " + object.getQualifiedNameWithType(),
                object.getProject(),
                object.getConnectionId(),
                conn -> {
                    ResultSet resultSet = null;
                    try {
                        String schemaName = object.getSchemaName();
                        String objectName = object.getName();
                        String contentQualifier = getContentQualifier(object.getObjectType(), contentType);

                        DatabaseMetadataInterface metadata = object.getMetadataInterface();
                        resultSet = metadata.loadObjectChangeTimestamp(
                                schemaName,
                                objectName,
                                contentQualifier,
                                conn);

                        return resultSet.next() ? resultSet.getTimestamp(1) : null;
                    } finally {
                        Resources.close(resultSet);
                    }
                });

        if (timestamp != null) return ChangeTimestamp.of(timestamp);


        return ChangeTimestamp.now();
    }

    private static String getContentQualifier(DBObjectType objectType, DBContentType contentType) {
        switch (objectType) {
            case FUNCTION:         return "FUNCTION";
            case PROCEDURE:        return "PROCEDURE";
            case VIEW:             return "VIEW";
            case DATASET_TRIGGER:  return "TRIGGER";
            case DATABASE_TRIGGER: return "TRIGGER";
            case PACKAGE:
                return
                    contentType == DBContentType.CODE_SPEC ? "PACKAGE" :
                    contentType == DBContentType.CODE_BODY ? "PACKAGE BODY" : null;
            case TYPE:
                return
                    contentType == DBContentType.CODE_SPEC ? "TYPE" :
                    contentType == DBContentType.CODE_BODY ? "TYPE BODY" : null;
        }
        return null;
    }

    private boolean isValidObjectTypeAndName(@NotNull DBLanguagePsiFile psiFile, @NotNull DBSchemaObject object, DBContentType contentType) {
        ConnectionHandler connection = object.getConnection();
        DatabaseDataDefinitionInterface dataDefinition = connection.getDataDefinitionInterface();
        if (dataDefinition.includesTypeAndNameInSourceContent(object.getObjectType().getTypeId())) {
            PsiElement psiElement = PsiUtil.getFirstLeaf(psiFile);

            String typeName = object.getTypeName();
            String subtypeName = contentType.getObjectTypeSubname();
            String objectName = object.getName();
            String schemaName = object.getSchemaName();

            if (psiElement == null || !Strings.equalsIgnoreCase(psiElement.getText(), typeName)) {
                return false;
            }

            if (subtypeName != null) {
                psiElement = PsiUtil.getNextLeaf(psiElement);
                if (psiElement == null || !Strings.equalsIgnoreCase(psiElement.getText(), subtypeName)) {
                    return false;
                }
            }

            psiElement = PsiUtil.getNextLeaf(psiElement);
            if (psiElement == null) {
                return false;
            }

            if (Strings.equalsIgnoreCase(text(psiElement), schemaName)) {
                psiElement = PsiUtil.getNextLeaf(psiElement) ;
                if (psiElement == null || !Objects.equals(psiElement.getText(), ".")) {
                    return false;
                } else {
                    psiElement = PsiUtil.getNextLeaf(psiElement);
                    if (psiElement == null || !Strings.equalsIgnoreCase(text(psiElement), objectName)) {
                        return false;
                    }
                }
            } else {
                if (!Strings.equalsIgnoreCase(text(psiElement), objectName)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String text(@NotNull PsiElement psiElement) {
        return unquote(psiElement.getText());
    }

    public void storeSourceToDatabase(DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor, @Nullable Runnable successCallback) {
        Project project = getProject();
        DBSchemaObject object = sourceCodeFile.getObject();
        Progress.prompt(project, object, false,
                "Saving source code",
                "Saving sources of " + object.getQualifiedNameWithType() + " to database",
                progress -> {
                    try {
                        sourceCodeFile.saveSourceToDatabase();
                        ProjectEvents.notify(project,
                                SourceCodeManagerListener.TOPIC,
                                (listener) -> listener.sourceCodeSaved(sourceCodeFile, fileEditor));

                    } catch (SQLException e) {
                        showErrorDialog(project, "Could not save changes to database.", e);
                    } finally {
                        sourceCodeFile.set(SAVING, false);
                    }
                    if (successCallback != null) successCallback.run();
                });
    }

    public BasePsiElement getObjectNavigationElement(@NotNull DBSchemaObject parentObject, DBContentType contentType, DBObjectType objectType, CharSequence objectName) {
        DBEditableObjectVirtualFile editableObjectFile = parentObject.getEditableVirtualFile();
        DBContentVirtualFile contentFile = editableObjectFile.getContentFile(contentType);
        if (contentFile != null) {
            PSQLFile file = (PSQLFile) PsiUtil.getPsiFile(getProject(), contentFile);
            if (file != null) {
                return
                    contentType == DBContentType.CODE_BODY ? file.lookupObjectDeclaration(objectType, objectName) :
                    contentType == DBContentType.CODE_SPEC ? file.lookupObjectSpecification(objectType, objectName) : null;
            }
        }

        return null;
    }

    public void navigateToObject(@NotNull DBSchemaObject parentObject, @NotNull BasePsiElement basePsiElement) {
        DBEditableObjectVirtualFile editableObjectFile = parentObject.getEditableVirtualFile();
        DBLanguagePsiFile psiFile = basePsiElement.getFile();
        VirtualFile elementVirtualFile = psiFile.getVirtualFile();
        if (elementVirtualFile instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) elementVirtualFile;
            BasicTextEditor textEditor = Editors.getTextEditor(sourceCodeFile);
            if (textEditor != null) {
                Project project = getProject();
                EditorProviderId editorProviderId = textEditor.getEditorProviderId();
                FileEditor fileEditor = Editors.selectEditor(project, textEditor, editableObjectFile, editorProviderId, NavigationInstructions.create(OPEN));
                basePsiElement.navigateInEditor(fileEditor, NavigationInstructions.create(FOCUS, SCROLL));
            }
        }
    }

    @Override
    public boolean canCloseProject() {
        boolean exitApp = InternalApi.isAppExitInProgress();
        boolean canClose = true;
        Project project = getProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile[] openFiles = fileEditorManager.getOpenFiles();

        for (VirtualFile openFile : openFiles) {
            if (openFile instanceof DBEditableObjectVirtualFile) {
                DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) openFile;
                if (databaseFile.isModified()) {
                    canClose = false;
                    if (!databaseFile.isSaving()) {
                        DBSchemaObject object = databaseFile.getObject();
                        String objectDescription = object.getQualifiedNameWithType();
                        Project objectProject = object.getProject();

                        CodeEditorSettings codeEditorSettings = CodeEditorSettings.getInstance(objectProject);
                        CodeEditorConfirmationSettings confirmationSettings = codeEditorSettings.getConfirmationSettings();
                        confirmationSettings.getExitOnChanges().resolve(
                                list(objectDescription),
                                option -> {
                                    switch (option) {
                                        case SAVE: saveSourceCodeChanges(databaseFile, () -> closeProject(exitApp)); break;
                                        case DISCARD: revertSourceCodeChanges(databaseFile, () -> closeProject(exitApp)); break;
                                        case SHOW: {
                                            List<DBSourceCodeVirtualFile> sourceCodeFiles = databaseFile.getSourceCodeFiles();
                                            for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
                                                if (sourceCodeFile.is(MODIFIED)) {
                                                    SourceCodeDiffManager diffManager = SourceCodeDiffManager.getInstance(objectProject);
                                                    diffManager.opedDatabaseDiffWindow(sourceCodeFile);
                                                }
                                            }

                                        } break;
                                        case CANCEL: break;
                                    }
                                });
                    }
                }
            }
        }
        return canClose;
    }

    public void loadSourceCode(@NotNull DBSourceCodeVirtualFile sourceCodeFile, boolean force) {
        ConnectionAction.invoke("loading the source code", false, sourceCodeFile,
                action -> Background.run(getProject(), () -> loadSourceFromDatabase(sourceCodeFile, force, false)));
    }

    public void saveSourceCode(@NotNull DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor, Runnable successCallback) {
        DBSchemaObject object = sourceCodeFile.getObject();
        ConnectionAction.invoke("saving the source code", false, sourceCodeFile,
                action -> Progress.prompt(getProject(), object, false,
                        "Saving source code",
                        "Saving source code for " + object.getQualifiedNameWithType(),
                        progress -> saveSourceToDatabase(sourceCodeFile, fileEditor, successCallback)));
    }

    public void revertSourceCodeChanges(@NotNull DBEditableObjectVirtualFile databaseFile, Runnable successCallback) {
        try {
            List<DBSourceCodeVirtualFile> sourceCodeFiles = databaseFile.getSourceCodeFiles();
            for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
                sourceCodeFile.revertLocalChanges();
            }
        } finally {
            if (successCallback != null) {
                successCallback.run();
            }
        }

/*
        String objectDescription = databaseFile.getObject().getQualifiedNameWithType();
        ConnectionAction.invoke("loading the source code", false, databaseFile,
                (action) -> Progress.background(getProject(), "Loading source code for " + objectDescription, false,
                        (progress) -> {
                            try {
                                List<DBSourceCodeVirtualFile> sourceCodeFiles = databaseFile.getSourceCodeFiles();
                                for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
                                    sourceCodeFile.revertLocalChanges();
                                }
                            } finally {
                                if (successCallback != null) {
                                    successCallback.run();
                                }
                            }
                        }));
*/
    }

    public void saveSourceCodeChanges(@NotNull DBEditableObjectVirtualFile databaseFile, Runnable successCallback) {
        DBSchemaObject object = databaseFile.getObject();
        ConnectionAction.invoke("saving the source code", false, databaseFile,
                action -> Progress.prompt(getProject(), object, false,
                        "Saving source code",
                        "Saving source code for " + object.getQualifiedNameWithType(),
                        progress -> {
                            List<DBSourceCodeVirtualFile> sourceCodeFiles = databaseFile.getSourceCodeFiles();
                            for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
                                if (sourceCodeFile.is(MODIFIED)) {
                                    saveSourceToDatabase(sourceCodeFile, null, successCallback);
                                }
                            }
                        }));
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
    }
}
