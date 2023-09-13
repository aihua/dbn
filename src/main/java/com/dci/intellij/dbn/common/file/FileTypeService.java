package com.dci.intellij.dbn.common.file;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.component.ApplicationComponentBase;
import com.dci.intellij.dbn.common.component.PersistentState;
import com.dci.intellij.dbn.common.event.ApplicationEvents;
import com.dci.intellij.dbn.common.thread.Write;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.common.util.Unsafe;
import com.dci.intellij.dbn.language.common.DBLanguageFileType;
import com.dci.intellij.dbn.language.psql.PSQLFileType;
import com.dci.intellij.dbn.language.sql.SQLFileType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.dci.intellij.dbn.common.component.Components.applicationService;
import static com.dci.intellij.dbn.common.file.FileTypeService.COMPONENT_NAME;
import static com.dci.intellij.dbn.common.options.setting.Settings.stringAttribute;

@Slf4j
@Getter
@Setter
@State(
    name = COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class FileTypeService extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.FileTypeService";

    private final Map<String, String> originalFileAssociations = new HashMap<>();
    private boolean silentFileChangeContext = false;

    private FileTypeService() {
        super(COMPONENT_NAME);
        ApplicationEvents.subscribe(this, FileTypeManager.TOPIC, createFileTypeListener());
    }

    public static FileTypeService getInstance() {
        return applicationService(FileTypeService.class);
    }

    public final void associateExtension(@NotNull DBLanguageFileType fileType, @NotNull String extension) {
        try {
            FileType currentFileType = getCurrentFileType(extension);
            if (currentFileType == fileType) return;

            if (!Commons.isOneOf(currentFileType,
                    UnknownFileType.INSTANCE,
                    SQLFileType.INSTANCE,
                    PSQLFileType.INSTANCE)) {

                originalFileAssociations.put(extension, currentFileType.getName());
            }

            dissociate(currentFileType, extension);
            associate(fileType, extension);
        } catch (Throwable e) {
            log.error("Failed to associate file type {} for extension {}", fileType, extension, e);
        }
    }

    @NotNull
    private FileTypeListener createFileTypeListener() {
        return new FileTypeListener() {
            @Override
            public void beforeFileTypesChanged(@NotNull FileTypeEvent event) {
                captureFileAssociations(SQLFileType.INSTANCE);
                captureFileAssociations(PSQLFileType.INSTANCE);
            }
        };
    }

    private void captureFileAssociations(DBLanguageFileType fileType) {
        String[] extensions = fileType.getSupportedExtensions();
        for (String extension : extensions) {
            FileType currentFileType = getCurrentFileType(extension);
            if (Commons.isOneOf(currentFileType,
                    UnknownFileType.INSTANCE,
                    SQLFileType.INSTANCE,
                    PSQLFileType.INSTANCE)) continue;

            originalFileAssociations.put(extension, currentFileType.getName());
        }
    }

    public void claimFileAssociations(DBLanguageFileType fileType) {
        String[] extensions = fileType.getSupportedExtensions();
        for (String extension : extensions) {
            associateExtension(fileType, extension);
        }
    }

    public void restoreFileAssociations() {
        for (String fileExtension : originalFileAssociations.keySet()) {
            String fileTypeName = originalFileAssociations.get(fileExtension);
            FileType fileType = getFileType(fileTypeName);
            if (fileType == null) continue;

            associate(fileType, fileExtension);
        }

        FileType originalSqlFileType = getFileType("SQL");
        if (originalSqlFileType != null) {
            if (getCurrentFileType("sql") instanceof DBLanguageFileType) associate(originalSqlFileType, "sql");
            if (getCurrentFileType("ddl") instanceof DBLanguageFileType) associate(originalSqlFileType, "ddl");
        }
    }

    private static void associate(FileType fileType, String extension) {
        FileType currentFileType = getCurrentFileType(extension);
        if (currentFileType == fileType) return;

        Write.run(() -> FileTypeManager.getInstance().associateExtension(fileType, extension));
    }

    private static void dissociate(FileType fileType, String fileExtension) {
        Write.run(() -> FileTypeManager.getInstance().removeAssociatedExtension(fileType, fileExtension));
    }

    @NotNull
    private static FileType getCurrentFileType(String extension) {
        return Unsafe.silent(UnknownFileType.INSTANCE, extension, e -> FileTypeManager.getInstance().getFileTypeByExtension(e));
    }

    @Override
    public Element getComponentState() {
        Element stateElement = new Element("state");
        Element mappingsElement = new Element("original-file-types");
        stateElement.addContent(mappingsElement);

        for (String fileExtension : originalFileAssociations.keySet()) {
            String fileType = originalFileAssociations.get(fileExtension);
            Element mappingElement = new Element("mapping");
            mappingElement.setAttribute("file-extension", fileExtension);
            mappingElement.setAttribute("file-type", fileType);
            mappingsElement.addContent(mappingElement);
        }
        return stateElement;
    }

    @Override
    public void loadComponentState(@NotNull Element stateElement) {
        Element mappingsElement = stateElement.getChild("original-file-types");
        if (mappingsElement == null) return;

        for (Element mappingElement : mappingsElement.getChildren()) {
            String fileExtension = stringAttribute(mappingElement, "file-extension");
            String fileType = stringAttribute(mappingElement, "file-type");
            originalFileAssociations.put(fileExtension, fileType);
        }
    }

    @Nullable
    private static FileType getFileType(String fileTypeName) {
        FileType[] registeredFileTypes = FileTypeManager.getInstance().getRegisteredFileTypes();
        return Arrays
                .stream(registeredFileTypes)
                .filter(ft -> Objects.equals(ft.getName(), fileTypeName))
                .findFirst()
                .orElse(null);
    }

    public List<FileNameMatcher> getAssociations(DBLanguageFileType fileType) {
        return FileTypeManager.getInstance().getAssociations(fileType);
    }
}
