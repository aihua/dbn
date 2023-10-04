package com.dci.intellij.dbn.language.editor.action;

import com.dci.intellij.dbn.common.action.Lookups;
import com.dci.intellij.dbn.common.ui.misc.DBNComboBoxAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.mapping.FileConnectionContextManager;
import com.dci.intellij.dbn.ddl.DDLFileAttachmentManager;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.vfs.DatabaseFileSystem;
import com.dci.intellij.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ConnectionSelectDropdownAction extends DBNComboBoxAction implements DumbAware {
    private static final String NAME = "DB Connections";

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent component) {
        Project project = Lookups.getProject(component);
        return new ConnectionSelectActionGroup(project);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        String text = NAME;
        Icon icon = null;

        Project project = Lookups.getProject(e);
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        if (project != null && virtualFile != null) {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            ConnectionHandler activeConnection = contextManager.getConnection(virtualFile);
            if (activeConnection != null) {
                text = activeConnection.getName();
                icon = activeConnection.getIcon();
            }

            boolean isConsole = virtualFile instanceof DBConsoleVirtualFile;
            presentation.setVisible(!isConsole);

            if (virtualFile.isInLocalFileSystem()) {
                DDLFileAttachmentManager fileAttachmentManager = DDLFileAttachmentManager.getInstance(project);
                DBObjectRef mappedObject = fileAttachmentManager.getMappedObjectRef(virtualFile);
                if (mappedObject != null) {
                    boolean isOpened = DatabaseFileSystem.isFileOpened(mappedObject);
                    presentation.setEnabled(!isOpened);
                }
            }
        }

        presentation.setText(text, false);
        presentation.setIcon(icon);
    }
 }
