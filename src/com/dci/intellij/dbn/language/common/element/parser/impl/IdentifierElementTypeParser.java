package com.dci.intellij.dbn.language.common.element.parser.impl;

import com.dci.intellij.dbn.language.common.TokenType;
import com.dci.intellij.dbn.language.common.element.impl.IdentifierElementType;
import com.dci.intellij.dbn.language.common.element.parser.ElementTypeParser;
import com.dci.intellij.dbn.language.common.element.parser.ParseResult;
import com.dci.intellij.dbn.language.common.element.parser.ParseResultType;
import com.dci.intellij.dbn.language.common.element.parser.ParserBuilder;
import com.dci.intellij.dbn.language.common.element.parser.ParserContext;
import com.dci.intellij.dbn.language.common.element.path.ParsePathNode;
import com.intellij.lang.PsiBuilder;
import org.jetbrains.annotations.NotNull;

public class IdentifierElementTypeParser extends ElementTypeParser<IdentifierElementType> {
    public IdentifierElementTypeParser(IdentifierElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(@NotNull ParsePathNode parentNode, ParserContext context) {
        ParserBuilder builder = context.getBuilder();
        TokenType tokenType = builder.getTokenType();
        if (tokenType != null && !tokenType.isChameleon()){
            if (tokenType.isIdentifier()) {
                PsiBuilder.Marker marker = builder.mark(null);
                builder.advanceLexer(parentNode);
                return stepOut(marker, null, context, ParseResultType.FULL_MATCH, 1);
            }
            else if (isSuppressibleReservedWord(parentNode, context, tokenType)) {
                PsiBuilder.Marker marker = builder.mark(null);
                builder.advanceLexer(parentNode);
                return stepOut(marker, null, context, ParseResultType.FULL_MATCH, 1);
            }
        }
        return stepOut(null, null, context, ParseResultType.NO_MATCH, 0);
    }

    private boolean isSuppressibleReservedWord(ParsePathNode parentNode, ParserContext context, TokenType tokenType) {
        if (tokenType.isSuppressibleReservedWord()) {
            if (context.isWavedTokenType(tokenType)) {
                return true;
            }

            return (elementType.isDefinition() && !elementType.isAlias()) || isSuppressibleReservedWord(tokenType, parentNode, context);
        }
        return false;
    }
}