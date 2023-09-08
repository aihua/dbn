package com.dci.intellij.dbn.editor.code.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.connection.operation.options.OperationSettings;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.code.SourceCodeEditor;
import com.dci.intellij.dbn.execution.compiler.CompileType;
import com.dci.intellij.dbn.execution.compiler.CompilerAction;
import com.dci.intellij.dbn.execution.compiler.CompilerActionSource;
import com.dci.intellij.dbn.execution.compiler.DatabaseCompilerManager;
import com.dci.intellij.dbn.execution.compiler.options.CompilerSettings;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.common.property.DBObjectProperty;
import com.dci.intellij.dbn.object.common.status.DBObjectStatus;
import com.dci.intellij.dbn.object.common.status.DBObjectStatusHolder;
import com.dci.intellij.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ProgramCompileAction extends AbstractCodeEditorAction {
    public ProgramCompileAction() {
        super("", "", Icons.OBEJCT_COMPILE);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull SourceCodeEditor fileEditor, @NotNull DBSourceCodeVirtualFile sourceCodeFile) {
        DatabaseCompilerManager compilerManager = DatabaseCompilerManager.getInstance(project);
        CompilerSettings compilerSettings = getCompilerSettings(project);
        DBContentType contentType = sourceCodeFile.getContentType();
        CompilerAction compilerAction = new CompilerAction(CompilerActionSource.COMPILE, contentType, sourceCodeFile, fileEditor);
        compilerManager.compileInBackground(sourceCodeFile.getObject(), compilerSettings.getCompileType(), compilerAction);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project, @Nullable SourceCodeEditor fileEditor, @Nullable DBSourceCodeVirtualFile sourceCodeFile) {
        Presentation presentation = e.getPresentation();
        if (sourceCodeFile == null) {
            presentation.setEnabled(false);
        } else {

            DBSchemaObject schemaObject = sourceCodeFile.getObject();
            if (schemaObject.is(DBObjectProperty.COMPILABLE) && DatabaseFeature.OBJECT_INVALIDATION.isSupported(schemaObject)) {
                CompilerSettings compilerSettings = getCompilerSettings(schemaObject.getProject());
                CompileType compileType = compilerSettings.getCompileType();
                DBObjectStatusHolder objectStatus = schemaObject.getStatus();
                DBContentType contentType = sourceCodeFile.getContentType();

                boolean isDebug = compileType == CompileType.DEBUG;
                if (compileType == CompileType.KEEP) {
                    isDebug = objectStatus.is(contentType, DBObjectStatus.DEBUG);
                }

                boolean isPresent = objectStatus.is(contentType, DBObjectStatus.PRESENT);
                boolean isValid = objectStatus.is(contentType, DBObjectStatus.VALID);
                boolean isModified = sourceCodeFile.isModified();

                boolean isCompiling = objectStatus.is(contentType, DBObjectStatus.COMPILING);
                boolean isEnabled = !isModified && isPresent && !isCompiling && (compilerSettings.isAlwaysShowCompilerControls() || !isValid /*|| isDebug != isDebugActive*/);

                presentation.setEnabled(isEnabled);
                String text =
                        contentType == DBContentType.CODE_SPEC ? "Compile Spec" :
                        contentType == DBContentType.CODE_BODY ? "Compile Body" : "Compile";

                if (isDebug) text = text + " (Debug)";
                if (compileType == CompileType.ASK) text = text + "...";

                presentation.setVisible(true);
                presentation.setText(text);

                Icon icon = isDebug ?
                        CompileType.DEBUG.getIcon() :
                        CompileType.NORMAL.getIcon();
                presentation.setIcon(icon);
            } else {
                presentation.setVisible(false);
            }
        }
    }

    private static CompilerSettings getCompilerSettings(Project project) {
        return OperationSettings.getInstance(project).getCompilerSettings();
    }
}
