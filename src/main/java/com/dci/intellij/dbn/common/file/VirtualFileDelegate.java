package com.dci.intellij.dbn.common.file;

import com.dci.intellij.dbn.common.ref.WeakRef;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class VirtualFileDelegate extends VirtualFile{
    private final WeakRef<VirtualFile> inner;
    private final FileType fileType;

    public VirtualFileDelegate(VirtualFile virtualFile, FileType fileType) {
        this.inner = WeakRef.of(virtualFile);
        this.fileType = fileType;
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return fileType;
    }

    //@Delegate
    public VirtualFile inner() {
        return WeakRef.ensure(inner);
    }

    @NotNull
    @Override
    public String getName() {
        return inner().getName();
    }

    @NotNull
    @Override
    public VirtualFileSystem getFileSystem() {
        return inner().getFileSystem();
    }

    @NotNull
    @Override
    public String getPath() {
        return inner().getPath();
    }

    @Override
    public boolean isWritable() {
        return inner().isWritable();
    }

    @Override
    public boolean isDirectory() {
        return inner().isDirectory();
    }

    @Override
    public boolean isValid() {
        return inner().isValid();
    }

    @Override
    public VirtualFile getParent() {
        return inner().getParent();
    }

    @Override
    public VirtualFile[] getChildren() {
        return inner().getChildren();
    }

    @Override
    public @NotNull OutputStream getOutputStream(Object o, long l, long l1) throws IOException {
        return inner().getOutputStream(o, l, l1);
    }

    @NotNull
    @Override
    public byte[] contentsToByteArray() throws IOException {
        return inner().contentsToByteArray();
    }

    @Override
    public long getTimeStamp() {
        return inner().getTimeStamp();
    }

    @Override
    public long getLength() {
        return inner().getLength();
    }

    @Override
    public void refresh(boolean b, boolean b1, @Nullable Runnable runnable) {
        inner().refresh(b, b1, runnable);
    }

    @Override
    public @NotNull InputStream getInputStream() throws IOException {
        return inner().getInputStream();
    }

    @Override
    public long getModificationStamp() {
        return inner().getModificationStamp();
    }

    @Override
    public long getModificationCount() {
        return inner().getModificationCount();
    }
}
