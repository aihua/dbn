package com.dci.intellij.dbn.common.editor;

import com.dci.intellij.dbn.common.thread.Read;
import com.dci.intellij.dbn.common.thread.Write;
import com.dci.intellij.dbn.common.util.Documents;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.CodeFoldingState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Data;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dci.intellij.dbn.common.dispose.Failsafe.conditionallyLog;
import static com.dci.intellij.dbn.common.dispose.Failsafe.nd;
import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.integerAttribute;
import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.stringAttribute;

@Data
public class BasicTextEditorState implements FileEditorState {
    private int line;
    private int column;
    private int selectionStart;
    private int selectionEnd;
    private float verticalScrollProportion;
    private CodeFoldingState foldingState;

    @Override
    public boolean canBeMergedWith(@NotNull FileEditorState fileEditorState, @NotNull FileEditorStateLevel fileEditorStateLevel) {
        return fileEditorState instanceof BasicTextEditorState;
    }

    public void readState(@NotNull Element sourceElement, final Project project, final VirtualFile virtualFile) {
        line = integerAttribute(sourceElement, "line", 0);
        column = integerAttribute(sourceElement, "column", 0);
        selectionStart = integerAttribute(sourceElement, "selection-start", 0);
        selectionEnd = integerAttribute(sourceElement, "selection-end", 0);
        verticalScrollProportion = Float.parseFloat(stringAttribute(sourceElement, "vertical-scroll-proportion"));

        Element foldingElement = sourceElement.getChild("folding");
        if (foldingElement != null) {
            Read.run(() -> {
                Document document = Documents.getDocument(virtualFile);
                CodeFoldingManager instance = CodeFoldingManager.getInstance(project);
                if (document != null) {
                    CodeFoldingState foldingState = instance.readFoldingState(foldingElement, document);
                    setFoldingState(foldingState);
                }
            });
        }

    }

    public void writeState(Element targetElement, Project project) {
        targetElement.setAttribute("line", Integer.toString(line));
        targetElement.setAttribute("column", Integer.toString(column));
        targetElement.setAttribute("selection-start", Integer.toString(selectionStart));
        targetElement.setAttribute("selection-end", Integer.toString(selectionEnd));
        targetElement.setAttribute("vertical-scroll-proportion", Float.toString(verticalScrollProportion));
        if (foldingState != null) {
            Element foldingElement = new Element("folding");
            targetElement.addContent(foldingElement);
            try {
                CodeFoldingManager.getInstance(project).writeFoldingState(foldingState, foldingElement);
            } catch (WriteExternalException e) { // TODO
                conditionallyLog(e);
            } catch (Exception e) {
                conditionallyLog(e);
                e.printStackTrace();
            }
        }
    }

    public void loadFromEditor(@NotNull FileEditorStateLevel level, @NotNull final TextEditor textEditor) {
        Editor editor = textEditor.getEditor();
        SelectionModel selectionModel = editor.getSelectionModel();
        LogicalPosition logicalPosition = editor.getCaretModel().getLogicalPosition();

        line = logicalPosition.line;
        column = logicalPosition.column;

        if(FileEditorStateLevel.FULL == level) {
            selectionStart = selectionModel.getSelectionStart();
            selectionEnd = selectionModel.getSelectionEnd();
            Project project = editor.getProject();
            if (project != null && !editor.isDisposed()) {
                foldingState = CodeFoldingManager.getInstance(project).saveFoldingState(editor);
            }

/*
            new WriteActionRunner() {
                @Override
                public void run() {
                    Editor editor = textEditor.getEditor();
                    Project project = editor.getProject();
                    if (project != null && !editor.isDisposed()) {
                        try {
                            PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
                        } catch (ProcessCanceledException ignore) {
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        foldingState = CodeFoldingManager.getInstance(project).saveFoldingState(editor);
                    }
                }
            }.start();
*/
        }
        verticalScrollProportion = level != FileEditorStateLevel.UNDO ? EditorUtil.calcVerticalScrollProportion(editor) : -1F;
    }

    public void applyToEditor(@NotNull TextEditor textEditor) {
        Editor editor = nd(textEditor.getEditor());
        SelectionModel selectionModel = editor.getSelectionModel();

        LogicalPosition logicalPosition = new LogicalPosition(line, column);
        editor.getCaretModel().moveToLogicalPosition(logicalPosition);
        selectionModel.removeSelection();
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        if (verticalScrollProportion != -1F)
            EditorUtil.setVerticalScrollProportion(editor, verticalScrollProportion);
        final Document document = editor.getDocument();
        if (selectionStart == selectionEnd) {
            selectionModel.removeSelection();
        } else {
            int selectionStart = Math.min(this.selectionStart, document.getTextLength());
            int selectionEnd = Math.min(this.selectionEnd, document.getTextLength());
            selectionModel.setSelection(selectionStart, selectionEnd);
        }
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);

        if (foldingState != null) {
            Write.run(() -> {
                Project project = nd(editor.getProject());
                //PsiDocumentManager.getInstance(project).commitDocument(document);
                editor.getFoldingModel().runBatchFoldingOperation(
                        () -> {
                            CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(project);
                            foldingManager.restoreFoldingState(editor, foldingState);
                        }
                );
            });
            //editor.getFoldingModel().runBatchFoldingOperation(runnable);
        }
    }
}
