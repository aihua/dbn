package com.dci.intellij.dbn.code.common.completion.options.filter;

import com.dci.intellij.dbn.code.common.completion.options.CodeCompletionSettings;
import com.dci.intellij.dbn.code.common.completion.options.filter.ui.CodeCompletionFiltersSettingsForm;
import com.dci.intellij.dbn.common.options.CompositeConfiguration;
import com.dci.intellij.dbn.common.options.Configuration;
import com.dci.intellij.dbn.language.common.TokenTypeCategory;
import com.dci.intellij.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@EqualsAndHashCode(callSuper = false)
public class CodeCompletionFiltersSettings extends CompositeConfiguration<CodeCompletionSettings, CodeCompletionFiltersSettingsForm> {
    private final CodeCompletionFilterSettings basicFilterSettings = new CodeCompletionFilterSettings(this, false);
    private final CodeCompletionFilterSettings extendedFilterSettings = new CodeCompletionFilterSettings(this, true);;

    public CodeCompletionFiltersSettings(CodeCompletionSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return "Code completion filter";
    }

   /*********************************************************
    *                         Custom                        *
    *********************************************************/
    public CodeCompletionFilterSettings getFilterSettings(boolean extended) {
        return extended ? extendedFilterSettings : basicFilterSettings;
    }

    boolean acceptRootObjects(boolean extended, DBObjectType objectType) {
        return getFilterSettings(extended).acceptsRootObject(objectType);
    }

    boolean showReservedWords(boolean extended, TokenTypeCategory tokenTypeCategory) {
        return getFilterSettings(extended).acceptReservedWord(tokenTypeCategory);
    }

    boolean showUserSchemaObjects(boolean extended, DBObjectType objectType) {
        return getFilterSettings(extended).acceptsCurrentSchemaObject(objectType);
    }

    boolean acceptPublicSchemaObjects(boolean extended, DBObjectType objectType) {
        return getFilterSettings(extended).acceptsPublicSchemaObject(objectType);
    }

    boolean acceptAnySchemaObjects(boolean extended, DBObjectType objectType) {
        return getFilterSettings(extended).acceptsAnySchemaObject(objectType);
    }

    /*********************************************************
    *                   Configuration                       *
    *********************************************************/
    @Override
    @NotNull
    public CodeCompletionFiltersSettingsForm createConfigurationEditor() {
        return new CodeCompletionFiltersSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "filters";
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                basicFilterSettings,
                extendedFilterSettings};
    }
}