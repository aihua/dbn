package com.dci.intellij.dbn.language.common.psi;

import com.dci.intellij.dbn.common.consumer.SetCollector;
import com.dci.intellij.dbn.common.dispose.Checks;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.language.common.TokenType;
import com.dci.intellij.dbn.language.common.element.ElementType;
import com.dci.intellij.dbn.language.common.element.impl.LeafElementType;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.psi.lookup.ObjectLookupAdapter;
import com.dci.intellij.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.DBObjectBundle;
import com.dci.intellij.dbn.object.common.ObjectTypeFilter;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

public abstract class LeafPsiElement<T extends LeafElementType> extends BasePsiElement<T> implements PsiReference {

    public LeafPsiElement(ASTNode astNode, T elementType) {
        super(astNode, elementType);
    }

    @Override
    public int approximateLength() {
        return getTextLength() + 1;
    }

    @Override
    public PsiReference getReference() {
        return this;
    }

    public CharSequence getChars() {
        return getNode().getFirstChildNode().getChars();
    }

    /*********************************************************
     *                       PsiReference                    *
     *********************************************************/

    @NotNull
    @Override
    public PsiElement getElement() {
        return this;
    }

    @Override
    @Nullable
    public PsiElement resolve() {
        return null;
    }

    @Override
    @NotNull
    public String getCanonicalText() {
        PsiElement reference = resolve();
        return reference == null ? getText() : reference.getText();
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        return null;
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        return null;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return false;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement() {
        return new TextRange(0, getTextLength());
    }

    @Override
    public boolean isSoft() {
        return true;
    }

    @Override
    @NotNull
    public Object[] getVariants() {
        return PsiElement.EMPTY_ARRAY;
    }

    public static Set<DBObject> identifyPotentialParentObjects(DBObjectType objectType, @Nullable ObjectTypeFilter filter, @NotNull BasePsiElement sourceScope, LeafPsiElement lookupIssuer) {
        SetCollector<DBObject> parentObjects = SetCollector.create();
        Set<DBObjectType> parentTypes = objectType.getGenericParents();
        if (parentTypes.size() > 0) {
            if (objectType.isSchemaObject()) {
                ConnectionHandler connection = sourceScope.getConnection();

                if (connection != null && !connection.isVirtual()) {
                    DBObjectBundle objectBundle = connection.getObjectBundle();

                    if (filter == null || filter.acceptsCurrentSchemaObject(objectType)) {
                        DBSchema currentSchema = sourceScope.getSchema();
                        collectObject(parentObjects, currentSchema);
                    }

                    if (filter == null || filter.acceptsPublicSchemaObject(objectType)) {
                        DBSchema publicSchema = objectBundle.getPublicSchema();
                        collectObject(parentObjects, publicSchema);
                    }
                }
            }

            Consumer<BasePsiElement> parentObjectPsiElements = parentObjectPsiElement -> {
                if (!parentObjectPsiElement.containsPsiElement(sourceScope)) {
                    DBObject parentObject = parentObjectPsiElement.getUnderlyingObject();
                    collectObject(parentObjects, parentObject);
                }
            };
            for (DBObjectType parentObjectType : parentTypes) {
                PsiLookupAdapter lookupAdapter = new ObjectLookupAdapter(lookupIssuer, parentObjectType, null);
                lookupAdapter.setAssertResolved(true);

                if (!objectType.isSchemaObject() && parentObjectType.isSchemaObject())
                        lookupAdapter.collectInScope(sourceScope, parentObjectPsiElements); else
                        lookupAdapter.collectInParentScopeOf(sourceScope, parentObjectPsiElements);
            }
        }

        DBObject fileObject = sourceScope.getFile().getUnderlyingObject();
        if (fileObject != null && fileObject.getObjectType().isParentOf(objectType)) {
            collectObject(parentObjects, fileObject);
        }

        return parentObjects.elements();
    }

    private static void collectObject(Consumer<DBObject> objects, DBObject object) {
        if (Checks.isValid(object)) {
            objects.accept(object);
        }
    }

    @Override
    public BasePsiElement findPsiElementByAttribute(ElementTypeAttribute attribute) {
        return getElementType().is(attribute) ? this : null;
    }

    @Override
    public BasePsiElement findFirstPsiElement(ElementTypeAttribute attribute) {
        if (getElementType().is(attribute)) {
            return this;
        }
        return null;
    }

    @Override
    public BasePsiElement findFirstPsiElement(Class<? extends ElementType> clazz) {
        if (getElementType().getClass().isAssignableFrom(clazz)) {
            return this;
        }
        return null;
    }

    @Override
    public BasePsiElement findFirstLeafPsiElement() {
        return this;
    }

    @Override
    public boolean isScopeBoundary() {
        return false;
    }

    public boolean isCharacterToken() {
        return false;
    }

    public boolean isToken(TokenType tokenType) {
        return false;
    }

    @Override
    public PsiElement getFirstChild() {
        return null;
    }
}
