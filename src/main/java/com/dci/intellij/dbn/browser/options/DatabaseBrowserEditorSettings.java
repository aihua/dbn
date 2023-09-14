package com.dci.intellij.dbn.browser.options;

import com.dci.intellij.dbn.browser.options.ui.DatabaseBrowserEditorSettingsForm;
import com.dci.intellij.dbn.common.options.BasicProjectConfiguration;
import com.dci.intellij.dbn.object.common.editor.DefaultEditorOption;
import com.dci.intellij.dbn.object.common.editor.DefaultEditorType;
import com.dci.intellij.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.dci.intellij.dbn.common.options.setting.Settings.newElement;
import static com.dci.intellij.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DatabaseBrowserEditorSettings extends BasicProjectConfiguration<DatabaseBrowserSettings, DatabaseBrowserEditorSettingsForm> {

    private List<DefaultEditorOption> options = new ArrayList<>();

    public DatabaseBrowserEditorSettings(DatabaseBrowserSettings parent) {
        super(parent);
        options.add(new DefaultEditorOption(DBObjectType.VIEW, DefaultEditorType.SELECTION));
        options.add(new DefaultEditorOption(DBObjectType.PACKAGE, DefaultEditorType.SELECTION));
        options.add(new DefaultEditorOption(DBObjectType.TYPE, DefaultEditorType.SELECTION));
    }

    @NotNull
    @Override
    public DatabaseBrowserEditorSettingsForm createConfigurationEditor() {
        return new DatabaseBrowserEditorSettingsForm(this);
    }

    /*********************************************************
     *                        Custom                         *
     *********************************************************/
    public DefaultEditorOption getOption(DBObjectType objectType) {
        for (DefaultEditorOption option : options) {
            if (option.getObjectType().matches(objectType)) {
                return option;
            }
        }
        return null;
    }

    private static boolean contains(List<DefaultEditorOption> options, DBObjectType objectType) {
        for (DefaultEditorOption option : options) {
            if (option.getObjectType() == objectType){
                return true;
            }
        }
        return false;
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @Override
    public String getConfigElementName() {
        return "default-editors";
    }

    @Override
    public String getDisplayName() {
        return "Database Browser";
    }

    @Override
    public String getHelpTopic() {
        return "browserSettings";
    }

    @Override
    public void readConfiguration(Element element) {
        List<DefaultEditorOption> newOptions = new ArrayList<>();
        List<Element> children = element.getChildren();
        for (Element child : children) {
            String objectTypeName = stringAttribute(child, "name");
            String editorTypeName = stringAttribute(child, "editor-type");
            DBObjectType objectType = DBObjectType.get(objectTypeName);
            DefaultEditorType editorType = DefaultEditorType.valueOf(editorTypeName);

            DefaultEditorOption comparator = new DefaultEditorOption(objectType, editorType);
            newOptions.add(comparator);
        }
        for (DefaultEditorOption option : options) {
            if (!contains(newOptions, option.getObjectType())) {
                newOptions.add(option);
            }
        }
        options = newOptions;
    }

    @Override
    public void writeConfiguration(Element element) {
        for (DefaultEditorOption option : options) {
            Element child = newElement(element, "object-type");
            child.setAttribute("name", option.getObjectType().getName().toUpperCase());
            child.setAttribute("editor-type", option.getEditorType().name());
        }
    }
}
