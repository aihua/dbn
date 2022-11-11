package com.dci.intellij.dbn.debugger.common.evaluation;

import com.dci.intellij.dbn.debugger.common.frame.DBDebugStackFrame;
import com.dci.intellij.dbn.debugger.common.frame.DBDebugValue;
import com.dci.intellij.dbn.debugger.common.process.DBDebugProcess;
import com.dci.intellij.dbn.language.common.psi.IdentifierPsiElement;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.dci.intellij.dbn.language.common.psi.QualifiedIdentifierPsiElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DBDebuggerEvaluator<F extends DBDebugStackFrame<? extends DBDebugProcess, V>, V extends DBDebugValue> extends XDebuggerEvaluator {
    private F frame;

    public DBDebuggerEvaluator(F frame) {
        this.frame = frame;
    }

    public boolean evaluateCondition(@NotNull String expression) {
        return false;
    }

    public String evaluateMessage(@NotNull String expression) {
        return null;
    }

    @Override
    public void evaluate(@NotNull String expression, @NotNull XEvaluationCallback callback, @Nullable XSourcePosition expressionPosition) {
        evaluate(expression, callback);
    }

    public void evaluate(@NotNull String expression, XEvaluationCallback callback) {
        V value = frame.getValue(expression);
        if (value == null) {
            value = frame.createDebugValue(expression, null, null, null);
            frame.setValue(expression, value);
        }

        String errorMessage = value.getType();
        if (errorMessage != null) {
            callback.errorOccurred(errorMessage);
        } else {
            callback.evaluated(value);
        }
    }

    public abstract void computePresentation(@NotNull V debugValue, @NotNull final XValueNode node, @NotNull XValuePlace place);

    @Nullable
    public TextRange getExpressionRangeAtOffset(Project project, Document document, int offset) {
        PsiFile psiFile = PsiUtil.getPsiFile(project, document);
        if (psiFile != null) {
            PsiElement psiElement = psiFile.findElementAt(offset);
            if (psiElement != null && psiElement.getParent() instanceof IdentifierPsiElement) {
                return psiElement.getTextRange();
            }
        }
        return null;
    }

    @Nullable
    @Override
    public TextRange getExpressionRangeAtOffset(Project project, Document document, int offset, boolean sideEffectsAllowed) {
        PsiFile psiFile = PsiUtil.getPsiFile(project, document);
        if (psiFile != null) {
            PsiElement psiElement = psiFile.findElementAt(offset);
            if (psiElement != null && psiElement.getParent() instanceof IdentifierPsiElement) {
                IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) psiElement.getParent();
                QualifiedIdentifierPsiElement qualifiedIdentifier = identifierPsiElement.getParentQualifiedIdentifier();
                if (qualifiedIdentifier == null) {
                    return identifierPsiElement.getTextRange();
                } else {
                    int startOffset = qualifiedIdentifier.getTextRange().getStartOffset();
                    int endOffset = identifierPsiElement.getTextRange().getEndOffset();
                    return new TextRange(startOffset, endOffset);
                }

            }
        }
        return null;
    }
}
