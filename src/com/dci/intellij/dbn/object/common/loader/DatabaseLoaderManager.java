package com.dci.intellij.dbn.object.common.loader;

import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.SafeDisposer;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.util.DocumentUtil;
import com.dci.intellij.dbn.common.util.EditorUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionLoadListener;
import com.dci.intellij.dbn.connection.mapping.FileConnectionMappingManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class DatabaseLoaderManager extends AbstractProjectComponent {
    private DatabaseLoaderQueue loaderQueue;

    private DatabaseLoaderManager(Project project) {
        super(project);
        ProjectEvents.subscribe(project, this, ConnectionLoadListener.TOPIC,
                connectionHandler -> Dispatch.run(() -> {
                    checkDisposed();
                    Failsafe.nn(project);
                    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                    FileConnectionMappingManager connectionMappingManager = FileConnectionMappingManager.getInstance(project);
                    VirtualFile[] openFiles = fileEditorManager.getOpenFiles();
                    for (VirtualFile openFile : openFiles) {

                        checkDisposed();
                        ConnectionHandler activeConnection = connectionMappingManager.getConnectionHandler(openFile);
                        if (activeConnection == connectionHandler) {
                            FileEditor[] fileEditors = fileEditorManager.getEditors(openFile);
                            for (FileEditor fileEditor : fileEditors) {

                                checkDisposed();
                                Editor editor = EditorUtil.getEditor(fileEditor);
                                DocumentUtil.refreshEditorAnnotations(editor);
                            }

                        }
                    }
                }));
    }

    public static DatabaseLoaderManager getInstance(@NotNull Project project) {
        return Failsafe.getComponent(project, DatabaseLoaderManager.class);
    }

    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.Project.DatabaseLoaderManager";
    }

    @Override
    public void disposeInner() {
        SafeDisposer.dispose(loaderQueue);
        super.disposeInner();
    }
}
