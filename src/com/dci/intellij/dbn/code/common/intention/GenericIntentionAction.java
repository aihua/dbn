package com.dci.intellij.dbn.code.common.intention;

import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class GenericIntentionAction implements IntentionAction, PriorityAction, Iconable, DumbAware, Comparable {

    @Override
    @NotNull
    public String getFamilyName() {
        return getText();
    }

    @Nullable
    protected ConnectionHandler getConnectionHandler(PsiFile psiFile) {
        if (psiFile instanceof DBLanguagePsiFile) {
            DBLanguagePsiFile dbLanguagePsiFile = (DBLanguagePsiFile) psiFile;
            return dbLanguagePsiFile.getConnectionHandler();
        }
        return null;
    }

    protected Integer getGroupPriority() {
        return 0;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof GenericIntentionAction) {
            GenericIntentionAction a = (GenericIntentionAction) o;
            int groupLevel = getPriority().compareTo(a.getPriority());

            return groupLevel == 0 ? getGroupPriority().compareTo(a.getGroupPriority()) : groupLevel;
        }
        return 0;
    }
}
