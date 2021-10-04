package com.dci.intellij.dbn.code.common.completion;

import com.dci.intellij.dbn.code.common.lookup.CodeCompletionLookupItem;
import com.dci.intellij.dbn.language.common.SimpleTokenType;
import com.dci.intellij.dbn.language.common.TokenType;
import com.dci.intellij.dbn.language.common.element.impl.TokenElementType;
import com.dci.intellij.dbn.language.common.psi.IdentifierPsiElement;
import com.dci.intellij.dbn.language.common.psi.LeafPsiElement;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;

import java.util.Set;

public class BasicInsertHandler implements InsertHandler<CodeCompletionLookupItem> {
    public static final BasicInsertHandler INSTANCE = new BasicInsertHandler();

    @Override
    public void handleInsert(InsertionContext insertionContext, CodeCompletionLookupItem lookupElement) {
        PsiFile file = insertionContext.getFile();
        int tailOffset = insertionContext.getTailOffset();

        Object lookupElementObject = lookupElement.getObject();
        if (lookupElementObject instanceof TokenElementType) {
            TokenElementType tokenElementType = (TokenElementType) lookupElementObject;
            TokenType tokenType = tokenElementType.getTokenType();
            if (tokenType.isReservedWord()) {
                /* TODO any considerations on completion char??
                    char completionChar = insertionContext.getCompletionChar();
                    if (completionChar == '\t' || completionChar == '\u0000' || completionChar == '\n') */

                if (tokenType.isFunction()) {
                    SimpleTokenType leftParenthesis = tokenElementType.getLanguage().getSharedTokenTypes().getChrLeftParenthesis();
                    Set nextPossibleTokens = tokenElementType.getLookupCache().getNextPossibleTokens();
                    if (nextPossibleTokens.contains(leftParenthesis)) {
                        addParenthesis(insertionContext);
                        shiftCaret(insertionContext, 1);
                    } else {
                        addWhiteSpace(insertionContext, tailOffset);
                        shiftCaret(insertionContext, 1);
                    }
                } else if (tokenType.isKeyword()) {
                    addWhiteSpace(insertionContext, tailOffset);
                    shiftCaret(insertionContext, 1);
                }
            }
        } else if (lookupElementObject instanceof DBObject) {
            DBObject object = (DBObject) lookupElementObject;
            LeafPsiElement leafPsiElement = PsiUtil.lookupLeafBeforeOffset(file, tailOffset);
            if (leafPsiElement instanceof IdentifierPsiElement) {
                IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) leafPsiElement;
                identifierPsiElement.resolveAs(object);

                if (identifierPsiElement.getObjectType().getGenericType() == DBObjectType.METHOD) {
                    addParenthesis(insertionContext);
                    shiftCaret(insertionContext, 1);
                }
            }
        }
    }

    private static void addWhiteSpace(InsertionContext insertionContext, int offset) {
        char completionChar = insertionContext.getCompletionChar();
        if (completionChar != ' ' && !isInlineSpace(insertionContext, offset)) {
            insertionContext.getDocument().insertString(offset, " ");
        }
    }

    private static void addParenthesis(InsertionContext insertionContext) {
        int tailOffset = insertionContext.getTailOffset();
        PsiFile file = insertionContext.getFile();

        boolean addWhiteSpace = !isInlineSpace(insertionContext, tailOffset);

        LeafPsiElement leafAtOffset = PsiUtil.lookupLeafAtOffset(file, tailOffset);
        if (leafAtOffset == null || !leafAtOffset.isToken(leafAtOffset.getLanguage().getSharedTokenTypes().getChrLeftParenthesis())) {
            insertionContext.getDocument().insertString(tailOffset, addWhiteSpace ? "() " : "()");
        }
    }

    private static void shiftCaret(InsertionContext insertionContext, int columnShift) {
        Editor editor = insertionContext.getEditor();
        CaretModel caretModel = editor.getCaretModel();
        caretModel.moveCaretRelatively(columnShift, 0, false, false, false);
    }

    private static boolean isInlineSpace(InsertionContext insertionContext, int offset) {
        PsiFile file = insertionContext.getFile();
        PsiElement element = file.findElementAt(offset);
        if (element instanceof PsiWhiteSpace) {
            String text = element.getText();
            return text.startsWith(" ") || text.startsWith("\t");
        }
        return false;
    }

    protected static boolean shouldInsertCharacter(char chr) {
        return chr != '\t' && chr != '\n' && chr!='\u0000';
    }
}
