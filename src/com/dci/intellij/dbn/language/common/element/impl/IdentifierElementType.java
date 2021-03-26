package com.dci.intellij.dbn.language.common.element.impl;

import com.dci.intellij.dbn.code.common.style.formatting.FormattingDefinition;
import com.dci.intellij.dbn.code.common.style.formatting.SpacingDefinition;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.language.common.element.ElementTypeBundle;
import com.dci.intellij.dbn.language.common.element.lookup.IdentifierElementTypeLookupCache;
import com.dci.intellij.dbn.language.common.element.parser.impl.IdentifierElementTypeParser;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeDefinition;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dci.intellij.dbn.language.common.element.util.IdentifierCategory;
import com.dci.intellij.dbn.language.common.element.util.IdentifierType;
import com.dci.intellij.dbn.language.common.psi.IdentifierDefPsiElement;
import com.dci.intellij.dbn.language.common.psi.IdentifierRefPsiElement;
import com.dci.intellij.dbn.language.common.resolve.UnderlyingObjectResolver;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;


public class IdentifierElementType extends LeafElementType {
    public static final FormattingDefinition FORMATTING = new FormattingDefinition(null, null, SpacingDefinition.ONE_SPACE, null);

    private IdentifierType identifierType;
    private IdentifierCategory identifierCategory;
    private DBObjectType objectType;
    private String underlyingObjectResolverId;
    private boolean referenceable; // is referenceable ()
    private boolean localReference; // is local reference


    public IdentifierElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
        setTokenType(bundle.getTokenTypeBundle().getIdentifier());
    }

    @Override
    public IdentifierElementTypeLookupCache createLookupCache() {
        return new IdentifierElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    public IdentifierElementTypeParser createParser() {
        return new IdentifierElementTypeParser(this);
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);
        String objectTypeName = ElementTypeBundle.determineMandatoryAttribute(def, "type", "Incomplete definition " + getId() + ". ");
        objectType = ElementTypeBundle.resolveObjectType(objectTypeName); 

        String type = def.getName();
        identifierType =
            ElementTypeDefinition.OBJECT_DEF.is(type) || ElementTypeDefinition.OBJECT_REF.is(type) ? IdentifierType.OBJECT :
            ElementTypeDefinition.ALIAS_DEF.is(type) || ElementTypeDefinition.ALIAS_REF.is(type) ? IdentifierType.ALIAS :
            ElementTypeDefinition.VARIABLE_DEF.is(type) || ElementTypeDefinition.VARIABLE_REF.is(type) ? IdentifierType.VARIABLE : IdentifierType.UNKNOWN;

        identifierCategory =
                    ElementTypeDefinition.OBJECT_REF.is(type) ||
                    ElementTypeDefinition.ALIAS_REF.is(type) ||
                    ElementTypeDefinition.VARIABLE_REF.is(type) ? IdentifierCategory.REFERENCE :
                    ElementTypeDefinition.OBJECT_DEF.is(type) ||
                    ElementTypeDefinition.ALIAS_DEF.is(type) ||
                    ElementTypeDefinition.VARIABLE_DEF.is(type) ? IdentifierCategory.DEFINITION : IdentifierCategory.UNKNOWN;

        referenceable = getBooleanAttribute(def, "referenceable");
        localReference = getBooleanAttribute(def, "local");

        underlyingObjectResolverId = StringUtil.intern(def.getAttributeValue("underlying-object-resolver"));

        if (isDefinition()) {
            setDefaultFormatting(FORMATTING);
        }

/*
        setDefaultFormatting();
        if (is(ElementTypeAttribute.STATEMENT)) {
            formatting = FormattingDefinitionFactory.mergeDefinitions(formatting, FormattingDefinition.STATEMENT_ATTRIBUTES_INDENTED);
        }
*/

    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return isDefinition() ?
                new IdentifierDefPsiElement(astNode, this) :
                new IdentifierRefPsiElement(astNode, this);
    }

    @NotNull
    @Override
    public String getName() {
        String prefix =
                isObject() ? (isReference() ? "object-ref " : "object-def ") :
                isAlias() ? (isReference() ? "alias-ref " : "alias-def ") :
                isVariable() ? (isReference() ? "variable-ref " : "variable-def ") : "unknown";
        return prefix + getObjectTypeName() + " (" + getId() + ")";
    }

    public String toString() {
        return getObjectTypeName() + " (" + getId() + ")";
    }

    /*********************************************************
     *                Identifier type accessors               *
     *********************************************************/

    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    public boolean isObject() {
        return identifierType == IdentifierType.OBJECT;
    }

    public boolean isAlias() {
        return identifierType == IdentifierType.ALIAS;
    }
    
    public boolean isVariable() {
        return identifierType == IdentifierType.VARIABLE;
    }

    public IdentifierCategory getIdentifierCategory() {
        return identifierCategory;
    }

    public boolean isReference() {
        return identifierCategory == IdentifierCategory.REFERENCE;
    }

    public boolean isReferenceable() {
        return referenceable;
    }

    public boolean isLocalReference() {
        return localReference;
    }

    public boolean isDefinition() {
        return identifierCategory == IdentifierCategory.DEFINITION;
    }

    public DBObjectType getObjectType() {
        return objectType;
    }

    public String getObjectTypeName() {
        return objectType.getName();
    }

    public String getQualifiedObjectTypeName() {
        return getObjectTypeName() + " " + identifierType.name().toLowerCase();

    }

    public boolean isObjectOfType(DBObjectType type) {
        return objectType.matches(type);
    }

    public boolean isSubject() {
        return is(ElementTypeAttribute.SUBJECT);
    }

    @Override
    public boolean isSameAs(LeafElementType elementType) {
        if (elementType instanceof IdentifierElementType) {
            IdentifierElementType identifierElementType = (IdentifierElementType) elementType;
            return  identifierElementType.objectType.matches(objectType) &&
                    identifierElementType.identifierType == identifierType &&
                    identifierElementType.identifierCategory == identifierCategory;
        }
        return false;
    }

    @Override
    public boolean isIdentifier() {
        return true;
    }

    public UnderlyingObjectResolver getUnderlyingObjectResolver() {
        return underlyingObjectResolverId == null ? null : UnderlyingObjectResolver.get(underlyingObjectResolverId);
    }
}
