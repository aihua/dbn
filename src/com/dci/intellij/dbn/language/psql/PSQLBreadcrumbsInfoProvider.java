package com.dci.intellij.dbn.language.psql;

import com.intellij.lang.Language;
//import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;

public class PSQLBreadcrumbsInfoProvider /*implements BreadcrumbsProvider*/ {

    private static final Language[] LANGUAGES = {PSQLLanguage.INSTANCE};
/*
    @Override
    public Language[] getLanguages() {
        return LANGUAGES;
    }

    @Override
    public boolean acceptElement(@NotNull PsiElement psiElement) {
        IdentifierPsiElement identifierPsiElement = getBreadcrumbIdentifier(psiElement);
        return identifierPsiElement != null;
    }

    @NotNull
    @Override
    public String getElementInfo(@NotNull PsiElement psiElement) {
        IdentifierPsiElement identifierPsiElement = getBreadcrumbIdentifier(psiElement);

        return identifierPsiElement != null ? identifierPsiElement.getText() : "";
    }

    @Nullable
    @Override
    public Icon getElementIcon(@NotNull PsiElement psiElement) {
        IdentifierPsiElement identifierPsiElement = getBreadcrumbIdentifier(psiElement);
        if (identifierPsiElement != null) {
            return identifierPsiElement.getIcon(false);
        }
        return null;
    }

    @Nullable
    @Override
    public String getElementTooltip(@NotNull PsiElement element) {
        if (element instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) element;
            return basePsiElement.getElementType().getDescription();
        }
        return null;
    }

    @Nullable
    @Override
    public PsiElement getParent(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) parent;
            return basePsiElement.findEnclosingScopePsiElement();
        }
        return null;
    }

    @NotNull
    @Override
    public List<PsiElement> getChildren(@NotNull PsiElement element) {
        return Collections.emptyList();
    }

    @Nullable
    private IdentifierPsiElement getBreadcrumbIdentifier(@NotNull PsiElement psiElement) {
        if (psiElement instanceof NamedPsiElement) {
            NamedPsiElement namedPsiElement = (NamedPsiElement) psiElement;
            boolean isObject =
                    namedPsiElement.is(ElementTypeAttribute.OBJECT_DEFINITION) ||
                    namedPsiElement.is(ElementTypeAttribute.OBJECT_DECLARATION) ||
                    namedPsiElement.is(ElementTypeAttribute.OBJECT_SPECIFICATION);

            if (isObject) {
                BasePsiElement subject = namedPsiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
                if (subject instanceof IdentifierPsiElement) {
                    IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) subject;
                    DBObjectType objectType = identifierPsiElement.getObjectType();
                    if (objectType.matchesOneOf(
                            DBObjectType.METHOD,
                            DBObjectType.PROGRAM,
                            DBObjectType.SYNONYM,
                            DBObjectType.TYPE,
                            DBObjectType.CURSOR,
                            DBObjectType.TRIGGER)) {
                        return identifierPsiElement;
                    }
                }
            }
        }
        return null;
    }*/
}
