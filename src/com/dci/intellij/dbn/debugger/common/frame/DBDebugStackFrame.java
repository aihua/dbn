package com.dci.intellij.dbn.debugger.common.frame;

import com.dci.intellij.dbn.code.common.style.DBLCodeStyleManager;
import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.LoggerFactory;
import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.util.DocumentUtil;
import com.dci.intellij.dbn.debugger.DBDebugUtil;
import com.dci.intellij.dbn.debugger.common.process.DBDebugProcess;
import com.dci.intellij.dbn.editor.code.SourceCodeManager;
import com.dci.intellij.dbn.execution.ExecutionTarget;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.dci.intellij.dbn.language.common.psi.IdentifierPsiElement;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.dci.intellij.dbn.language.psql.PSQLLanguage;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.dci.intellij.dbn.vfs.DBVirtualFile;
import com.dci.intellij.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DBDebugStackFrame<P extends DBDebugProcess, V extends DBDebugValue> extends XStackFrame {
    private static final Logger LOGGER = LoggerFactory.createLogger();
    private final P debugProcess;
    private final int frameIndex;
    private Map<String, V> valuesMap;

    private final Latent<VirtualFile> virtualFile = Latent.basic(() -> resolveVirtualFile());
    private final Latent<XSourcePosition> sourcePosition = Latent.basic(() -> resolveSourcePosition());

    private final Latent<IdentifierPsiElement> subject = Latent.basic(() -> {
        Project project = getDebugProcess().getProject();
        XSourcePosition sourcePosition = getSourcePosition();
        VirtualFile virtualFile = getVirtualFile();
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) virtualFile;
            SourceCodeManager.getInstance(project).ensureSourcesLoaded(databaseFile.getObject(), true);
        }


        if (virtualFile != null) {
            Document document = DocumentUtil.getDocument(virtualFile);
            DBLanguagePsiFile psiFile = (DBLanguagePsiFile) PsiUtil.getPsiFile(project, document);

            if (sourcePosition != null && psiFile != null && document != null) {
                int offset = document.getLineEndOffset(sourcePosition.getLine());
                PsiElement elementAtOffset = psiFile.findElementAt(offset);
                while (elementAtOffset instanceof PsiWhiteSpace || elementAtOffset instanceof PsiComment) {
                    elementAtOffset = elementAtOffset.getNextSibling();
                }

                if (elementAtOffset instanceof BasePsiElement) {
                    BasePsiElement basePsiElement = (BasePsiElement) elementAtOffset;
                    BasePsiElement objectDeclarationPsiElement = basePsiElement.findEnclosingPsiElement(ElementTypeAttribute.OBJECT_DECLARATION);
                    if (objectDeclarationPsiElement != null) {
                        return (IdentifierPsiElement) objectDeclarationPsiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
                    }
                }
            }
        }

        return null;
    });

    public DBDebugStackFrame(P debugProcess, int frameIndex) {
        this.debugProcess = debugProcess;
        this.frameIndex = frameIndex;
    }

    protected abstract XSourcePosition resolveSourcePosition();

    protected abstract VirtualFile resolveVirtualFile();

    @NotNull
    @Override
    public abstract XDebuggerEvaluator getEvaluator();

    @Nullable
    @Override
    public final XSourcePosition getSourcePosition() {
        return sourcePosition.get();
    }

    @Nullable
    public final VirtualFile getVirtualFile() {
        return virtualFile.get();
    }



    public IdentifierPsiElement getSubject() {
        return subject.get();
    }

    public P getDebugProcess() {
        return debugProcess;
    }

    public int getFrameIndex() {
        return frameIndex;
    }


    public V getValue(String variableName) {
        return valuesMap == null ? null : valuesMap.get(variableName.toLowerCase());
    }

    public void setValue(String variableName, V value) {
        if (valuesMap == null) {
            valuesMap = new THashMap<>();
        }
        valuesMap.put(variableName.toLowerCase(), value);
    }

    @Nullable
    protected abstract V createSuspendReasonDebugValue();

    @NotNull
    public abstract V createDebugValue(String variableName, V parentValue, Set<String> childVariableNames, Icon icon);

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        valuesMap = new THashMap<>();
        List<DBDebugValue> values = new ArrayList<>();

        V frameInfoValue = createSuspendReasonDebugValue();
        if (frameInfoValue != null) {
            values.add(frameInfoValue);
            valuesMap.put(frameInfoValue.getName(), frameInfoValue);
        }

        XSourcePosition sourcePosition = getSourcePosition();
        VirtualFile virtualFile = DBDebugUtil.getSourceCodeFile(sourcePosition);

        if (virtualFile != null) {
            if (virtualFile instanceof DBEditableObjectVirtualFile) {
                virtualFile = ((DBEditableObjectVirtualFile) virtualFile).getMainContentFile();
            }
            Project project = getDebugProcess().getProject();
            Document document = DocumentUtil.getDocument(virtualFile);
            DBLanguagePsiFile psiFile = (DBLanguagePsiFile) PsiUtil.getPsiFile(project, virtualFile);

            if (document != null && psiFile != null) {
                int offset = document.getLineStartOffset(sourcePosition.getLine());
                Set<BasePsiElement> variables = psiFile.lookupVariableDefinition(offset);
                CodeStyleCaseSettings codeStyleCaseSettings = DBLCodeStyleManager.getInstance(psiFile.getProject()).getCodeStyleCaseSettings(PSQLLanguage.INSTANCE);
                CodeStyleCaseOption objectCaseOption = codeStyleCaseSettings.getObjectCaseOption();

                for (final BasePsiElement basePsiElement : variables) {
                    String variableName = objectCaseOption.format(basePsiElement.getText());
                    //DBObject object = basePsiElement.resolveUnderlyingObject();

                    Set<String> childVariableNames = null;
                    if (basePsiElement instanceof IdentifierPsiElement) {
                        IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) basePsiElement;
                        List<BasePsiElement> qualifiedUsages = identifierPsiElement.findQualifiedUsages();
                        for (BasePsiElement qualifiedUsage : qualifiedUsages) {
                            if (childVariableNames == null) childVariableNames = new HashSet<>();

                            String childVariableName = objectCaseOption.format(qualifiedUsage.getText());
                            childVariableNames.add(childVariableName);
                        }
                    }

                    String valueCacheKey = variableName.toUpperCase();
                    if (!valuesMap.containsKey(valueCacheKey)) {
                        Icon icon = basePsiElement.getIcon(true);
                        V value = createDebugValue(variableName, null, childVariableNames, icon);
                        values.add(value);
                        valuesMap.put(valueCacheKey, value);
                    }
                }
            }
        }

        Collections.sort(values);

        XValueChildrenList children = new XValueChildrenList();
        for (DBDebugValue value : values) {
            children.add(value.getVariableName(), value);
        }
        node.addChildren(children, true);
    }

    @Override
    public void customizePresentation(@NotNull ColoredTextContainer component) {
        XSourcePosition sourcePosition = getSourcePosition();
        VirtualFile virtualFile = DBDebugUtil.getSourceCodeFile(sourcePosition);

        DBSchemaObject object = DBDebugUtil.getObject(sourcePosition);
        if (object != null) {
            String frameName = object.getName();
            Icon frameIcon = object.getIcon();

            IdentifierPsiElement subject = getSubject();
            if (subject != null) {
                DBObjectType objectType = subject.getObjectType();
                frameName = frameName + "." + subject.getChars();
                frameIcon = objectType.getIcon();
            }

            component.append(frameName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            component.append(" (line " + (sourcePosition.getLine() + 1) + ") ", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
            component.setIcon(frameIcon);

        } else if (virtualFile != null){
            Icon frameIcon;
            if (virtualFile instanceof DBVirtualFile) {
                frameIcon = ((DBVirtualFile) virtualFile).getIcon();
            } else {
                frameIcon = virtualFile.getFileType().getIcon();
            }
            component.setIcon(frameIcon);
            component.append(virtualFile.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            component.append(" (line " + (sourcePosition.getLine() + 1) + ") ", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
        } else if (getDebugProcess().getExecutionTarget() == ExecutionTarget.METHOD) {
            component.append("Anonymous block (method runner)", SimpleTextAttributes.GRAY_ATTRIBUTES);
            component.setIcon(Icons.FILE_SQL_DEBUG_CONSOLE);
        } else {
            component.append(XDebuggerBundle.message("invalid.frame"), SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }


}
