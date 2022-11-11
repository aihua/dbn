package com.dci.intellij.dbn.editor.code.ui;

import com.dci.intellij.dbn.common.environment.EnvironmentManager;
import com.dci.intellij.dbn.common.message.MessageType;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.code.SourceCodeEditor;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.options.ConfigId;
import com.dci.intellij.dbn.options.ProjectSettingsManager;
import com.dci.intellij.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.project.Project;

import static com.dci.intellij.dbn.common.message.MessageCallback.when;

public class SourceCodeReadonlyNotificationPanel extends SourceCodeEditorNotificationPanel{
    public SourceCodeReadonlyNotificationPanel(DBSchemaObject schemaObject, SourceCodeEditor sourceCodeEditor) {
        super(isReadonly(sourceCodeEditor) ? MessageType.NEUTRAL : MessageType.WARNING);
        DBSourceCodeVirtualFile sourceCodeFile = sourceCodeEditor.getVirtualFile();
        String environmentName = sourceCodeFile.getEnvironmentType().getName();

        Project project = schemaObject.getProject();
        DBContentType contentType = sourceCodeEditor.getContentType();

        if (isReadonly(sourceCodeEditor)) {
            setText("READONLY CODE - This is meant to prevent accidental code changes in \"" + environmentName + "\" environments (check environment settings)");
            createActionLabel("Edit Mode", () ->
                    Messages.showQuestionDialog(project,
                            "Enable edit-mode",
                            "Are you sure you want to enable editing for " + schemaObject.getQualifiedNameWithType(),
                            new String[]{"Yes", "Cancel"}, 0,
                            option -> when(option == 0, () -> {
                                EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
                                environmentManager.enableEditing(schemaObject, contentType);
                            })));
        } else {
            setText("EDITABLE CODE! - Edit-mode enabled (the environment \"" + environmentName + "\" is configured as \"Readonly Code\")");
            createActionLabel("Cancel Editing", () -> {
                EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
                environmentManager.disableEditing(schemaObject, contentType);
            });
        }

        createActionLabel("Settings", () -> {
            ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(project);
            settingsManager.openProjectSettings(ConfigId.GENERAL);
        });
    }

    private static boolean isReadonly(SourceCodeEditor sourceCodeEditor) {
        Project project = sourceCodeEditor.getProject();
        EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
        return environmentManager.isReadonly(sourceCodeEditor.getVirtualFile());
    }
}
