package com.dci.intellij.dbn.execution.compiler.options;

import com.dci.intellij.dbn.common.options.BasicConfiguration;
import com.dci.intellij.dbn.common.options.setting.Settings;
import com.dci.intellij.dbn.connection.operation.options.OperationSettings;
import com.dci.intellij.dbn.execution.compiler.CompileDependenciesOption;
import com.dci.intellij.dbn.execution.compiler.CompileType;
import com.dci.intellij.dbn.execution.compiler.options.ui.CompilerSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CompilerSettings extends BasicConfiguration<OperationSettings, CompilerSettingsForm> {
    private CompileType compileType = CompileType.KEEP;
    private CompileDependenciesOption compileDependenciesOption = CompileDependenciesOption.ASK;
    private boolean alwaysShowCompilerControls = false;

    public CompilerSettings(OperationSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return "Data editor general settings";
    }

    @Override
    public String getHelpTopic() {
        return "executionEngine";
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    @NotNull
    public CompilerSettingsForm createConfigurationEditor() {
        return new CompilerSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "compiler";
    }

    @Override
    public void readConfiguration(Element element) {
        compileType = CompileType.get(Settings.getString(element, "compile-type", compileType.name()));
        compileDependenciesOption = CompileDependenciesOption.get(Settings.getString(element, "compile-dependencies", compileDependenciesOption.name()));
        alwaysShowCompilerControls = Settings.getBoolean(element, "always-show-controls", alwaysShowCompilerControls);
    }

    @Override
    public void writeConfiguration(Element element) {
        Settings.setString(element, "compile-type", compileType.name());
        Settings.setString(element, "compile-dependencies", compileDependenciesOption.name());
        Settings.setBoolean(element, "always-show-controls", alwaysShowCompilerControls);
    }
}
