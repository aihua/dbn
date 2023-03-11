package com.dci.intellij.dbn.language.common;

import com.dci.intellij.dbn.language.common.element.ChameleonElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import lombok.Getter;
import org.jdom.Document;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
public class TokenTypeBundle extends TokenTypeBundleBase {
    private final DBLanguage baseLanguage;

    private final IElementType integer;
    private final IElementType number;
    private final IElementType string;
    private final IElementType operator;
    private final IElementType keyword;
    private final IElementType function;
    private final IElementType variable;
    private final IElementType parameter;
    private final IElementType exception;
    private final IElementType dataType;

    private static final Set<String> GENERIC_TOKENS = new HashSet<>(Arrays.asList("INTEGER", "NUMBER", "STRING", "OPERATOR", "KEYWORD", "FUNCTION", "VARIABLE", "PARAMETER", "EXCEPTION", "DATA_TYPE"));

    public TokenTypeBundle(DBLanguageDialect languageDialect, Document document) {
        super(languageDialect, document);
        this.baseLanguage = languageDialect.getBaseLanguage();
        initIndex(getSharedTokenTypes().size());

        this.integer   = getTokenType("INTEGER");
        this.number    = getTokenType("NUMBER");
        this.string    = getTokenType("STRING");
        this.operator  = getTokenType("OPERATOR");
        this.keyword   = getTokenType("KEYWORD");
        this.function  = getTokenType("FUNCTION");
        this.variable  = getTokenType("VARIABLE");
        this.parameter = getTokenType("PARAMETER");
        this.exception = getTokenType("EXCEPTION");
        this.dataType  = getTokenType("DATA_TYPE");
    }

    public TokenType getTokenType(int index) {
        TokenType tokenType = super.getTokenType(index);
        if (tokenType == null ){
            return getSharedTokenTypes().getTokenType(index);
        }
        return tokenType;
    }

    public SharedTokenTypeBundle getSharedTokenTypes() {
        return baseLanguage.getSharedTokenTypes();
    }

    public DBLanguageDialect getLanguageDialect() {
        return (DBLanguageDialect) getLanguage();
    }

    @Override
    public SimpleTokenType getCharacterTokenType(int index) {
        return getSharedTokenTypes().getCharacterTokenType(index);
    }

    @Override
    public SimpleTokenType getOperatorTokenType(int index) {
        return getSharedTokenTypes().getOperatorTokenType(index);
    }

    @Override
    public SimpleTokenType getTokenType(String id) {
        SimpleTokenType tokenType = super.getTokenType(id);
        if (tokenType != null) return tokenType;

        tokenType = getSharedTokenTypes().getTokenType(id);
        if (tokenType != null) return tokenType;


        if (!GENERIC_TOKENS.contains(id)) System.out.println("DEBUG - [" + getLanguage().getID() + "] undefined token type: " + id);
        //log.info("[DBN-WARNING] Undefined token type: " + id);
        return getSharedTokenTypes().getIdentifier();
    }

    @Override
    public TokenSet getTokenSet(String id) {
        TokenSet tokenSet = super.getTokenSet(id);
        if (tokenSet != null) return tokenSet;

        tokenSet = getSharedTokenTypes().getTokenSet(id);
        if (tokenSet != null) return tokenSet;


        System.out.println("DEBUG - [" + getLanguage().getID() + "] undefined token set: " + id);
        //log.info("[DBN-WARNING] Undefined token set '" + id + "'");
        tokenSet = super.getTokenSet("UNDEFINED");
        return tokenSet;
    }

    public SimpleTokenType getIdentifier() {
        return getSharedTokenTypes().getIdentifier();
    }

    public SimpleTokenType getVariable() {
        return getSharedTokenTypes().getVariable();
    }

    public SimpleTokenType getString() {
        return getSharedTokenTypes().getString();
    }


    public ChameleonElementType getChameleon(DBLanguageDialectIdentifier dialectIdentifier) {
        return getLanguageDialect().getChameleonTokenType(dialectIdentifier);
    }
}
