package com.dci.intellij.dbn.common.options;

import com.dci.intellij.dbn.common.action.Lookups;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.common.project.ProjectSupplier;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dci.intellij.dbn.common.dispose.Checks.isValid;
import static com.dci.intellij.dbn.common.options.setting.Settings.newElement;

@Slf4j
public abstract class AbstractConfiguration<P extends Configuration, E extends ConfigurationEditorForm> implements Configuration<P, E> {

    /*****************************************************************
     *                         DOM utilities                         *
     ****************************************************************/
    protected void writeConfiguration(Element element, Configuration configuration) {
        String elementName = configuration.getConfigElementName();
        if (elementName == null) return;

        Element childElement = newElement(element, elementName);
        configuration.writeConfiguration(childElement);
    }


    protected void readConfiguration(Element element, Configuration configuration) {
        if (element == null) return;

        String elementName = configuration.getConfigElementName();
        if (elementName == null) return;

        Element childElement = element.getChild(elementName);
        if (childElement == null) return;

        configuration.readConfiguration(childElement);
    }

    @NotNull
    public E createConfigurationEditor() {
        throw new UnsupportedOperationException();
    };


    @Override
    public final Project resolveProject() {
        if (this instanceof ProjectSupplier) {
            ProjectSupplier projectSupplier = (ProjectSupplier) this;
            Project project = projectSupplier.getProject();
            if (project != null) {
                return project;
            }
        }

        Configuration parent = this.getParent();
        if (parent != null) {
            Project project = parent.resolveProject();
            if (project != null) {
                return project;
            }
        }

        ConfigurationEditorForm settingsEditor = this.getSettingsEditor();
        if (isValid(settingsEditor)) {
            return Lookups.getProject(settingsEditor.getComponent());
        }
        return null;
    }

}
