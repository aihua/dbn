package com.dci.intellij.dbn.language.common.element.parser.impl;

import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.language.common.SharedTokenTypeBundle;
import com.dci.intellij.dbn.language.common.SimpleTokenType;
import com.dci.intellij.dbn.language.common.TokenType;
import com.dci.intellij.dbn.language.common.element.impl.TokenElementType;
import com.dci.intellij.dbn.language.common.element.parser.*;
import com.dci.intellij.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.PsiBuilder.Marker;

public class TokenElementTypeParser extends ElementTypeParser<TokenElementType> {
    public TokenElementTypeParser(TokenElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) {
        ParserBuilder builder = context.getBuilder();
        Marker marker = null;

        TokenType token = builder.getToken();
        if (token == elementType.getTokenType() || builder.isDummyToken()) {

            String text = elementType.getText();
            if (Strings.isNotEmpty(text) && Strings.equalsIgnoreCase(builder.getTokenText(), text)) {
                marker = builder.markAndAdvance();
                return stepOut(marker, context, ParseResultType.FULL_MATCH, 1);
            }

            SharedTokenTypeBundle sharedTokenTypes = getElementBundle().getTokenTypeBundle().getSharedTokenTypes();
            SimpleTokenType leftParenthesis = sharedTokenTypes.getChrLeftParenthesis();
            SimpleTokenType dot = sharedTokenTypes.getChrDot();

            if (token.isSuppressibleReservedWord()) {
                TokenType nextTokenType = builder.getNextToken();
                if (nextTokenType == dot && !elementType.isNextPossibleToken(dot, parentNode, context)) {
                    context.setWavedTokenType(token);
                    return stepOut(marker, context, ParseResultType.NO_MATCH, 0);
                }
                if (token.isFunction() && elementType.getFlavor() == null) {
                    if (nextTokenType != leftParenthesis && elementType.isNextRequiredToken(leftParenthesis, parentNode, context)) {
                        context.setWavedTokenType(token);
                        return stepOut(marker, context, ParseResultType.NO_MATCH, 0);
                    }
                }
            }

            marker = builder.markAndAdvance();
            return stepOut(marker, context, ParseResultType.FULL_MATCH, 1);
        }
        return stepOut(marker, context, ParseResultType.NO_MATCH, 0);
    }
}
