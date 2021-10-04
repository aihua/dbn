package com.dci.intellij.dbn.editor.code.options;

import com.dci.intellij.dbn.common.options.BasicConfiguration;
import com.dci.intellij.dbn.common.project.ProjectSupplier;
import com.dci.intellij.dbn.editor.code.options.ui.CodeEditorGeneralSettingsForm;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.getBoolean;
import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.setBoolean;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CodeEditorGeneralSettings
        extends BasicConfiguration<CodeEditorSettings, CodeEditorGeneralSettingsForm>
        implements ProjectSupplier {

    private boolean showObjectsNavigationGutter = false;
    private boolean showSpecDeclarationNavigationGutter = true;
    private boolean enableSpellchecking = true;
    private boolean enableReferenceSpellchecking = false;

    CodeEditorGeneralSettings(CodeEditorSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return "Code editor general settings";
    }

    @Override
    public String getHelpTopic() {
        return "codeEditor";
    }

    @NotNull
    public Project getProject() {
        return getParent().getProject();
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    @NotNull
    public CodeEditorGeneralSettingsForm createConfigurationEditor() {
        return new CodeEditorGeneralSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "general";
    }

    @Override
    public void readConfiguration(Element element) {
        showObjectsNavigationGutter = getBoolean(element, "show-object-navigation-gutter", showObjectsNavigationGutter);
        showSpecDeclarationNavigationGutter = getBoolean(element, "show-spec-declaration-navigation-gutter", showSpecDeclarationNavigationGutter);
        enableSpellchecking = getBoolean(element, "enable-spellchecking", enableSpellchecking);
        enableReferenceSpellchecking = getBoolean(element, "enable-reference-spellchecking", enableReferenceSpellchecking);
    }

    @Override
    public void writeConfiguration(Element element) {
        setBoolean(element, "show-object-navigation-gutter", showObjectsNavigationGutter);
        setBoolean(element, "show-spec-declaration-navigation-gutter", showSpecDeclarationNavigationGutter);
        setBoolean(element, "enable-spellchecking", enableSpellchecking);
        setBoolean(element, "enable-reference-spellchecking", enableReferenceSpellchecking);
    }
}
