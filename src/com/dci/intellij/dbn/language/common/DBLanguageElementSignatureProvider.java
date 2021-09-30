package com.dci.intellij.dbn.language.common;

import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.intellij.codeInsight.folding.impl.ElementSignatureProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.StringTokenizer;

public class DBLanguageElementSignatureProvider implements ElementSignatureProvider {
    @Override
    public String getSignature(@NotNull PsiElement psiElement) {
        if (psiElement.getContainingFile() instanceof DBLanguagePsiFile) {
            TextRange textRange = psiElement.getTextRange();
            String offsets = textRange.getStartOffset() + "#" + textRange.getEndOffset();
            if (psiElement instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) psiElement;
                return basePsiElement.getElementType().getId() + "#" + offsets;
            }

            if (psiElement instanceof PsiComment) {
                return "comment#" + offsets;
            }
        }
        return null;
    }

    @Override
    public PsiElement restoreBySignature(@NotNull PsiFile psifile, @NotNull String signature, @Nullable StringBuilder processingInfoStorage) {
        if (psifile instanceof DBLanguagePsiFile) {
            StringTokenizer tokenizer = new StringTokenizer(signature, "#");
            String id = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
            String startOffsetToken = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
            String endOffsetToken = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;

            if (StringUtil.isNotEmptyOrSpaces(id) &&
                    StringUtil.isNotEmptyOrSpaces(startOffsetToken) &&
                    StringUtil.isNotEmptyOrSpaces(endOffsetToken) &&
                    StringUtil.isInteger(startOffsetToken) &&
                    StringUtil.isInteger(endOffsetToken)) {


                int startOffset = Integer.parseInt(startOffsetToken);
                int endOffset = Integer.parseInt(endOffsetToken);

                PsiElement psiElement = psifile.findElementAt(startOffset);
                if (psiElement instanceof PsiComment) {
                    if (Objects.equals(id, "comment") && endOffset == startOffset + psiElement.getTextLength()) {
                        return psiElement;
                    }
                }

                while (psiElement != null && !(psiElement instanceof PsiFile)) {
                    int elementStartOffset = psiElement.getTextOffset();
                    int elementEndOffset = elementStartOffset + psiElement.getTextLength();
                    if (elementStartOffset < startOffset || elementEndOffset > endOffset) {
                        break;
                    }
                    if (psiElement instanceof BasePsiElement) {
                        BasePsiElement basePsiElement = (BasePsiElement) psiElement;
                        if (Objects.equals(basePsiElement.getElementType().getId(), id) &&
                                elementStartOffset == startOffset &&
                                elementEndOffset == endOffset) {
                            return basePsiElement;
                        }
                    }
                    psiElement = psiElement.getParent();
                }

            }
        }
        return null;
    }

    public PsiElement restoreBySignature(PsiFile psifile, String signature) {
        return restoreBySignature(psifile, signature, null);

    }
}
