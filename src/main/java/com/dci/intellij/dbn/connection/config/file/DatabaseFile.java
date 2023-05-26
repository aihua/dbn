package com.dci.intellij.dbn.connection.config.file;

import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.util.Cloneable;
import com.dci.intellij.dbn.common.util.Files;
import com.dci.intellij.dbn.common.util.Strings;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

@Data
public class DatabaseFile implements Cloneable<DatabaseFile> {
    private String path;
    private String schema;
    private Latent<File> file = Latent.mutable(
            () -> getPath(),
            () -> Strings.isEmpty(path) ? null : new File(path));

    public DatabaseFile() {}

    public DatabaseFile(String path) {
        this(path, Files.getFileName(path));
    }

    public DatabaseFile(String path, String schema) {
        this.path = path;
        this.schema = schema;
    }

    public String getFileName() {
        return Files.getFileName(path);
    }

    @Nullable
    public File getFile() {
        return file.get();
    }

    public boolean isValid() {
        File file = getFile();
        if (file == null) return false;
        if (file.isDirectory()) return false;

        return true;
    }

    public boolean isMain() {
        return Objects.equals(schema, "main");
    }

    public boolean isPresent() {
        File file = getFile();
        return isValid() && file != null  && file.exists();
    }

    @Override
    public DatabaseFile clone() {
        return new DatabaseFile(path, schema);
    }

}
