package com.dci.intellij.dbn.ddl.action;

import com.dci.intellij.dbn.common.util.ActionUtil;
import com.dci.intellij.dbn.ddl.DDLFileAttachmentManager;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class AttachDDLFileAction extends AnAction {
    private DBObjectRef<DBSchemaObject> objectRef;

    public AttachDDLFileAction(@NotNull DBSchemaObject object) {
        super("Attach files");
        this.objectRef = DBObjectRef.from(object);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = ActionUtil.ensureProject(e);
        DDLFileAttachmentManager fileAttachmentManager = DDLFileAttachmentManager.getInstance(project);
        fileAttachmentManager.attachDDLFiles(objectRef);
    }
}
