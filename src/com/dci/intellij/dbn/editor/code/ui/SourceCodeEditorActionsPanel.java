package com.dci.intellij.dbn.editor.code.ui;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.util.ActionUtil;
import com.dci.intellij.dbn.editor.code.SourceCodeEditor;
import com.dci.intellij.dbn.editor.code.SourceCodeManagerAdapter;
import com.dci.intellij.dbn.editor.code.SourceCodeManagerListener;
import com.dci.intellij.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SourceCodeEditorActionsPanel extends DBNFormImpl{
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel loadingDataPanel;
    private JLabel loadingLabel;
    private JPanel loadingIconPanel;

    private SourceCodeEditor sourceCodeEditor;

    public SourceCodeEditorActionsPanel(@NotNull SourceCodeEditor sourceCodeEditor) {
        this.sourceCodeEditor = sourceCodeEditor;
        Editor editor = sourceCodeEditor.getEditor();
        Project project = sourceCodeEditor.getProject();
        JComponent editorComponent = editor.getComponent();
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("", true, "DBNavigator.ActionGroup.SourceEditor");
        actionToolbar.setTargetComponent(editorComponent);
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
        loadingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
        loadingDataPanel.setVisible(false);

        subscribe(project, this, SourceCodeManagerListener.TOPIC, sourceCodeManagerListener);
    }

    private SourceCodeManagerListener sourceCodeManagerListener = new SourceCodeManagerAdapter() {
        @Override
        public void sourceCodeLoading(@NotNull DBSourceCodeVirtualFile sourceCodeFile) {
            DBSourceCodeVirtualFile virtualFile = getSourceCodeEditor().getVirtualFile();
            if (virtualFile.equals(sourceCodeFile)) {
                Dispatch.run(() -> loadingDataPanel.setVisible(true));
            }
        }

        @Override
        public void sourceCodeLoaded(@NotNull DBSourceCodeVirtualFile sourceCodeFile, boolean initialLoad) {
            DBSourceCodeVirtualFile virtualFile = getSourceCodeEditor().getVirtualFile();
            if (virtualFile.equals(sourceCodeFile)) {
                Dispatch.run(() -> loadingDataPanel.setVisible(false));
            }
        }
    };

    public SourceCodeEditor getSourceCodeEditor() {
        return Failsafe.nn(sourceCodeEditor);
    }

    @NotNull
    @Override
    public JPanel ensureComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        Object data = super.getData(dataId);
        if (data == null) {
            return getSourceCodeEditor().getData(dataId);
        }
        return data;
    }
}
