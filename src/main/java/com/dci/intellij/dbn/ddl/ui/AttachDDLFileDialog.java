package com.dci.intellij.dbn.ddl.ui;

import com.dci.intellij.dbn.common.file.VirtualFileInfo;
import com.dci.intellij.dbn.common.text.TextContent;
import com.dci.intellij.dbn.common.ui.dialog.DBNDialog;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.ddl.DDLFileAttachmentManager;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.dci.intellij.dbn.common.text.TextContent.plain;

public class AttachDDLFileDialog extends DBNDialog<SelectDDLFileForm> {
    private final DBObjectRef<DBSchemaObject> object;
    private final boolean showLookupOption;
    private final List<VirtualFileInfo> fileInfos;

    public AttachDDLFileDialog(List<VirtualFileInfo> fileInfos, @NotNull DBSchemaObject object, boolean showLookupOption) {
        super(object.getProject(), "Attach DDL file", true);
        this.fileInfos = fileInfos;
        this.object = DBObjectRef.of(object);
        this.showLookupOption = showLookupOption;
        renameAction(getOKAction(), "Attach selected");
        setDefaultSize(700, 400);
        init();
    }

    @NotNull
    @Override
    protected SelectDDLFileForm createForm() {
        DBSchemaObject object = getObject();
        String typeName = object.getTypeName();
        TextContent hintText = plain(
                "Following DDL files were found matching the name of the " + object.getQualifiedNameWithType() + ".\n" +
                        "NOTE: Attached DDL files will become readonly and their content will change automatically when the " + typeName + " is edited.\n\n" +
                        "Select the files to attach to this " + typeName + ".");
        return new SelectDDLFileForm(this, object, fileInfos, hintText, showLookupOption);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                new SelectAllAction(),
                new SelectNoneAction(),
                getCancelAction()
        };
    }

    @NotNull
    public DBSchemaObject getObject() {
        return DBObjectRef.ensure(object);
    }

    private class SelectAllAction extends AbstractAction {
        private SelectAllAction() {
            super("Attach all");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getForm().selectAll();
            doOKAction();
        }
    }

    private class SelectNoneAction extends AbstractAction {
        private SelectNoneAction() {
            super("Attach none");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SelectDDLFileForm component = getForm();
            component.selectNone();
            if (showLookupOption && component.isDoNotPromptSelected()) {
                ConnectionHandler connection = getObject().getConnection();
                connection.getSettings().getDetailSettings().setEnableDdlFileBinding(false);
            }
            close(2);
        }
    }

    @Override
    protected void doOKAction() {
        SelectDDLFileForm component = getForm();
        DBSchemaObject object = getObject();
        Project project = object.getProject();
        DDLFileAttachmentManager fileAttachmentManager = DDLFileAttachmentManager.getInstance(project);
        List<VirtualFileInfo> fileInfos = component.getSelection();
        for (VirtualFileInfo fileInfo : fileInfos) {
            fileAttachmentManager.attachDDLFile(object.ref(), fileInfo.getFile());
        }
        if (showLookupOption && component.isDoNotPromptSelected()) {
            ConnectionHandler connection = object.getConnection();
            connection.getSettings().getDetailSettings().setEnableDdlFileBinding(false);
        }

        super.doOKAction();
    }
}
