package com.dci.intellij.dbn.language.common.psi;

import com.dci.intellij.dbn.common.property.PropertyHolderImpl;
import com.dci.intellij.dbn.common.util.Safe;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionHandlerRef;
import com.dci.intellij.dbn.language.common.PsiElementRef;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.DBObjectPsiElement;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.dci.intellij.dbn.language.common.psi.PsiResolveStatus.*;

public class PsiResolveResult extends PropertyHolderImpl<PsiResolveStatus>{
    private ConnectionHandlerRef connectionHandler;
    private DBObjectRef<DBSchema> databaseSchema;
    private PsiElementRef<IdentifierPsiElement> element;
    private PsiElementRef<BasePsiElement> parent;
    private PsiElementRef referencedElement;
    private WeakRef<DBObject> underlyingObject;
    private CharSequence text;
    private long lastResolveInvocation = 0;
    private int scopeTextLength;
    private int resolveAttempts = 0;

    PsiResolveResult(IdentifierPsiElement element) {
        this.connectionHandler = ConnectionHandlerRef.from(element.getConnectionHandler());
        this.element = PsiElementRef.from(element);
        set(PsiResolveStatus.NEW, true);
    }

    @Override
    protected PsiResolveStatus[] properties() {
        return PsiResolveStatus.values();
    }

    public void accept(IdentifierPsiElement element) {
        this.element = PsiElementRef.from(element);
    }

    public void preResolve(IdentifierPsiElement psiElement) {
        set(RESOLVING, true);
        this.text = psiElement.getUnquotedText();
        ConnectionHandler connectionHandler = psiElement.getConnectionHandler();
        set(CONNECTION_VALID, connectionHandler != null && !connectionHandler.isVirtual() && connectionHandler.isValid());
        set(CONNECTION_ACTIVE, connectionHandler != null && !connectionHandler.isVirtual() && connectionHandler.canConnect());
        this.referencedElement = null;
        this.parent = null;
        this.connectionHandler = ConnectionHandlerRef.from(connectionHandler);
        this.databaseSchema = DBObjectRef.of(psiElement.getDatabaseSchema());
        BasePsiElement enclosingScopePsiElement = psiElement.getEnclosingScopePsiElement();
        this.scopeTextLength = enclosingScopePsiElement == null ? 0 : enclosingScopePsiElement.getTextLength();
        if (StringUtil.isEmpty(text)) {
            text = "";
        }
    }

    public void postResolve(boolean cancelled) {
        set(NEW, false);
        if (!cancelled) {
            PsiElement referencedElement = this.referencedElement == null ? null : this.referencedElement.get();
            this.resolveAttempts = referencedElement == null ? resolveAttempts + 1 : 0;
            this.lastResolveInvocation = System.currentTimeMillis();
        }
        set(RESOLVING, false);
    }

    public boolean isResolving() {
        return is(RESOLVING);
    }

    public boolean isNew() {
        return is(NEW);
    }

    boolean isDirty() {
        if (isResolving()) return false;
        if (isNew()) return true;
        if (isConnectionChanged()) {
            resolveAttempts = 0;
            return true;
        }

        PsiElement referencedElement = getReferencedElement();
        if (referencedElement == null) {
            if (resolveAttempts > 0) {
                return lastResolveInvocation < System.currentTimeMillis() - (resolveAttempts * 5000);
            } else {
                return true;
            }
        }

        if (!referencedElement.isValid()) {
            return true;
        }

        if (elementTextChanged() || enclosingScopeChanged()) {
            resolveAttempts = 0;
            return true;
        }

        IdentifierPsiElement element = this.element.get();
        if (element != null && !element.textMatches(referencedElement.getText())) {
            return true;
        }

        BasePsiElement parent = getParent();
        if (parent != null) {
            if (!parent.isValid()) {
                return true;
            } else if (referencedElement instanceof DBObjectPsiElement) {
                DBObjectPsiElement objectPsiElement = (DBObjectPsiElement) referencedElement;
                if (!objectPsiElement.isValid()) {
                    return true;
                }
                DBObject parentObject = objectPsiElement.ensureObject().getParentObject();
                DBObject underlyingObject = parent.getUnderlyingObject();
                return parentObject != null && !parentObject.isVirtual() &&
                        underlyingObject != null && !underlyingObject.isVirtual() &&
                        !Objects.equals(parentObject, underlyingObject);
            }
        } else {
            return element != null && element.isPrecededByDot();
        }
        return false;
    }

    private boolean isConnectionChanged() {
        if (connectionChanged() || schemaChanged()) {
            return true;
        }
        IdentifierPsiElement element = this.element.get();
        ConnectionHandler activeConnection = element == null ? null : element.getConnectionHandler();
        if (activeConnection == null || activeConnection.isVirtual()) {
            if (databaseSchema != null) return true;
        } else {
            if (connectionBecameActive(activeConnection) || connectionBecameValid(activeConnection)) {
                return true;
            }
        }
        return false;
    }

    private BasePsiElement getParent() {
        return Safe.call(parent, source -> source.get());
    }

    private boolean elementTextChanged() {
        IdentifierPsiElement element = this.element.get();
        return element!= null && !element.textMatches(text);
    }

    private boolean connectionChanged() {
        IdentifierPsiElement element = this.element.get();
        return element != null && getConnectionHandler() != element.getConnectionHandler();
    }

    private boolean schemaChanged() {
        IdentifierPsiElement element = this.element.get();
        return element != null && !Safe.equal(DBObjectRef.get(databaseSchema), element.getDatabaseSchema());
    }

    private boolean connectionBecameValid(ConnectionHandler connectionHandler) {
        return isNot(CONNECTION_VALID) && connectionHandler!= null && !connectionHandler.isVirtual() && connectionHandler.isValid();
    }

    private boolean connectionBecameActive(ConnectionHandler connectionHandler) {
        return isNot(CONNECTION_ACTIVE) && connectionHandler!= null && !connectionHandler.isVirtual() && connectionHandler.canConnect();
    }

    private boolean enclosingScopeChanged() {
        IdentifierPsiElement element = this.element.get();
        if (element != null) {
            BasePsiElement scopePsiElement = element.getEnclosingScopePsiElement();
            int scopeTextLength = scopePsiElement == null ? 0 : scopePsiElement.getTextLength();
            return this.scopeTextLength != scopeTextLength;
        }
        return false;
    }

    @NotNull
    public DBObjectType getObjectType() {
        if (isNot(RESOLVING_OBJECT_TYPE)) {
            set(RESOLVING_OBJECT_TYPE, true);
            try {
                PsiElement referencedElement = getReferencedElement();
                if (referencedElement instanceof DBObjectPsiElement) {
                    DBObjectPsiElement objectPsiElement = (DBObjectPsiElement) referencedElement;
                    return objectPsiElement.getObjectType();
                }
                if (referencedElement instanceof IdentifierPsiElement) {
                    IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) referencedElement;
                    return identifierPsiElement.getObjectType();
                }

                if (referencedElement instanceof BasePsiElement) {
                    BasePsiElement basePsiElement = (BasePsiElement) referencedElement;
                    DBObject object = basePsiElement.getUnderlyingObject();
                    if (object != null) {
                        return object.getObjectType();
                    }
                }
            } finally {
                set(RESOLVING_OBJECT_TYPE, false);
            }
        }

        return DBObjectType.UNKNOWN;
    }

    /*********************************************************
     *                   Getters/Setters                     *
     *********************************************************/

    public CharSequence getText() {
        return text;
    }

    @Nullable
    public PsiElement getReferencedElement() {
        return Safe.call(referencedElement, source -> source.get());
    }

    public ConnectionHandler getConnectionHandler() {
        return ConnectionHandlerRef.get(connectionHandler);
    }

    public void setParent(@Nullable BasePsiElement parent) {
        this.parent = PsiElementRef.from(parent);
    }

    public void setReferencedElement(PsiElement referencedElement) {
        this.referencedElement = PsiElementRef.from(referencedElement);
    }

    public int getResolveAttempts() {
        return resolveAttempts;
    }

    public long getLastResolveInvocation() {
        return lastResolveInvocation;
    }

    public DBObject getUnderlyingObject() {
        return WeakRef.get(underlyingObject);
    }

    public void setUnderlyingObject(DBObject underlyingObject) {
        this.underlyingObject = WeakRef.of(underlyingObject);
    }
}
