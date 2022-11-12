package com.dci.intellij.dbn.language.sql;

import com.dci.intellij.dbn.code.sql.color.SQLTextAttributesKeys;
import com.dci.intellij.dbn.common.thread.ThreadMonitor;
import com.dci.intellij.dbn.common.thread.ThreadProperty;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionHandlerStatus;
import com.dci.intellij.dbn.execution.statement.StatementGutterRenderer;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.TokenTypeCategory;
import com.dci.intellij.dbn.language.common.psi.*;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import static com.dci.intellij.dbn.connection.mapping.FileConnectionContextManager.hasHasConnectivityContext;
import static com.dci.intellij.dbn.debugger.DatabaseDebuggerManager.isDebugConsole;

public class SQLLanguageAnnotator implements Annotator {
    public static final SQLLanguageAnnotator INSTANCE = new SQLLanguageAnnotator();

    @Override
    public void annotate(@NotNull final PsiElement psiElement, @NotNull final AnnotationHolder holder) {
        ThreadMonitor.run(null,
                ThreadProperty.CODE_ANNOTATING,
                () -> {
                    if (psiElement instanceof ExecutablePsiElement) {
                        annotateExecutable((ExecutablePsiElement) psiElement, holder);

                    } else if (psiElement instanceof ChameleonPsiElement) {
                        annotateChameleon(psiElement, holder);

                    } else if (psiElement instanceof TokenPsiElement) {
                        annotateToken((TokenPsiElement) psiElement, holder);

                    } else if (psiElement instanceof IdentifierPsiElement) {
                        IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) psiElement;
                        ConnectionHandler connection = identifierPsiElement.getConnection();
                        if (connection != null && !connection.isVirtual()) {
                            annotateIdentifier(identifierPsiElement, holder);
                        }
                    }


                    if (psiElement instanceof NamedPsiElement) {
                        NamedPsiElement namedPsiElement = (NamedPsiElement) psiElement;
                        if (namedPsiElement.hasErrors()) {
                            holder.createErrorAnnotation(namedPsiElement, "Invalid " + namedPsiElement.getElementType().getDescription());
                        }
                    }
                });
    }

    private static void annotateToken(@NotNull TokenPsiElement tokenPsiElement, AnnotationHolder holder) {
        TokenTypeCategory flavor = tokenPsiElement.getElementType().getFlavor();
        if (flavor != null) {
            Annotation annotation = holder.createInfoAnnotation(tokenPsiElement, null);
            switch (flavor) {
                case DATATYPE: annotation.setTextAttributes(SQLTextAttributesKeys.DATA_TYPE); break;
                case FUNCTION: annotation.setTextAttributes(SQLTextAttributesKeys.FUNCTION); break;
                case KEYWORD: annotation.setTextAttributes(SQLTextAttributesKeys.KEYWORD); break;
                case IDENTIFIER: annotation.setTextAttributes(SQLTextAttributesKeys.IDENTIFIER); break;
            }
        }
    }

    private void annotateIdentifier(@NotNull IdentifierPsiElement identifierPsiElement, final AnnotationHolder holder) {
        if (identifierPsiElement.getLanguageDialect().isReservedWord(identifierPsiElement.getText())) {
            Annotation annotation = holder.createInfoAnnotation(identifierPsiElement, null);
            annotation.setTextAttributes(SQLTextAttributesKeys.IDENTIFIER);
        }
        if (identifierPsiElement.isObject()) {
            annotateObject(identifierPsiElement, holder);
        } else if (identifierPsiElement.isAlias()) {
            if (identifierPsiElement.isReference())
                annotateAliasRef(identifierPsiElement, holder); else
                annotateAliasDef(identifierPsiElement, holder);
        }
    }

    private static void annotateAliasRef(@NotNull IdentifierPsiElement aliasReference, AnnotationHolder holder) {
        if (aliasReference.resolve() == null &&  aliasReference.getResolveAttempts() > 3) {
            Annotation annotation = holder.createWarningAnnotation(aliasReference, "Unknown identifier");
            annotation.setTextAttributes(SQLTextAttributesKeys.UNKNOWN_IDENTIFIER);
        } else {
            Annotation annotation = holder.createInfoAnnotation(aliasReference, null);
            annotation.setTextAttributes(SQLTextAttributesKeys.ALIAS);
        }
    }

    private void annotateAliasDef(IdentifierPsiElement aliasDefinition, @NotNull AnnotationHolder holder) {
        /*Set<BasePsiElement> aliasDefinitions = new HashSet<BasePsiElement>();
        BasePsiElement scope = aliasDefinition.getEnclosingScopePsiElement();
        scope.collectAliasDefinitionPsiElements(aliasDefinitions, aliasDefinition.getUnquotedText(), DBObjectType.ANY);
        if (aliasDefinitions.size() > 1) {
            holder.createWarningAnnotation(aliasDefinition, "Duplicate alias definition: " + aliasDefinition.getUnquotedText());
        }*/
        Annotation annotation = holder.createInfoAnnotation(aliasDefinition, null);
        annotation.setTextAttributes(SQLTextAttributesKeys.ALIAS);
    }

    private static void annotateObject(@NotNull IdentifierPsiElement objectReference, AnnotationHolder holder) {
        if (!objectReference.isResolving() && !objectReference.isDefinition()) {
            PsiElement reference = objectReference.resolve();
            if (reference == null && objectReference.getResolveAttempts() > 3 && checkConnection(objectReference)) {
                if (!objectReference.getLanguageDialect().getParserTokenTypes().isFunction(objectReference.getText())) {
                    Annotation annotation = holder.createWarningAnnotation(objectReference.node,
                            "Unknown identifier");
                    annotation.setTextAttributes(SQLTextAttributesKeys.UNKNOWN_IDENTIFIER);
                }
            }
        }
    }

    private static boolean checkConnection(@NotNull IdentifierPsiElement objectReference) {
        ConnectionHandler connection = objectReference.getConnection();
        return connection != null &&
                !connection.isVirtual() &&
                connection.canConnect() &&
                connection.isValid() &&
                !connection.getConnectionStatus().is(ConnectionHandlerStatus.LOADING);
    }

    private static void annotateExecutable(@NotNull ExecutablePsiElement executablePsiElement, AnnotationHolder holder) {
        if (executablePsiElement.isInjectedContext()) return;

        if (executablePsiElement.isValid() && !executablePsiElement.isNestedExecutable()) {
            DBLanguagePsiFile psiFile = executablePsiElement.getFile();
            VirtualFile file = psiFile.getVirtualFile();
            if (!isDebugConsole(file) && hasHasConnectivityContext(file)) {
                Annotation annotation = holder.createInfoAnnotation(executablePsiElement, null);
                annotation.setGutterIconRenderer(new StatementGutterRenderer(executablePsiElement));
            }
        }
    }

    private static void annotateChameleon(PsiElement psiElement, AnnotationHolder holder) {
        ChameleonPsiElement executable = (ChameleonPsiElement) psiElement;
/*
        if (!executable.isNestedExecutable()) {
            StatementExecutionProcessor executionProcessor = executable.getExecutionProcessor();
            if (executionProcessor != null) {
                Annotation annotation = holder.createInfoAnnotation(psiElement, null);
                annotation.setGutterIconRenderer(new StatementGutterRenderer(executionProcessor));
            }
        }
*/
    }
}