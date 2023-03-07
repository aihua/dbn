package com.dci.intellij.dbn.editor.console;

import com.dci.intellij.dbn.common.editor.BasicTextEditorProvider;
import com.dci.intellij.dbn.editor.EditorProviderId;
import com.dci.intellij.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;


public class SQLConsoleEditorProvider extends BasicTextEditorProvider implements DumbAware{

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile instanceof DBConsoleVirtualFile;
    }

    @Override
    @NotNull
    public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile virtualFile) {
        SQLConsoleEditorState editorState = new SQLConsoleEditorState();
        editorState.readState(sourceElement, project, virtualFile);
        return editorState;
    }

    @Override
    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        if (state instanceof SQLConsoleEditorState) {
            SQLConsoleEditorState editorState = (SQLConsoleEditorState) state;
            editorState.writeState(targetElement, project);
        }
    }

    @Override
    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        DBConsoleVirtualFile consoleVirtualFile = (DBConsoleVirtualFile) file;
        SQLConsoleEditor editor = new SQLConsoleEditor(project, consoleVirtualFile, "SQL Console", getEditorProviderId());

        Document document = editor.getEditor().getDocument();
        int documentSignature = document.hashCode();
        if (document.hashCode() != consoleVirtualFile.getDocumentSignature()) {
            document.addDocumentListener(consoleVirtualFile);
            consoleVirtualFile.setDocumentSignature(documentSignature);
        }
        return editor;
    }

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        editor.dispose();
    }

    @Override
    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    @NotNull
    @Override
    public EditorProviderId getEditorProviderId() {
        return EditorProviderId.CONSOLE;
    }

    /*********************************************************
     *                ApplicationComponent                   *
     *********************************************************/

    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.SQLConsoleEditorProvider";
    }

}
