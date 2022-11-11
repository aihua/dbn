package com.dci.intellij.dbn.language.common.element.parser;

import com.dci.intellij.dbn.language.common.DBLanguageDialect;
import com.dci.intellij.dbn.language.common.SimpleTokenType;
import com.dci.intellij.dbn.language.common.TokenType;
import com.dci.intellij.dbn.language.common.TokenTypeBundle;
import com.dci.intellij.dbn.language.common.element.TokenPairTemplate;
import com.intellij.util.containers.Stack;
import lombok.Getter;
import lombok.Setter;

@Deprecated
public class OldTokenPairStack {
    private int stackSize = 0;
    private final Stack<TokenPairMarker> markersStack = new Stack<>();
    private final ParserBuilder builder;

    private final SimpleTokenType beginToken;
    private final SimpleTokenType endToken;


    public OldTokenPairStack(ParserBuilder builder, DBLanguageDialect languageDialect, TokenPairTemplate template) {
        this.builder = builder;

        TokenTypeBundle parserTokenTypes = languageDialect.getParserTokenTypes();
        beginToken = parserTokenTypes.getTokenType(template.getBeginToken());
        endToken = parserTokenTypes.getTokenType(template.getEndToken());
    }

    /**
     * cleanup all markers registered after the builder offset (remained dirty after a marker rollback)
     */
    public void rollback() {
        int builderOffset = builder.getOffset();
        while (markersStack.size() > 0) {
            TokenPairMarker lastMarker = markersStack.peek();
            if (lastMarker.getOffset() >= builderOffset) {
                markersStack.pop();
                if (stackSize > 0) stackSize--;
            } else {
                break;
            }
        }
    }

    public void acknowledge(boolean explicit) {
        TokenType tokenType = builder.getToken();
        if (tokenType == beginToken) {
            stackSize++;
            TokenPairMarker marker = new TokenPairMarker(builder.getOffset(), explicit);
            markersStack.push(marker);
        } else if (tokenType == endToken) {
            if (stackSize > 0) stackSize--;
            if (markersStack.size() > 0) {
/*
                NestedRangeMarker marker = markersStack.peek();
                ParsePathNode markerNode = marker.getParseNode();
                if (markerNode == node) {

                } else if (markerNode.isSiblingOf(node)) {

                } else if (node.isSiblingOf(markerNode)) {
                    WrappingDefinition childWrapping = node.getElementType().getWrapping();
                    WrappingDefinition parentWrapping = markerNode.getElementType().getWrapping();
                    if (childWrapping != null && childWrapping.equals(parentWrapping)) {

                    }
                }
*/
                cleanup(false);
            }
        }
    }

    public void cleanup(boolean force) {
        if (force) stackSize = 0;
        while(markersStack.size() > stackSize) {
            markersStack.pop();
        }
    }

    public boolean isExplicitRange() {
        if (!markersStack.isEmpty()) {
            TokenPairMarker marker = markersStack.peek();
            return marker.isExplicit();
        }

        return false;
    }

    public void setExplicitRange(boolean value) {
        if (!markersStack.isEmpty()) {
            TokenPairMarker marker = markersStack.peek();
            marker.setExplicit(value);
        }
    }

    @Getter
    @Setter
    public static class TokenPairMarker {
        private final int offset;
        private boolean explicit;

        public TokenPairMarker(int offset, boolean explicit) {
            this.offset = offset;
            this.explicit = explicit;
        }

        @Override
        public String toString() {
            return offset + " " + explicit;
        }
    }
}
