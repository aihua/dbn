package com.dci.intellij.dbn.code.common.lookup;

import com.dci.intellij.dbn.code.common.completion.BasicInsertHandler;
import com.dci.intellij.dbn.code.common.completion.CodeCompletionContext;
import com.dci.intellij.dbn.code.common.completion.options.sorting.CodeCompletionSortingSettings;
import com.dci.intellij.dbn.common.util.Naming;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupItem;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Objects;


public class CodeCompletionLookupItem extends LookupItem {
    public CodeCompletionLookupItem(LookupItemBuilder lookupItemBuilder, @NotNull String text, CodeCompletionContext completionContext) {
        super(lookupItemBuilder, Naming.unquote(text));
        setIcon(lookupItemBuilder.getIcon());
        if (lookupItemBuilder.isBold()) setBold();
        setAttribute(LookupItem.TYPE_TEXT_ATTR, lookupItemBuilder.getTextHint());
        addLookupStrings(text.toUpperCase(), text.toLowerCase());
        setPresentableText(Naming.unquote(text));
        CodeCompletionSortingSettings sortingSettings = completionContext.getCodeCompletionSettings().getSortingSettings();
        if (sortingSettings.isEnabled()) {
            setPriority(sortingSettings.getSortingIndexFor(lookupItemBuilder));
        }
    }

    public CodeCompletionLookupItem(Object source, Icon icon, @NotNull String text, String description, boolean bold, double sortPriority) {
        this(source, icon, text, description, bold);
        setPriority(sortPriority);
    }


    public CodeCompletionLookupItem(Object source, Icon icon, @NotNull String text, String description, boolean bold) {
        super(source, text);
        addLookupStrings(text.toUpperCase(), text.toLowerCase());
        setIcon(icon);
        if (bold) setBold();
        setAttribute(LookupItem.TYPE_TEXT_ATTR, description);
        setPresentableText(Naming.unquote(text));
        setInsertHandler(BasicInsertHandler.INSTANCE);
    }

    @NotNull
    @Override
    public Object getObject() {
        return super.getObject();
    }

    @Override
    public InsertHandler getInsertHandler() {
        return super.getInsertHandler();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CodeCompletionLookupItem) {
            CodeCompletionLookupItem lookupItem = (CodeCompletionLookupItem) o;
            return Objects.equals(lookupItem.getLookupString(), getLookupString());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return getLookupString().hashCode();
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
