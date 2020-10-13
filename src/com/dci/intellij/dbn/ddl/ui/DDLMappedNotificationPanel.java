package com.dci.intellij.dbn.ddl.ui;

import com.dci.intellij.dbn.common.editor.EditorNotificationPanel;
import com.dci.intellij.dbn.common.message.MessageType;
import com.dci.intellij.dbn.ddl.DDLFileAttachmentManager;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.vfs.DatabaseFileSystem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class DDLMappedNotificationPanel extends EditorNotificationPanel {

    public DDLMappedNotificationPanel(@NotNull final VirtualFile virtualFile, final DBSchemaObject editableObject) {
        super(MessageType.INFO);
        Project project = editableObject.getProject();
        DBObjectRef<DBSchemaObject> editableObjectRef = DBObjectRef.of(editableObject);
        String objectName = editableObject.getQualifiedNameWithType();
        String objectTypeName = editableObject.getObjectType().getName();
        setText("This DDL file is attached to the database " + objectName + ". " +
                "Changes done to the " + objectTypeName + " are mirrored to this DDL file, overwriting any changes you may do to it.");
        createActionLabel("Detach", () -> {
            if (!project.isDisposed()) {
                DDLFileAttachmentManager attachmentManager = DDLFileAttachmentManager.getInstance(project);
                attachmentManager.detachDDLFile(virtualFile);
                DBSchemaObject editableObject1 = DBObjectRef.get(editableObjectRef);
                if (editableObject1 != null) {
                    DatabaseFileSystem.getInstance().reopenEditor(editableObject1);
                }
            }
        });
    }
}
