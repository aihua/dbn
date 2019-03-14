package com.dci.intellij.dbn.language.common.element.impl;

import com.dci.intellij.dbn.language.common.element.ElementType;
import com.dci.intellij.dbn.language.common.element.ElementTypeBundle;
import com.dci.intellij.dbn.language.common.element.ExecVariableElementType;
import com.dci.intellij.dbn.language.common.element.LeafElementType;
import com.dci.intellij.dbn.language.common.element.lookup.ExecVariableElementTypeLookupCache;
import com.dci.intellij.dbn.language.common.element.parser.impl.ExecVariableElementTypeParser;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dci.intellij.dbn.language.common.psi.ExecVariablePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;


public class ExecVariableElementTypeImpl extends LeafElementTypeImpl implements ExecVariableElementType {

    public ExecVariableElementTypeImpl(ElementTypeBundle bundle, ElementType parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
        setTokenType(bundle.getTokenTypeBundle().getVariable());
    }

    @Override
    public ExecVariableElementTypeLookupCache createLookupCache() {
        return new ExecVariableElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    public ExecVariableElementTypeParser createParser() {
        return new ExecVariableElementTypeParser(this);
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new ExecVariablePsiElement(astNode, this);
    }

    @Override
    public String getDebugName() {
        return "variable (" + getId() + ")";
    }

    public String toString() {
        return "variable (" + getId() + ")";
    }

    @Override
    public boolean isSameAs(LeafElementType elementType) {
        return elementType instanceof ExecVariableElementType;
    }

    @Override
    public boolean isIdentifier() {
        return true;
    }
}
