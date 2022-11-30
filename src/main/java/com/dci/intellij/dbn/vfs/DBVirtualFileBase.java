package com.dci.intellij.dbn.vfs;

import com.dci.intellij.dbn.common.DevNullStreams;
import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.ui.Presentable;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.dci.intellij.dbn.language.sql.SQLFileType;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFilePathWrapper;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.util.LocalTimeCounter;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public abstract class DBVirtualFileBase extends VirtualFile implements DBVirtualFile, Presentable, VirtualFilePathWrapper {
    private static final AtomicInteger ID_STORE = new AtomicInteger(1000);
    private final int id;
    private final ProjectRef project;
    private final WeakRef<DatabaseFileSystem> fileSystem;

    protected String path;
    protected String url;
    private volatile boolean valid = true;
    private volatile int documentSignature;

    private long modificationStamp = LocalTimeCounter.currentTime();
    private long timeStamp = System.currentTimeMillis();

    private String name;

    public DBVirtualFileBase(@NotNull Project project, @NotNull String name) {
        this.id = ID_STORE.incrementAndGet();
        this.name = name;
        //id = DummyFileIdGenerator.next();
        this.project = ProjectRef.of(project);
        this.fileSystem = WeakRef.of(DatabaseFileSystem.getInstance());
    }

    public long getModificationStamp() {
        return modificationStamp;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    @NotNull
    @Override
    public EnvironmentType getEnvironmentType() {
        return getConnection().getEnvironmentType();
    }

    @Override
    public boolean isInLocalFileSystem() {
        return false;
    }

    @NotNull
    @Override
    public DatabaseFileSystem getFileSystem() {
        return fileSystem.ensure();
    }

    @Override
    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @Override
    public abstract Icon getIcon();

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public VirtualFile[] getChildren() {
        return VirtualFile.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public InputStream getInputStream() throws IOException {
        return DevNullStreams.INPUT_STREAM;
    }

    @Override
    @NotNull
    public OutputStream getOutputStream(Object requestor, long modificationStamp, long timeStamp) throws IOException {
        return DevNullStreams.OUTPUT_STREAM;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public VirtualFile getParent() {
        return null;
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return SQLFileType.INSTANCE;
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return getName();
    }

    @NotNull
    @Override
    public String getPresentablePath() {
        return getFileSystem().extractPresentablePath(getPath());
    }

    @Override
    public boolean enforcePresentableName() {
        return false;
    }

    @NotNull
    @Override
    public final String getPath() {
        if (path == null)
            path = DatabaseFileSystem.createFilePath(this);
        return path;
    }

    @NotNull
    @Override
    public final String getUrl() {
        if (url == null)
            url = DatabaseFileSystem.createFileUrl(this);
        return url;
    }

    @Override
    public void rename(Object requestor, @NotNull String newName) throws IOException {
        throw DatabaseFileSystem.READONLY_FILE_SYSTEM;
    }

    @Override
    public void move(Object requestor, @NotNull VirtualFile newParent) throws IOException {
        throw DatabaseFileSystem.READONLY_FILE_SYSTEM;
    }

    @NotNull
    @Override
    public VirtualFile copy(Object requestor, @NotNull VirtualFile newParent, @NotNull String copyName) throws IOException {
        throw DatabaseFileSystem.READONLY_FILE_SYSTEM;
    }

    @Override
    public void delete(Object requestor) throws IOException {
        throw DatabaseFileSystem.READONLY_FILE_SYSTEM;
    }

    @Override
    public void setCachedViewProvider(@Nullable DatabaseFileViewProvider viewProvider) {
        putUserData(DatabaseFileViewProvider.CACHED_VIEW_PROVIDER, viewProvider);
    }

    @Override
    @Nullable
    public DatabaseFileViewProvider getCachedViewProvider() {
        return getUserData(DatabaseFileViewProvider.CACHED_VIEW_PROVIDER);
    }

    public void invalidate() {
        if (!valid) return;

        valid = false;
        DatabaseFileViewProvider cachedViewProvider = getCachedViewProvider();
        if (cachedViewProvider != null) {
            DebugUtil.performPsiModification("disposing database view provider", () -> cachedViewProvider.markInvalidated());
            List<PsiFile> cachedPsiFiles = cachedViewProvider.getCachedPsiFiles();
            for (PsiFile cachedPsiFile: cachedPsiFiles) {
                if (cachedPsiFile instanceof DBLanguagePsiFile) {
                    DBLanguagePsiFile languagePsiFile = (DBLanguagePsiFile) cachedPsiFile;
                    Disposer.dispose(languagePsiFile);
                }
            }

            setCachedViewProvider(null);
        }
        putUserData(FileDocumentManagerImpl.HARD_REF_TO_DOCUMENT_KEY, null);
    }

}
