package com.dci.intellij.dbn.language.common.psi;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.connection.ConnectionHandler;
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
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

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
        return node.getFirstChildNode().getChars();
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
        Set<DBObject> parentObjects = null;
        Set<DBObjectType> parentTypes = objectType.getGenericParents();
        if (parentTypes.size() > 0) {
            if (objectType.isSchemaObject()) {
                ConnectionHandler connectionHandler = sourceScope.getConnectionHandler();

                if (connectionHandler != null && !connectionHandler.isVirtual()) {
                    DBObjectBundle objectBundle = connectionHandler.getObjectBundle();

                    if (filter == null || filter.acceptsCurrentSchemaObject(objectType)) {
                        DBSchema currentSchema = sourceScope.getDatabaseSchema();
                        parentObjects = addObjectToSet(parentObjects, currentSchema);
                    }

                    if (filter == null || filter.acceptsPublicSchemaObject(objectType)) {
                        DBSchema publicSchema = objectBundle.getPublicSchema();
                        parentObjects = addObjectToSet(parentObjects, publicSchema);
                    }
                }
            }

            Set<BasePsiElement> parentObjectPsiElements = null;
            for (DBObjectType parentObjectType : parentTypes) {
                PsiLookupAdapter lookupAdapter = new ObjectLookupAdapter(lookupIssuer, parentObjectType, null);
                lookupAdapter.setAssertResolved(true);

                parentObjectPsiElements = !objectType.isSchemaObject() && parentObjectType.isSchemaObject() ?
                        lookupAdapter.collectInScope(sourceScope, parentObjectPsiElements) :
                        lookupAdapter.collectInParentScopeOf(sourceScope, parentObjectPsiElements);
            }

            if (parentObjectPsiElements != null) {
                for (BasePsiElement parentObjectPsiElement : parentObjectPsiElements) {
                    if (!parentObjectPsiElement.containsPsiElement(sourceScope)) {
                        DBObject parentObject = parentObjectPsiElement.getUnderlyingObject();
                        parentObjects = addObjectToSet(parentObjects, parentObject);
                    }
                }
            }
        }

        DBObject fileObject = sourceScope.getFile().getUnderlyingObject();
        if (fileObject != null && fileObject.getObjectType().isParentOf(objectType)) {
            parentObjects = addObjectToSet(parentObjects, fileObject);
        }

        return parentObjects;
    }

    private static Set<DBObject> addObjectToSet(Set<DBObject> objects, DBObject object) {
        if (Failsafe.check(object)) {
            if (objects == null) objects = new THashSet<>();
            objects.add(object);
        }
        return objects;
    }

    @Override
    public BasePsiElement findPsiElementByAttribute(ElementTypeAttribute attribute) {
        return elementType.is(attribute) ? this : null;
    }

    @Override
    public BasePsiElement findFirstPsiElement(ElementTypeAttribute attribute) {
        if (elementType.is(attribute)) {
            return this;
        }
        return null;
    }

    @Override
    public BasePsiElement findFirstPsiElement(Class<? extends ElementType> clazz) {
        if (elementType.getClass().isAssignableFrom(clazz)) {
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
}
