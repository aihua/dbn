package com.dci.intellij.dbn.language.common;

import com.dci.intellij.dbn.common.util.Commons;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import lombok.Getter;
import org.jdom.Document;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class DBLanguageSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey[] EMPTY_ARRAY = new TextAttributesKey[0];
    protected Map colors = new HashMap<>();
    protected Map backgrounds = new HashMap();

    private final DBLanguageDialect languageDialect;
    private final TokenTypeBundle tokenTypes;

    public DBLanguageSyntaxHighlighter(DBLanguageDialect languageDialect, String tokenTypesFile) {
        Document document = Commons.loadXmlFile(getResourceLookupClass(), tokenTypesFile);
        tokenTypes = new TokenTypeBundle(languageDialect, document);
        this.languageDialect = languageDialect;
    }

    protected Class getResourceLookupClass() {
        return getClass();
    }

    @NotNull
    protected abstract Lexer createLexer();

    @Override
    @NotNull
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType instanceof SimpleTokenType) {
            SimpleTokenType simpleTokenType = (SimpleTokenType) tokenType;
            return simpleTokenType.getTokenHighlights(() -> pack(
                        getAttributeKeys(tokenType, backgrounds),
                        getAttributeKeys(tokenType, colors)));
        } else {
            return EMPTY_ARRAY;
        }
    }

    private static TextAttributesKey getAttributeKeys(IElementType tokenType, Map map) {
        return (TextAttributesKey) map.get(tokenType);
    }

    @Override
    @NotNull
    public Lexer getHighlightingLexer() {
        return createLexer();
    }
}
