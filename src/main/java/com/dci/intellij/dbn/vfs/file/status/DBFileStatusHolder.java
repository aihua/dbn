package com.dci.intellij.dbn.vfs.file.status;

import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.property.PropertyHolderBase;
import com.dci.intellij.dbn.common.ref.WeakRef;
import com.dci.intellij.dbn.vfs.file.DBContentVirtualFile;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dci.intellij.dbn.common.dispose.Failsafe.guarded;

public class DBFileStatusHolder extends PropertyHolderBase.IntStore<DBFileStatus> {
    private final WeakRef<DBContentVirtualFile> file;

    public DBFileStatusHolder(DBContentVirtualFile file) {
        super();
        this.file = WeakRef.of(file);
    }

    @NotNull
    public DBContentVirtualFile getFile() {
        return file.ensure();
    }

    @NotNull
    private Project getProject() {
        return getFile().getProject();
    }

    @Override
    public boolean set(DBFileStatus property, boolean value) {
        return super.set(property, value);
    }

    @Override
    protected DBFileStatus[] properties() {
        return DBFileStatus.VALUES;
    }

    @Override
    protected void changed(DBFileStatus property, boolean value) {
        if (file == null) return; // not initialised yet
        guarded(() -> {
            ProjectEvents.notify(getProject(),
                    DBFileStatusListener.TOPIC,
                    (listener) -> listener.statusChanged(getFile(), property, value));;
        });
    }
}
