package com.dci.intellij.dbn.common.file.util;

import com.dci.intellij.dbn.common.event.ApplicationEvents;
import com.dci.intellij.dbn.common.thread.Write;
import com.dci.intellij.dbn.vfs.DBVirtualFileBase;
import com.dci.intellij.dbn.vfs.DatabaseFileSystem;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@UtilityClass
public final class VirtualFiles {

    public static Icon getIcon(VirtualFile virtualFile) {
        if (virtualFile instanceof DBVirtualFileBase) {
            DBVirtualFileBase file = (DBVirtualFileBase) virtualFile;
            return file.getIcon();
        }
        return virtualFile.getFileType().getIcon();
    }

    public static boolean isDatabaseFileSystem(@NotNull VirtualFile file) {
        return file.getFileSystem() instanceof DatabaseFileSystem;
    }

    public static boolean isLocalFileSystem(@NotNull VirtualFile file) {
        return file.isInLocalFileSystem();
    }

    public static boolean isVirtualFileSystem(@NotNull VirtualFile file) {
        return !isDatabaseFileSystem(file) && !isLocalFileSystem(file);
    }

    public static VirtualFile ioFileToVirtualFile(File file) {
        return LocalFileSystem.getInstance().findFileByIoFile(file);
    }

    public static void setReadOnlyAttribute(VirtualFile file, boolean readonly) {
        try {
            ReadOnlyAttributeUtil.setReadOnlyAttribute(file, readonly);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setReadOnlyAttribute(String path, boolean readonly) {
        try {
            ReadOnlyAttributeUtil.setReadOnlyAttribute(path, readonly);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static VirtualFile[] findFiles(Project project, FileSearchRequest request) {
        FileCollector collector = FileCollector.create(request);
        ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
        VirtualFile[] contentRoots = projectRootManager.getContentRoots();
        for (VirtualFile contentRoot : contentRoots) {
            VfsUtilCore.visitChildrenRecursively(contentRoot, collector);
        }
        return collector.files();
    }

    public static String ensureFilePath(String fileUrlOrPath) {
        if (fileUrlOrPath != null && fileUrlOrPath.startsWith(StandardFileSystems.FILE_PROTOCOL_PREFIX)) {
            return fileUrlOrPath.substring(StandardFileSystems.FILE_PROTOCOL_PREFIX.length());
        }
        return fileUrlOrPath;
    }

    public static String ensureFileUrl(String fileUrlOrPath) {
        if (fileUrlOrPath != null && !fileUrlOrPath.startsWith(StandardFileSystems.FILE_PROTOCOL_PREFIX)) {
            return StandardFileSystems.FILE_PROTOCOL_PREFIX + fileUrlOrPath;
        }
        return fileUrlOrPath;
    }

    @Nullable
    public static VirtualFile getOriginalFile(VirtualFile file) {
        if (file instanceof LightVirtualFile) {
            LightVirtualFile lightVirtualFile = (LightVirtualFile) file;
            VirtualFile originalFile = lightVirtualFile.getOriginalFile();
            if (originalFile != null && originalFile != file) {
                return getOriginalFile(originalFile);
            }
        }
        return file;
    }

    @Contract("null -> null; !null-> !null")
    public static VirtualFile getUnderlyingFile(VirtualFile file) {
        file = getOriginalFile(file);

        if (file instanceof VirtualFileWindow) {
            VirtualFileWindow fileWindow = (VirtualFileWindow) file;
            return fileWindow.getDelegate();
        }

        if (file instanceof LightVirtualFile) {
            LightVirtualFile lightVirtualFile = (LightVirtualFile) file;
            // TODO is this ever the case?
        }
        return file;

    }

    public static VFileEvent createFileRenameEvent(
            @NotNull VirtualFile virtualFile,
            @NotNull String oldName,
            @NotNull String newName) {
        return new VFilePropertyChangeEvent(null, virtualFile, VirtualFile.PROP_NAME, oldName, newName, false);
    }

    public static VFileEvent createFileDeleteEvent(@NotNull VirtualFile virtualFile) {
        return new VFileDeleteEvent(null, virtualFile, false);
    }

    public static void notifiedFileChange(VFileEvent event, Runnable changeAction) {
        BulkFileListener publisher = ApplicationEvents.publisher(VirtualFileManager.VFS_CHANGES);
        List<VFileEvent> events = Collections.singletonList(event);
        Write.run(() -> {
            publisher.before(events);
            changeAction.run();
            publisher.after(events);
        });

    }


}

