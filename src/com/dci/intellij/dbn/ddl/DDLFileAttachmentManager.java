package com.dci.intellij.dbn.ddl;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.thread.Write;
import com.dci.intellij.dbn.common.ui.ListUtil;
import com.dci.intellij.dbn.common.util.DocumentUtil;
import com.dci.intellij.dbn.common.util.MessageUtil;
import com.dci.intellij.dbn.common.util.VirtualFileUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.dci.intellij.dbn.connection.mapping.FileConnectionMappingManager;
import com.dci.intellij.dbn.ddl.options.DDLFileSettings;
import com.dci.intellij.dbn.ddl.ui.AttachDDLFileDialog;
import com.dci.intellij.dbn.ddl.ui.DDLFileNameListCellRenderer;
import com.dci.intellij.dbn.ddl.ui.DetachDDLFileDialog;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.code.SourceCodeEditor;
import com.dci.intellij.dbn.editor.code.SourceCodeManagerAdapter;
import com.dci.intellij.dbn.editor.code.SourceCodeManagerListener;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.dci.intellij.dbn.options.ConfigId;
import com.dci.intellij.dbn.options.ProjectSettingsManager;
import com.dci.intellij.dbn.vfs.DatabaseFileSystem;
import com.dci.intellij.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dci.intellij.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.SelectFromListDialog;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dci.intellij.dbn.common.message.MessageCallback.conditional;
import static com.dci.intellij.dbn.common.util.MessageUtil.options;

@State(
    name = DDLFileAttachmentManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DDLFileAttachmentManager extends AbstractProjectComponent implements PersistentStateComponent<Element> {

    public static final String COMPONENT_NAME = "DBNavigator.Project.DDLFileAttachmentManager";

    private final Map<String, DBObjectRef<DBSchemaObject>> mappings = new HashMap<>();
    private DDLFileAttachmentManager(@NotNull Project project) {
        super(project);

        VirtualFileManager.getInstance().addVirtualFileListener(virtualFileListener);
        ProjectEvents.subscribe(project, this, SourceCodeManagerListener.TOPIC, sourceCodeManagerListener);
    }

    private final SourceCodeManagerListener sourceCodeManagerListener = new SourceCodeManagerAdapter() {
        @Override
        public void sourceCodeLoaded(@NotNull DBSourceCodeVirtualFile sourceCodeFile, boolean initialLoad) {
            if (!initialLoad && DatabaseFileSystem.isFileOpened(sourceCodeFile.getObject())) {
                updateDDLFiles(sourceCodeFile.getMainDatabaseFile());
            }
        }

        @Override
        public void sourceCodeSaved(@NotNull DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor) {
            updateDDLFiles(sourceCodeFile.getMainDatabaseFile());
        }
    };

    @Nullable
    public List<VirtualFile> getAttachedDDLFiles(DBObjectRef<DBSchemaObject> objectRef) {
        List<String> fileUrls = getAttachedFileUrls(objectRef);
        List<VirtualFile> virtualFiles = null;

        if (fileUrls.size() > 0) {
            VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
            for (String fileUrl : fileUrls) {
                VirtualFile virtualFile = virtualFileManager.findFileByUrl(fileUrl);
                if (virtualFile == null || !virtualFile.isValid()) {
                    mappings.remove(fileUrl);
                } else {
                    if (virtualFiles == null) virtualFiles = new ArrayList<>();
                    virtualFiles.add(virtualFile);
                }
            }
        }
        checkInvalidAttachedFiles(virtualFiles, objectRef);
        return virtualFiles;
    }

    @Nullable
    public DBSchemaObject getEditableObject(@NotNull VirtualFile ddlFile) {
        DBObjectRef<DBSchemaObject> objectRef = mappings.get(ddlFile.getUrl());
        return DBObjectRef.get(objectRef);
    }

    public ConnectionHandler getMappedConnection(VirtualFile ddlFile) {
        DBObjectRef<DBSchemaObject> objectRef = mappings.get(ddlFile.getUrl());
        if (objectRef != null) {
            ConnectionId connectionId = objectRef.getConnectionId();
            return ConnectionManager.getInstance(getProject()).getConnectionHandler(connectionId);
        }
        return null;
    }


    public boolean hasAttachedDDLFiles(DBObjectRef<DBSchemaObject> objectRef) {
        for (String filePath : mappings.keySet()) {
            DBObjectRef<DBSchemaObject> mappedObjectRef = mappings.get(filePath);
            if (mappedObjectRef.equals(objectRef)) return true;
        }
        return false;
    }


    private void checkInvalidAttachedFiles(List<VirtualFile> virtualFiles, DBObjectRef<DBSchemaObject> objectRef) {
        if (virtualFiles != null && virtualFiles.size() > 0) {
            List<VirtualFile> obsolete = null;
            for (VirtualFile virtualFile : virtualFiles) {
                if (!virtualFile.isValid() || !isValidDDLFile(virtualFile, objectRef)) {
                    if (obsolete == null) obsolete = new ArrayList<>();
                    obsolete.add(virtualFile);
                }
            }
            if (obsolete != null) {
                virtualFiles.removeAll(obsolete);
                for (VirtualFile virtualFile : obsolete) {
                    detachDDLFile(virtualFile);
                }
            }
        }
    }

    private boolean isValidDDLFile(VirtualFile virtualFile, DBObjectRef<DBSchemaObject> objectRef) {
        List<DDLFileType> ddlFileTypes = getDdlFileTypes(objectRef);
        for (DDLFileType ddlFileType : ddlFileTypes) {
            if (ddlFileType.getExtensions().contains(virtualFile.getExtension())) {
                return true;
            }
        }
        return false;
    }

    public int showFileAttachDialog(DBSchemaObject object, List<VirtualFile> virtualFiles, boolean showLookupOption) {
        AttachDDLFileDialog dialog = new AttachDDLFileDialog(virtualFiles, object, showLookupOption);
        dialog.show();
        return dialog.getExitCode();
    }

    public static int showFileDetachDialog(DBSchemaObject object, List<VirtualFile> virtualFiles) {
        DetachDDLFileDialog dialog = new DetachDDLFileDialog(virtualFiles, object);
        dialog.show();
        return dialog.getExitCode();
    }

    public void attachDDLFile(DBObjectRef<DBSchemaObject> objectRef, VirtualFile virtualFile) {
        if (objectRef != null) {
            mappings.put(virtualFile.getUrl(), objectRef);
            Project project = getProject();
            ProjectEvents.notify(project,
                    DDLFileAttachmentManagerListener.TOPIC,
                    (listener) -> listener.ddlFileAttached(project, virtualFile));
        }
    }

    public void detachDDLFile(VirtualFile virtualFile) {
        DBObjectRef<DBSchemaObject> objectRef = mappings.remove(virtualFile.getUrl());

        if (objectRef != null) {
            // map last used connection/schema
            FileConnectionMappingManager connectionMappingManager = FileConnectionMappingManager.getInstance(getProject());
            ConnectionHandler activeConnection = connectionMappingManager.getConnectionHandler(virtualFile);
            if (activeConnection == null) {
                DBSchemaObject schemaObject = objectRef.get();
                if (schemaObject != null) {
                    ConnectionHandler connectionHandler = schemaObject.getConnectionHandler();
                    connectionMappingManager.setConnectionHandler(virtualFile, connectionHandler);
                    connectionMappingManager.setDatabaseSchema(virtualFile, schemaObject.getSchemaIdentifier());
                }
            }
        }

        Project project = getProject();
        ProjectEvents.notify(project,
                DDLFileAttachmentManagerListener.TOPIC,
                (listener) -> listener.ddlFileDetached(project, virtualFile));
    }

    private List<VirtualFile> lookupApplicableDDLFiles(@NotNull DBObjectRef<DBSchemaObject> objectRef) {
        List<VirtualFile> fileList = new ArrayList<>();

        Project project = getProject();
        List<DDLFileType> ddlFileTypes = getDdlFileTypes(objectRef);
        for (DDLFileType ddlFileType : ddlFileTypes) {
            for (String extension : ddlFileType.getExtensions()) {
                String fileName = objectRef.getFileName().toLowerCase() + '.' + extension;
                VirtualFile[] files = VirtualFileUtil.lookupFilesForName(project, fileName);
                fileList.addAll(Arrays.asList(files));
            }
        }
        return fileList;
    }

    @NotNull
    private List<DDLFileType> getDdlFileTypes(@NotNull DBObjectRef<DBSchemaObject> objectRef) {
        DBObjectType objectType = objectRef.objectType;
        DDLFileManager ddlFileManager = DDLFileManager.getInstance(getProject());
        return ddlFileManager.getDDLFileTypes(objectType);
    }

    public List<VirtualFile> lookupDetachedDDLFiles(DBObjectRef<DBSchemaObject> object) {
        List<String> fileUrls = getAttachedFileUrls(object);
        List<VirtualFile> virtualFiles = lookupApplicableDDLFiles(object);
        List<VirtualFile> detachedVirtualFiles = new ArrayList<>();
        for (VirtualFile virtualFile : virtualFiles) {
            if (!fileUrls.contains(virtualFile.getUrl())) {
                detachedVirtualFiles.add(virtualFile);
            }
        }

        return detachedVirtualFiles;
    }

    public void createDDLFile(@NotNull DBObjectRef<DBSchemaObject> objectRef) {
        DDLFileNameProvider fileNameProvider = getDDLFileNameProvider(objectRef);
        Project project = getProject();

        if (fileNameProvider != null) {
            //ConnectionHandler connectionHandler = object.getCache();
            FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
            descriptor.setTitle("Select New DDL-File Location");

            VirtualFile[] selectedDirectories = FileChooser.chooseFiles(descriptor, project, null);
            if (selectedDirectories.length > 0) {
                String fileName = fileNameProvider.getFileName();
                VirtualFile parentDirectory = selectedDirectories[0];
                Write.run(() -> {
                    try {
                        DBSchemaObject object = objectRef.ensure();
                        VirtualFile virtualFile = parentDirectory.createChildData(this, fileName);
                        attachDDLFile(objectRef, virtualFile);
                        DBEditableObjectVirtualFile editableObjectFile = object.getEditableVirtualFile();
                        updateDDLFiles(editableObjectFile);
                        DatabaseFileSystem.getInstance().reopenEditor(object);
                    } catch (IOException e) {
                        MessageUtil.showErrorDialog(project, "Could not create file " + parentDirectory + File.separator + fileName + ".", e);
                    }
                });
            }
        } else {
            showMissingFileAssociations(objectRef);
        }
    }

    public void updateDDLFiles(DBEditableObjectVirtualFile databaseFile) {
        Project project = getProject();
        DDLFileSettings ddlFileSettings = DDLFileSettings.getInstance(project);
        if (ddlFileSettings.getGeneralSettings().isSynchronizeDDLFilesEnabled()) {
            DDLFileManager ddlFileManager = DDLFileManager.getInstance(project);
            List<VirtualFile> ddlFiles = databaseFile.getAttachedDDLFiles();
            if (ddlFiles != null && !ddlFiles.isEmpty()) {
                for (VirtualFile ddlFile : ddlFiles) {
                    DDLFileType ddlFileType = ddlFileManager.getDDLFileTypeForExtension(ddlFile.getExtension());
                    DBContentType fileContentType = ddlFileType.getContentType();

                    StringBuilder buffer = new StringBuilder();
                    if (fileContentType.isBundle()) {
                        DBContentType[] contentTypes = fileContentType.getSubContentTypes();
                        for (DBContentType contentType : contentTypes) {
                            DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) databaseFile.getContentFile(contentType);
                            if (sourceCodeFile != null) {
                                String statement = ddlFileManager.createDDLStatement(sourceCodeFile, contentType);
                                if (statement.trim().length() > 0) {
                                    buffer.append(statement);
                                    buffer.append('\n');
                                }
                                if (contentType != contentTypes[contentTypes.length - 1]) buffer.append('\n');
                            }
                        }
                    } else {
                        DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) databaseFile.getContentFile(fileContentType);
                        if (sourceCodeFile != null) {
                            buffer.append(ddlFileManager.createDDLStatement(sourceCodeFile, fileContentType));
                            buffer.append('\n');
                        }
                    }
                    Document document = DocumentUtil.getDocument(ddlFile);
                    if (document != null) {
                        DocumentUtil.setText(document, buffer);
                    }
                }
            }
        }
    }

    public void attachDDLFiles(DBObjectRef<DBSchemaObject> objectRef) {
        DDLFileNameProvider ddlFileNameProvider = getDDLFileNameProvider(objectRef);
        if (ddlFileNameProvider == null) {
            showMissingFileAssociations(objectRef);

        } else {
            List<VirtualFile> virtualFiles = lookupDetachedDDLFiles(objectRef);
            if (virtualFiles.size() == 0) {
                List<String> fileUrls = getAttachedFileUrls(objectRef);

                StringBuilder message = new StringBuilder();
                message.append(fileUrls.size() == 0 ?
                        "No DDL Files were found in " :
                        "No additional DDL Files were found in ");
                message.append("project scope.");

                if (fileUrls.size() > 0) {
                    message.append("\n\nFollowing files are already attached to ");
                    message.append(objectRef.getQualifiedNameWithType());
                    message.append(':');
                    for (String fileUrl : fileUrls) {
                        message.append('\n');
                        message.append(VirtualFileUtil.ensureFilePath(fileUrl));
                    }
                }

                String[] options = {"Create New...", "Cancel"};
                MessageUtil.showInfoDialog(getProject(),
                        "No DDL files found",
                        message.toString(), options, 0,
                        (option) -> conditional(option == 0,
                                () -> createDDLFile(objectRef)));
            } else {
                DBSchemaObject object = objectRef.ensure();
                int exitCode = showFileAttachDialog(object, virtualFiles, false);
                if (exitCode != DialogWrapper.CANCEL_EXIT_CODE) {
                    DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
                    databaseFileSystem.reopenEditor(object);
                }
            }
        }
    }

    public void showMissingFileAssociations(DBObjectRef<DBSchemaObject> objectRef) {
        MessageUtil.showWarningDialog(
                getProject(),
                "No DDL File Type Association",
                "No DDL file type is configured for database " + objectRef.getObjectType().getListName() +
                        ".\nPlease check the DDL file association in Project Settings > DB Navigator > DDL File Settings.",
                options("Open Settings...", "Cancel"), 0,
                (option) -> conditional(option == 0, () -> {
                    ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(getProject());
                    settingsManager.openProjectSettings(ConfigId.DDL_FILES);
                }));
    }

    public void detachDDLFiles(DBObjectRef<DBSchemaObject> objectRef) {
        List<VirtualFile> virtualFiles = getAttachedDDLFiles(objectRef);
        DBSchemaObject object = objectRef.ensure();
        int exitCode = showFileDetachDialog(object, virtualFiles);
        if (exitCode != DialogWrapper.CANCEL_EXIT_CODE) {
            DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
            databaseFileSystem.reopenEditor(object);
        }
    }

    @Nullable
    private DDLFileNameProvider getDDLFileNameProvider(DBObjectRef<DBSchemaObject> objectRef) {
        List<DDLFileType> ddlFileTypes = getDdlFileTypes(objectRef);
        if (ddlFileTypes.size() == 1 && ddlFileTypes.get(0).getExtensions().size() == 1) {
            DDLFileType ddlFileType = ddlFileTypes.get(0);
            return new DDLFileNameProvider(objectRef, ddlFileType, ddlFileType.getExtensions().get(0));
        } else if (ddlFileTypes.size() > 1) {
            List<DDLFileNameProvider> fileNameProviders = new ArrayList<>();
            for (DDLFileType ddlFileType : ddlFileTypes) {
                for (String extension : ddlFileType.getExtensions()) {
                    DDLFileNameProvider fileNameProvider = new DDLFileNameProvider(objectRef, ddlFileType, extension);
                    fileNameProviders.add(fileNameProvider);
                }
            }

            SelectFromListDialog fileTypeDialog = new SelectFromListDialog(
                    getProject(),
                    fileNameProviders.toArray(),
                    ListUtil.BASIC_TO_STRING_ASPECT,
                    "Select DDL file type",
                    ListSelectionModel.SINGLE_SELECTION);
            JList list = Failsafe.nd((JList) fileTypeDialog.getPreferredFocusedComponent());
            list.setCellRenderer(new DDLFileNameListCellRenderer());
            fileTypeDialog.show();
            Object[] selectedFileTypes = fileTypeDialog.getSelection();
            if (selectedFileTypes != null) {
                return (DDLFileNameProvider) selectedFileTypes[0];
            }
        }
        return null;
    }

    private List<String> getAttachedFileUrls(DBObjectRef<DBSchemaObject> objectRef) {
        List<String> fileUrls = new ArrayList<>();
        for (String fileUrl : mappings.keySet()) {
            DBObjectRef<DBSchemaObject> fileObjectRef = mappings.get(fileUrl);
            if (fileObjectRef.equals(objectRef)) {
                fileUrls.add(fileUrl);
            }
        }
        return fileUrls;
    }

    /***************************************
     *            ProjectComponent         *
     ***************************************/
    public static DDLFileAttachmentManager getInstance(@NotNull Project project) {
        return Failsafe.getComponent(project, DDLFileAttachmentManager.class);
    }

    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    /************************************************
     *               VirtualFileListener            *
     ************************************************/

    private VirtualFileListener virtualFileListener = new VirtualFileListener() {
        @Override
        public void fileDeleted(@NotNull VirtualFileEvent event) {
            DBObjectRef<DBSchemaObject> objectRef = mappings.get(event.getFile().getUrl());
            DBSchemaObject object = DBObjectRef.get(objectRef);
            if (object != null) {
                detachDDLFile(event.getFile());
                DatabaseFileSystem.getInstance().reopenEditor(object);
            }
        }
    };

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("state");
        for (String fileUrl : mappings.keySet()) {
            Element childElement = new Element("mapping");
            childElement.setAttribute("file-url", fileUrl);
            DBObjectRef<DBSchemaObject> objectRef = mappings.get(fileUrl);
            objectRef.writeState(childElement);
            element.addContent(childElement);
        }

        return element;
    }

    @Override
    public void loadState(@NotNull Element element) {
        VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
        for (Object child : element.getChildren()) {
            Element childElement = (Element) child;
            String fileUrl = childElement.getAttributeValue("file-url");
            if (StringUtil.isEmpty(fileUrl)) {
                // TODO backward compatibility. Do cleanup
                fileUrl = childElement.getAttributeValue("file");
            }

            if (StringUtil.isNotEmpty(fileUrl)) {
                fileUrl = VirtualFileUtil.ensureFileUrl(fileUrl);

                VirtualFile virtualFile = virtualFileManager.findFileByUrl(fileUrl);
                if (virtualFile != null) {
                    DBObjectRef<DBSchemaObject> objectRef = DBObjectRef.from(childElement);
                    if (objectRef != null) {
                        mappings.put(fileUrl, objectRef);
                    }
                }
            }
        }
    }
}
