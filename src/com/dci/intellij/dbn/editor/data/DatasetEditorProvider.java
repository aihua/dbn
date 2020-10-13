package com.dci.intellij.dbn.editor.data;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.SafeDisposer;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.EditorProviderId;
import com.dci.intellij.dbn.editor.data.state.DatasetEditorState;
import com.dci.intellij.dbn.object.DBDataset;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.dci.intellij.dbn.vfs.file.DBDatasetVirtualFile;
import com.dci.intellij.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.components.NamedComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class DatasetEditorProvider implements FileEditorProvider, NamedComponent, DumbAware {
    /*********************************************************
     *                  FileEditorProvider                   *
     *********************************************************/

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) virtualFile;
            DBContentType contentType = databaseFile.getContentType();
            return contentType == DBContentType.DATA || contentType == DBContentType.CODE_AND_DATA;

        }
        return false;
    }

    @Override
    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) file;
        DBDatasetVirtualFile datasetFile = (DBDatasetVirtualFile) databaseFile.getContentFile(DBContentType.DATA);
        datasetFile = Failsafe.nn(datasetFile);
        DBDataset dataset = datasetFile.getObject();
        return new DatasetEditor(databaseFile, dataset);
    }

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        // expensive task. start in background
        SafeDisposer.dispose(editor, true, true);
    }

    @Override
    @NotNull
    public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile virtualFile) {
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile editableObjectFile = (DBEditableObjectVirtualFile) virtualFile;
            DBObjectRef<DBSchemaObject> objectRef = editableObjectFile.getObjectRef();
            DBObjectType objectType = objectRef.objectType;
            if (objectType.isInheriting(DBObjectType.DATASET)) {
                DatasetEditorState editorState = new DatasetEditorState();
                editorState.readState(sourceElement);
                return editorState;

            }
        }
        return new DatasetEditorState();
    }

    @Override
    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        if (state instanceof DatasetEditorState) {
            DatasetEditorState editorState = (DatasetEditorState) state;
            editorState.writeState(targetElement);
        }
    }

    @Override
    @NotNull
    @NonNls
    public String getEditorTypeId() {
        return EditorProviderId.DATA.getId();
    }

    @Override
    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }


    /*********************************************************
     *                ApplicationComponent                   *
     *********************************************************/
    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.DatasetEditorProvider";
    }
}

