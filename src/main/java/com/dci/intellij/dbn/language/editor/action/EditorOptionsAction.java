package com.dci.intellij.dbn.language.editor.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.GroupPopupAction;
import com.dci.intellij.dbn.common.action.Lookups;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.options.ConfigId;
import com.dci.intellij.dbn.options.action.ProjectSettingsOpenAction;
import com.dci.intellij.dbn.vfs.DBConsoleType;
import com.dci.intellij.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EditorOptionsAction extends GroupPopupAction {
    public EditorOptionsAction() {
        super("Options", "Options", Icons.ACTION_OPTIONS_MENU);
    }

    @Override
    protected AnAction[] getActions(AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        if (virtualFile instanceof DBConsoleVirtualFile) {
            actions.add(new ConsoleRenameAction());
            actions.add(new ConsoleDeleteAction());
            actions.add(new ConsoleSaveToFileAction());
            actions.add(Separator.getInstance());

            DBConsoleVirtualFile consoleVirtualFile = (DBConsoleVirtualFile) virtualFile;
            if (consoleVirtualFile.getType() != DBConsoleType.DEBUG) {
                actions.add(new ConsoleCreateAction(DBConsoleType.STANDARD));
            }

            ConnectionHandler connection = consoleVirtualFile.getConnection();
            if (DatabaseFeature.DEBUGGING.isSupported(connection)) {
                actions.add(new ConsoleCreateAction(DBConsoleType.DEBUG));
            }
        }
        actions.add(Separator.getInstance());
        actions.add(new ProjectSettingsOpenAction(ConfigId.CODE_EDITOR, false));

        return actions.toArray(new AnAction[0]);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        presentation.setVisible(virtualFile instanceof DBConsoleVirtualFile);
    }
}
