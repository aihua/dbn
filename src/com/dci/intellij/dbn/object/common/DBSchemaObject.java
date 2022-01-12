package com.dci.intellij.dbn.object.common;

import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.language.common.DBLanguage;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.status.DBObjectStatusHolder;
import com.dci.intellij.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dci.intellij.dbn.vfs.file.DBObjectVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

public interface DBSchemaObject extends DBObject {
    List<DBObject> getReferencedObjects();
    List<DBObject> getReferencingObjects();
    boolean isEditable(DBContentType contentType);

    DBLanguage getCodeLanguage(DBContentType contentType);

    String getCodeParseRootId(DBContentType contentType);

    void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException;

    DBObjectStatusHolder getStatus();

    @Override
    @NotNull
    DBObjectVirtualFile<?> getVirtualFile();

    DBEditableObjectVirtualFile getEditableVirtualFile();

    @Nullable
    DBEditableObjectVirtualFile getCachedVirtualFile();

    List<DBSchema> getReferencingSchemas() throws SQLException;
}
