package com.dci.intellij.dbn.vfs;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.ui.Presentable;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFilePathWrapper;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public abstract class DBVirtualFileImpl extends VirtualFile implements DBVirtualFile, Presentable, VirtualFilePathWrapper {
    private static final AtomicInteger ID_STORE = new AtomicInteger(1000);
    private final int id;
    private final ProjectRef projectRef;
    private final WeakRef<DatabaseFileSystem> fileSystem;

    protected String name;
    protected String path;
    protected String url;
    private int documentSignature;
    private boolean valid = true;

    public DBVirtualFileImpl(@NotNull Project project) {
        id = ID_STORE.incrementAndGet();
        //id = DummyFileIdGenerator.next();
        projectRef = ProjectRef.of(project);
        fileSystem = WeakRef.of(DatabaseFileSystem.getInstance());
    }



    @NotNull
    @Override
    public EnvironmentType getEnvironmentType() {
        return getConnectionHandler().getEnvironmentType();
    }

    @NotNull
    public ConnectionId getConnectionId() {
        return getConnectionHandler().getConnectionId();
    }

    @Override
    public boolean isInLocalFileSystem() {
        return false;
    }

    @NotNull
    @Override
    public DatabaseFileSystem getFileSystem() {
        return Failsafe.nn(WeakRef.get(fileSystem));
    }

    @Override
    @NotNull
    public Project getProject() {
        return projectRef.ensure();
    }

    @Override
    public abstract Icon getIcon();

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @NotNull
    @Override
    public final synchronized String getPath() {
        if (path == null) {
            path = createPath();
        }
        return path;
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
    public final synchronized String getUrl() {
        if (url == null) {
            url = createUrl();
        }
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

    @NotNull
    private String createPath() {
        return DatabaseFileSystem.createPath(this);
    }
    @NotNull
    private String createUrl() {
        return DatabaseFileSystem.createUrl(this);
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
        if (valid) {
            valid = false;
            DatabaseFileViewProvider cachedViewProvider = getCachedViewProvider();
            if (cachedViewProvider != null) {
                cachedViewProvider.markInvalidated();
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

}
