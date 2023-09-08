package com.dci.intellij.dbn.ddl;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.component.PersistentState;
import com.dci.intellij.dbn.common.component.ProjectComponentBase;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.file.FileTypeService;
import com.dci.intellij.dbn.common.notification.NotificationGroup;
import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dci.intellij.dbn.ddl.options.DDLFileExtensionSettings;
import com.dci.intellij.dbn.ddl.options.DDLFileSettings;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.language.common.DBLanguageFileType;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.dci.intellij.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.dci.intellij.dbn.common.component.Components.projectService;

@State(
    name = DDLFileManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DDLFileManager extends ProjectComponentBase implements PersistentState {

    public static final String COMPONENT_NAME = "DBNavigator.Project.DDLFileManager";

    private DDLFileManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, FileTypeManager.TOPIC, fileTypeListener);
    }

    public void registerExtensions(DDLFileExtensionSettings settings) {
        FileTypeService fileTypeService = FileTypeService.getInstance();
        List<DDLFileType> fileTypes = settings.getFileTypes();
        fileTypes.forEach(ft -> ft.getExtensions().forEach(e -> fileTypeService.associateExtension(ft.getLanguageFileType(), e)));;
    }

    public static DDLFileManager getInstance(@NotNull Project project) {
        return projectService(project, DDLFileManager.class);
    }

    private DDLFileExtensionSettings getExtensionSettings() {
        return DDLFileSettings.getInstance(getProject()).getExtensionSettings();
    }

    public DDLFileType getDDLFileType(DDLFileTypeId ddlFileTypeId) {
        return getExtensionSettings().getFileType(ddlFileTypeId);
    }

    DDLFileType getDDLFileTypeForExtension(String extension) {
        return getExtensionSettings().getFileTypeForExtension(extension);
    }

    String createDDLStatement(DBSourceCodeVirtualFile sourceCodeFile, DBContentType contentType) {
        DBSchemaObject object = sourceCodeFile.getObject();
        String content = sourceCodeFile.getOriginalContent().toString().trim();
        if (content.isEmpty()) return "";

        ConnectionHandler connection = object.getConnection();
        String alternativeStatementDelimiter = connection.getSettings().getDetailSettings().getAlternativeStatementDelimiter();
        DatabaseDataDefinitionInterface dataDefinition = connection.getDataDefinitionInterface();
        return dataDefinition.createDDLStatement(getProject(),
                object.getObjectType().getTypeId(),
                connection.getUserName(),
                object.getSchema().getName(),
                object.getName(),
                contentType,
                content,
                alternativeStatementDelimiter);
    }


    @Nullable
    public DDLFileType getDDLFileType(DBObjectType objectType, DBContentType contentType) {
        DDLFileTypeId ddlFileTypeId = objectType.getDdlFileTypeId(contentType);
        return ddlFileTypeId == null ? null : getDDLFileType(ddlFileTypeId);
    }

    @NotNull
    List<DDLFileType> getDDLFileTypes(DBObjectType objectType) {
        Collection<DDLFileTypeId> typeIds = objectType.getDdlFileTypeIds();
        if (typeIds == null) return Collections.emptyList();

        return typeIds.stream().map(id -> getDDLFileType(id)).collect(Collectors.toList());
    }


    /***************************************
     *            FileTypeListener         *
     ***************************************/

    private final FileTypeListener fileTypeListener = new FileTypeListener() {
        @Override
        public void fileTypesChanged(@NotNull FileTypeEvent event) {
            FileTypeService fileTypeService = FileTypeService.getInstance();
            List<DDLFileType> ddlFileTypeList = getExtensionSettings().getFileTypes();
            for (DDLFileType ddlFileType : ddlFileTypeList) {
                DBLanguageFileType fileType = ddlFileType.getLanguageFileType();
                List<FileNameMatcher> associations = fileTypeService.getAssociations(fileType);
                List<String> registeredExtension = new ArrayList<>();
                for (FileNameMatcher association : associations) {
                    if (association instanceof ExtensionFileNameMatcher) {
                        ExtensionFileNameMatcher extensionMatcher = (ExtensionFileNameMatcher) association;
                        registeredExtension.add(extensionMatcher.getExtension());
                    }
                }

                StringBuilder restoredAssociations = new StringBuilder();
                for (String extension : ddlFileType.getExtensions()) {
                    if (!registeredExtension.contains(extension)) {
                        fileTypeService.associateExtension(fileType, extension);
                        if (restoredAssociations.length() > 0) {
                            restoredAssociations.append(", ");
                        }
                        restoredAssociations.append(extension);

                    }
                }

                if (restoredAssociations.length() > 0) {
                    sendInfoNotification(
                            NotificationGroup.DDL,
                            "Following file associations have been restored: \"" + restoredAssociations + "\". " +
                                    "They are registered as DDL file types in project \"" + getProject().getName() + "\".\n" +
                                    "Please remove them from project DDL configuration first (Project Settings > DB Navigator > DDL File Settings).");
                }
            }

        }
    };

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
        if (!getProject().isDefault()) initializeComponent();
    }

    //@Override
    public void initializeComponent() {
        Background.run(getProject(), () -> registerExtensions(getExtensionSettings()));
    }
}
