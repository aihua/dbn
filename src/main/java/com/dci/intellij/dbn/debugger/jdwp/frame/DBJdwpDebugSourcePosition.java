package com.dci.intellij.dbn.debugger.jdwp.frame;

import com.dci.intellij.dbn.common.compatibility.Compatibility;
import com.dci.intellij.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.debugger.SourcePosition;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class DBJdwpDebugSourcePosition extends SourcePosition {
    private final PsiFile file;
    private final int line;

    public DBJdwpDebugSourcePosition(PsiFile file, int line) {
        this.file = file;
        this.line = line;
    }

    @NotNull
    public PsiFile getFile() {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeVirtualFile = (DBSourceCodeVirtualFile) virtualFile;
            return sourceCodeVirtualFile.getObject().getPsiCache().getPsiFile();
        }
        return file;
    }

    @Override
    public PsiElement getElementAt() {
        return null;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public Editor openEditor(boolean requestFocus) {
        return null;
    }

    @Override
    @Compatibility
    public void navigate(boolean requestFocus) {

    }

    @Override
    @Compatibility
    public boolean canNavigate() {
        return false;
    }

    @Override
    @Compatibility
    public boolean canNavigateToSource() {
        return false;
    }
}
