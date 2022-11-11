package com.dci.intellij.dbn.code.common.style.presets.iteration;

import com.dci.intellij.dbn.language.common.element.ElementType;
import com.dci.intellij.dbn.language.common.element.impl.IterationElementType;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.Nullable;

public class IterationChopDownIfNotSinglePreset extends IterationAbstractPreset {
    public IterationChopDownIfNotSinglePreset() {
        super("chop_down_if_not_single", "Chop down unless single element");
    }

    @Override
    @Nullable
    public Wrap getWrap(BasePsiElement psiElement, CodeStyleSettings settings) {
        BasePsiElement parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement != null) {
            IterationElementType iterationElementType = (IterationElementType) parentPsiElement.getElementType();
            ElementType elementType = psiElement.getElementType();

            boolean shouldWrap = PsiUtil.getChildCount(parentPsiElement) > 1;
            return getWrap(elementType, iterationElementType, shouldWrap);
        }

        return null;
    }

    @Override
    @Nullable
    public Spacing getSpacing(BasePsiElement psiElement, CodeStyleSettings settings) {
        BasePsiElement parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement != null) {
            IterationElementType iterationElementType = (IterationElementType) parentPsiElement.getElementType();
            ElementType elementType = psiElement.getElementType();

            boolean shouldWrap = PsiUtil.getChildCount(parentPsiElement) > 1;
            return getSpacing(iterationElementType, elementType, shouldWrap);
        }
        return null;
    }
}