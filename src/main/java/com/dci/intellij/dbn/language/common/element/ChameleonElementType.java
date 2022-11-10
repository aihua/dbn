package com.dci.intellij.dbn.language.common.element;

import com.dci.intellij.dbn.code.common.style.formatting.FormattingDefinition;
import com.dci.intellij.dbn.language.common.*;
import com.dci.intellij.dbn.language.common.element.cache.ElementTypeLookupCache;
import com.dci.intellij.dbn.language.common.element.impl.LeafElementType;
import com.dci.intellij.dbn.language.common.element.impl.WrappingDefinition;
import com.dci.intellij.dbn.language.common.element.parser.Branch;
import com.dci.intellij.dbn.language.common.element.parser.ElementTypeParser;
import com.dci.intellij.dbn.language.common.element.path.LanguageNode;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.psi.ChameleonPsiElement;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.ILazyParseableElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ChameleonElementType extends ILazyParseableElementType implements ElementType, TokenType {
    private final DBLanguageDialect parentLanguage;
    public ChameleonElementType(DBLanguageDialect language,DBLanguageDialect parentLanguage) {
        super("chameleon (" + language.getDisplayName() + ")", language, false);
        this.parentLanguage = parentLanguage;
    }

    @Override
    public int index() {
        return -1;
    }

    @Override
    public TokenTypeBundleBase getBundle() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public String getId() {
        return "";
    }

    @Override
    protected ASTNode doParseContents(@NotNull final ASTNode chameleon, @NotNull final PsiElement psi) {
        Project project = psi.getProject();
        DBLanguageDialect languageDialect = getLanguageDialect();
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageDialect, chameleon.getChars());
        PsiParser parser = languageDialect.getParserDefinition().getParser();
        return parser.parse(this, builder).getFirstChildNode();
    }

    @NotNull
    @Override
    public DBLanguage getLanguage() {
        return getLanguageDialect().getBaseLanguage();
    }

    @Override
    public DBLanguageDialect getLanguageDialect() {
        return (DBLanguageDialect) super.getLanguage();
    }

    @Override
    public String getDescription() {
        return getName();
    }

    @NotNull
    @Override
    public String getName() {
        return "chameleon (" + getLanguage().getDisplayName() + ")";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public ElementType getParent() {
        return null;
    }

    @Override
    public ElementTypeLookupCache getLookupCache() {
        return null;
    }

    @Override
    public ElementTypeParser getParser() {
        return null;
    }

    @Override
    public FormattingDefinition getFormatting() {
        return null;
    }

    @Override
    public TokenPairTemplate getTokenPairTemplate() {
        return null;
    }

    @Override
    public void setDefaultFormatting(FormattingDefinition defaults) {
    }

    @Override
    public WrappingDefinition getWrapping() {
        return null;
    }

    @Override
    public boolean isWrappingBegin(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean isWrappingBegin(TokenType tokenType) {return false;}

    @Override
    public boolean isWrappingEnd(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean isWrappingEnd(TokenType tokenType) {return false;}

    @Override
    @Nullable
    public Branch getBranch() {
        return null;
    }

    @Override
    public boolean isScopeDemarcation() {
        return true;
    }

    @Override
    public boolean isScopeIsolation() {
        return true;
    }

    @Override
    public TokenType getTokenType() {
        return null;
    }

    @Override
    public int getIndexInParent(LanguageNode node) {
        return 0;
    }

    @Override
    public boolean is(ElementTypeAttribute attribute) {
        return false;
    }

    @Override
    public boolean set(ElementTypeAttribute status, boolean value) {
        throw new AbstractMethodError("Operation not allowed");
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean isVirtualObject() {
        return false;
    }

    @Override
    public DBObjectType getVirtualObjectType() {
        return null;
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new ChameleonPsiElement(astNode, this);
    }

    @Override
    public ElementTypeBundle getElementBundle() {
        return getLanguageDialect().getParserDefinition().getParser().getElementTypes();
    }

    public DBLanguageDialect getParentLanguage() {
        return parentLanguage;
    }

    @Override
    public int getLookupIndex() {
        return 0;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public String getTypeName() {
        return null;
    }

    @Override
    public boolean isSuppressibleReservedWord() {
        return false;
    }

    @Override
    public boolean isIdentifier() {
        return false;
    }

    @Override
    public boolean isVariable() {
        return false;
    }

    @Override
    public boolean isQuotedIdentifier() {
        return false;
    }

    @Override
    public boolean isKeyword() {
        return false;
    }

    @Override
    public boolean isFunction() {
        return false;
    }

    @Override
    public boolean isParameter() {
        return false;
    }

    @Override
    public boolean isDataType() {
        return false;
    }

    @Override
    public boolean isCharacter() {
        return false;
    }

    @Override
    public boolean isOperator() {
        return false;
    }

    @Override
    public boolean isChameleon() {
        return true;
    }

    @Override
    public boolean isReservedWord() {
        return false;
    }

    @Override
    public boolean isParserLandmark() {
        return false;
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    @NotNull
    public TokenTypeCategory getCategory() {
        return TokenTypeCategory.CHAMELEON;
    }

    @Nullable
    @Override
    public DBObjectType getObjectType() {
        return null;
    }

    @Override
    public boolean isOneOf(TokenType... tokenTypes) {
        for (TokenType tokenType : tokenTypes) {
            if (this == tokenType) return true;
        }
        return false;
    }

    @Override
    public boolean matches(TokenType tokenType) {
        return this.equals(tokenType);
    }
}
