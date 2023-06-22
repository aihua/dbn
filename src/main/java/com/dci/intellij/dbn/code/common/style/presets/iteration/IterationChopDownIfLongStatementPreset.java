package com.dci.intellij.dbn.code.common.style.presets.iteration;

import com.dci.intellij.dbn.language.common.element.ElementType;
import com.dci.intellij.dbn.language.common.element.impl.IterationElementType;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.dci.intellij.dbn.language.common.psi.NamedPsiElement;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.Nullable;

public class IterationChopDownIfLongStatementPreset extends IterationAbstractPreset {
    public IterationChopDownIfLongStatementPreset() {
        super("chop_down_if_statement_long", "Chop down if statement long");
    }

    @Override
    @Nullable
    public Wrap getWrap(BasePsiElement psiElement, CodeStyleSettings settings) {
        BasePsiElement<?> parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement == null) return null;

        IterationElementType iterationElementType = (IterationElementType) parentPsiElement.getElementType();
        ElementType elementType = psiElement.getElementType();

        NamedPsiElement namedPsiElement = parentPsiElement.findEnclosingElement(ElementTypeAttribute.EXECUTABLE);
        if (namedPsiElement == null) return null;

        boolean shouldWrap = namedPsiElement.approximateLength() > settings.getRightMargin(psiElement.getLanguage());
        return getWrap(elementType, iterationElementType, shouldWrap);
    }

    @Override
    @Nullable
    public Spacing getSpacing(BasePsiElement psiElement, CodeStyleSettings settings) {
        BasePsiElement<?> parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement == null) return null;

        IterationElementType iterationElementType = (IterationElementType) parentPsiElement.getElementType();
        ElementType elementType = psiElement.getElementType();

        NamedPsiElement namedPsiElement = parentPsiElement.findEnclosingElement(ElementTypeAttribute.EXECUTABLE);
        if (namedPsiElement == null) return null;

        boolean shouldWrap = namedPsiElement.approximateLength() > settings.getRightMargin(psiElement.getLanguage());
        return getSpacing(iterationElementType, elementType, shouldWrap);
    }
}