package com.dci.intellij.dbn.language.common.parameter;

import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dci.intellij.dbn.code.psql.style.options.PSQLCodeStyleSettings;
import com.dci.intellij.dbn.common.compatibility.Compatibility;
import com.dci.intellij.dbn.language.common.element.ElementType;
import com.dci.intellij.dbn.language.common.element.impl.IterationElementType;
import com.dci.intellij.dbn.language.common.element.impl.TokenElementType;
import com.dci.intellij.dbn.language.common.element.impl.WrapperElementType;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.dci.intellij.dbn.language.common.psi.IdentifierPsiElement;
import com.dci.intellij.dbn.language.common.psi.NamedPsiElement;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.dci.intellij.dbn.language.common.psi.lookup.ObjectReferenceLookupAdapter;
import com.dci.intellij.dbn.object.DBArgument;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.DBProgram;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.list.DBObjectList;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MethodParameterInfoHandler implements ParameterInfoHandler<BasePsiElement, DBMethod> {
    public static final ObjectReferenceLookupAdapter METHOD_LOOKUP_ADAPTER = new ObjectReferenceLookupAdapter(null, DBObjectType.METHOD, null);
    public static final ObjectReferenceLookupAdapter ARGUMENT_LOOKUP_ADAPTER = new ObjectReferenceLookupAdapter(null, DBObjectType.ARGUMENT, null);

    @Override
    @Compatibility
    public boolean couldShowInLookup() {
        return true;
    }

    @Nullable
    @Override
    @Compatibility
    public Object[] getParametersForLookup(LookupElement item, ParameterInfoContext context) {
        return null;
    }

    @Nullable
    @Override
    @Compatibility
    public Object[] getParametersForDocumentation(DBMethod method, ParameterInfoContext context) {
        return null;
    }

    @Nullable
    @Override
    public BasePsiElement findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        BasePsiElement handlerPsiElement = lookupHandlerElement(context.getFile(), context.getOffset());
        if (handlerPsiElement != null) {
            NamedPsiElement enclosingNamedPsiElement = handlerPsiElement.findEnclosingNamedPsiElement();
            if (enclosingNamedPsiElement != null) {
                BasePsiElement methodPsiElement = METHOD_LOOKUP_ADAPTER.findInElement(enclosingNamedPsiElement);
                if (methodPsiElement instanceof IdentifierPsiElement) {
                    IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) methodPsiElement;
                    DBObject object = identifierPsiElement.getUnderlyingObject();
                    if (object instanceof DBMethod) {
                        DBMethod method = (DBMethod) object;
                        DBProgram program = method.getProgram();
                        if (program != null) {
                            DBObjectList objectList = program.getChildObjectList(method.getObjectType());
                            if (objectList != null) {
                                List<DBMethod> methods = objectList.getObjects(method.getName());
                                context.setItemsToShow(methods.toArray());
                            }
                        } else {
                            context.setItemsToShow(new Object[]{method});
                        }
                        return identifierPsiElement;
                    }
                }
            }
        }
        return null;
    }


    public static BasePsiElement lookupHandlerElement(PsiFile file, int offset) {
        PsiElement psiElement = file.findElementAt(offset);
        while (psiElement != null && !(psiElement instanceof PsiFile)) {
            if (psiElement instanceof BasePsiElement) {
                ElementType elementType = PsiUtil.getElementType(psiElement);
                if (elementType instanceof WrapperElementType) {
                    WrapperElementType wrapperElementType = (WrapperElementType) elementType;
                    if (wrapperElementType.is(ElementTypeAttribute.METHOD_PARAMETER_HANDLER)) {
                        return (BasePsiElement) psiElement;
                    } else {
                        return null;
                    }
                }
            }
            psiElement = psiElement.getParent();
        }
        return null;
    }

    @Override
    public void showParameterInfo(@NotNull BasePsiElement element, @NotNull CreateParameterInfoContext context) {
        context.showHint(element, context.getOffset(), this);
    }

    @Nullable
    @Override
    public BasePsiElement findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        int offset = context.getOffset();
        BasePsiElement handlerPsiElement = lookupHandlerElement(context.getFile(), offset);
        if (handlerPsiElement != null) {
            BasePsiElement iterationPsiElement = handlerPsiElement.findFirstPsiElement(IterationElementType.class);
            if (iterationPsiElement != null) {
                IterationElementType iterationElementType = (IterationElementType) iterationPsiElement.getElementType();
                PsiElement paramPsiElement = iterationPsiElement.getFirstChild();
                int paramIndex = -1;
                BasePsiElement iteratedPsiElement = null;
                while (paramPsiElement != null) {
                    ElementType elementType = PsiUtil.getElementType(paramPsiElement);
                    if (elementType instanceof TokenElementType) {
                        TokenElementType tokenElementType = (TokenElementType) elementType;
                        if (iterationElementType.isSeparator(tokenElementType.getTokenType())){
                            if (paramPsiElement.getTextOffset() >= offset) {
                                break;
                            }
                        }
                    }
                    if (elementType == iterationElementType.getIteratedElementType()) {
                        iteratedPsiElement = (BasePsiElement) paramPsiElement;
                        paramIndex++;
                    }

                    paramPsiElement = paramPsiElement.getNextSibling();
                }
                context.setCurrentParameter(paramIndex);
                return iteratedPsiElement;
            } else {
                if (handlerPsiElement.getTextOffset()< offset && handlerPsiElement.getTextRange().contains(offset)) {
                    return handlerPsiElement;
                }
            }
        }

        return null;
    }

    @Override
    public void updateParameterInfo(@NotNull BasePsiElement parameter, @NotNull UpdateParameterInfoContext context) {
        BasePsiElement handlerPsiElement = lookupHandlerElement(context.getFile(), context.getOffset());
        if (handlerPsiElement != null) {
            BasePsiElement iterationPsiElement = handlerPsiElement.findFirstPsiElement(IterationElementType.class);
            if (iterationPsiElement != null) {
                BasePsiElement argumentPsiElement = ARGUMENT_LOOKUP_ADAPTER.findInElement(parameter);
                if (argumentPsiElement != null) {
                    DBObject object = argumentPsiElement.getUnderlyingObject();
                    if (object instanceof DBArgument) {
                        DBArgument argument = (DBArgument) object;
                        context.setCurrentParameter(argument.getPosition() -1);
                        return;
                    }
                }

                IterationElementType iterationElementType = (IterationElementType) iterationPsiElement.getElementType();
                int index = 0;
                PsiElement paramPsiElement = iterationPsiElement.getFirstChild();
                while (paramPsiElement != null) {
                    ElementType elementType = PsiUtil.getElementType(paramPsiElement);
                    if (elementType == iterationElementType.getIteratedElementType()) {
                        if (paramPsiElement == parameter) {
                            context.setCurrentParameter(index);
                            return;
                        }
                        index++;
                    }
                    paramPsiElement = paramPsiElement.getNextSibling();
                }
                context.setCurrentParameter(index);
            }
        }
    }

    @Nullable
    @Override
    @Compatibility
    public String getParameterCloseChars() {
        return ",";
    }

    @Override
    @Compatibility
    public boolean tracksParameterIndex() {
        return false;
    }

    @Override
    public void updateUI(DBMethod method, @NotNull ParameterInfoUIContext context) {
        PSQLCodeStyleSettings codeStyleSettings = PSQLCodeStyleSettings.getInstance(method.getProject());
        CodeStyleCaseSettings caseSettings = codeStyleSettings.getCaseSettings();
        CodeStyleCaseOption datatypeCaseOption = caseSettings.getDatatypeCaseOption();
        CodeStyleCaseOption objectCaseOption = caseSettings.getObjectCaseOption();

        context.setUIComponentEnabled(true);
        StringBuilder text = new StringBuilder();
        int highlightStartOffset = 0;
        int highlightEndOffset = 0;
        int index = 0;
        int currentIndex = context.getCurrentParameterIndex();
        for (DBArgument argument : method.getArguments()) {
            if (argument != method.getReturnArgument()) {
                boolean highlight = index == currentIndex || (index == 0 && currentIndex == -1);
                if (highlight) {
                    highlightStartOffset = text.length();
                }
                if (text.length() > 0) {
                    text.append(", ");
                }
                text.append(objectCaseOption.format(argument.getName()));
                text.append(" ");
                text.append(datatypeCaseOption.format(argument.getDataType().getName()));
                //text.append(" ");
                //text.append(argument.getDataType().getQualifiedName());
                if (highlight) {
                    highlightEndOffset = text.length();
                }
                index++;
            }
        }
        boolean disable = highlightEndOffset == 0 && currentIndex > -1 && text.length() > 0;
        if (text.length() == 0) {
            text.append("<no parameters>");
        }
        context.setupUIComponentPresentation(text.toString(), highlightStartOffset, highlightEndOffset, disable, false, false, context.getDefaultParameterColor());
    }

    public void processFoundElementForUpdatingParameterInfo(@Nullable BasePsiElement basePsiElement, @NotNull UpdateParameterInfoContext context) {
        context.setParameterOwner(basePsiElement);
    }
}
