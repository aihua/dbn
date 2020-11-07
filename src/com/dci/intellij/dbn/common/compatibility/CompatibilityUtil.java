package com.dci.intellij.dbn.common.compatibility;

import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CompatibilityUtil {
    private static final Key<PsiFile> HARD_REF_TO_PSI = Key.create("HARD_REFERENCE_TO_PSI");

    public static Color getEditorBackgroundColor(EditorEx editorEx) {
        return editorEx.getBackgroundColor();
    }

    public static void showSearchCompletionPopup(boolean byClickingToolbarButton, JComponent toolbarComponent, JBList list, String title, JTextField textField) {
        Utils.showCompletionPopup(byClickingToolbarButton ? toolbarComponent : null, list, title, textField, "", (JBPopupListener) null);
    }


    public static void setSmallerFont(JComponent component) {
        Utils.setSmallerFont(component);
    }

    public static boolean isUnderGTKLookAndFeel() {
        return SystemInfo.isXWindow && UIManager.getLookAndFeel().getName().contains("GTK");
    }


    @Nullable
    public static FileEditor getSelectedEditor(Project project) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        //return fileEditorManager.getSelectedEditor();

        VirtualFile[] files = fileEditorManager.getSelectedFiles();
        return files.length == 0 ? null : fileEditorManager.getSelectedEditor(files[0]);
    }
}
