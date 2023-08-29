package com.dci.intellij.dbn.execution.statement.options;

import com.dci.intellij.dbn.common.options.BasicProjectConfiguration;
import com.dci.intellij.dbn.common.options.setting.Settings;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.common.project.ProjectSupplier;
import com.dci.intellij.dbn.execution.common.options.ExecutionEngineSettings;
import com.dci.intellij.dbn.execution.common.options.ExecutionTimeoutSettings;
import com.dci.intellij.dbn.execution.statement.options.ui.StatementExecutionSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class StatementExecutionSettings extends BasicProjectConfiguration<ExecutionEngineSettings, ConfigurationEditorForm> implements ExecutionTimeoutSettings, ProjectSupplier {
    private static final String REMEMBER_OPTION_HINT = ""/*"\n\n(you can remember your option and change it at any time in Settings > Execution Engine > Statement Execution)"*/;

    private int resultSetFetchBlockSize = 100;
    private int executionTimeout = 20;
    private int debugExecutionTimeout = 600;
    private boolean focusResult = false;
    private boolean promptExecution = false;

    public StatementExecutionSettings(ExecutionEngineSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return "Statement execution settings";
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
    public ConfigurationEditorForm createConfigurationEditor() {
        return new StatementExecutionSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "statement-execution";
    }

    @Override
    public void readConfiguration(Element element) {
        resultSetFetchBlockSize = Settings.getInteger(element, "fetch-block-size", resultSetFetchBlockSize);
        executionTimeout = Settings.getInteger(element, "execution-timeout", executionTimeout);
        debugExecutionTimeout = Settings.getInteger(element, "debug-execution-timeout", debugExecutionTimeout);
        focusResult = Settings.getBoolean(element, "focus-result", focusResult);
        promptExecution = Settings.getBoolean(element, "prompt-execution", promptExecution);
    }

    @Override
    public void writeConfiguration(Element element) {
        Settings.setInteger(element, "fetch-block-size", resultSetFetchBlockSize);
        Settings.setInteger(element, "execution-timeout", executionTimeout);
        Settings.setInteger(element, "debug-execution-timeout", debugExecutionTimeout);
        Settings.setBoolean(element, "focus-result", focusResult);
        Settings.setBoolean(element, "prompt-execution", promptExecution);
    }
}
