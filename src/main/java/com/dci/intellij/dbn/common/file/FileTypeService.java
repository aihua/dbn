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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.dci.intellij.dbn.common.component.Components.applicationService;
import static com.dci.intellij.dbn.common.file.FileTypeService.COMPONENT_NAME;
import static com.dci.intellij.dbn.common.options.setting.Settings.newElement;
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

    private final Map<String, String> originalFileAssociations = new ConcurrentHashMap<>();
    private boolean silentFileTypeChange = false;

    private FileTypeService() {
        super(COMPONENT_NAME);
        ApplicationEvents.subscribe(this, FileTypeManager.TOPIC, snapshotFileTypeListener());
        ApplicationEvents.subscribe(this, FileTypeManager.TOPIC, toolbarsFileTypeListener());
    }

    public static FileTypeService getInstance() {
        return applicationService(FileTypeService.class);
    }

    private void withSilentContext(Runnable runnable) {
        boolean silent = silentFileTypeChange;
        try {
            silentFileTypeChange = true;
            runnable.run();
        } finally {
            silentFileTypeChange = silent;
        }
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
    private FileTypeListener snapshotFileTypeListener() {
        return new FileTypeListener() {
            @Override
            public void beforeFileTypesChanged(@NotNull FileTypeEvent event) {
                captureFileAssociations(SQLFileType.INSTANCE);
                captureFileAssociations(PSQLFileType.INSTANCE);
            }
        };
    }


    private FileTypeListener toolbarsFileTypeListener() {
        return new FileTypeListener() {
            @Override
            public void fileTypesChanged(@NotNull FileTypeEvent event) {
                // TODO plugin conflict resolution - show / hide toolbars
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
        withSilentContext(() -> {
            String[] extensions = fileType.getSupportedExtensions();
            for (String extension : extensions) {
                associateExtension(fileType, extension);
            }
        });
    }

    public void restoreFileAssociations() {
        withSilentContext(() -> {
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
        });
    }

    private void associate(FileType fileType, String extension) {
        FileType currentFileType = getCurrentFileType(extension);
        if (currentFileType == fileType) return;

        Write.run(() -> withSilentContext(() -> FileTypeManager.getInstance().associateExtension(fileType, extension)));
    }

    private void dissociate(FileType fileType, String fileExtension) {
        Write.run(() -> withSilentContext(() -> FileTypeManager.getInstance().removeAssociatedExtension(fileType, fileExtension)));
    }

    @NotNull
    public FileType getCurrentFileType(String extension) {
        return Unsafe.silent(UnknownFileType.INSTANCE, extension, e -> FileTypeManager.getInstance().getFileTypeByExtension(e));
    }

    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element mappingsElement = newElement(element, "original-file-types");

        for (String fileExtension : originalFileAssociations.keySet()) {
            String fileType = originalFileAssociations.get(fileExtension);
            Element mappingElement = newElement(mappingsElement, "mapping");
            mappingElement.setAttribute("file-extension", fileExtension);
            mappingElement.setAttribute("file-type", fileType);
        }
        return element;
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
