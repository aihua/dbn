package com.dci.intellij.dbn.debugger.jdwp.config;

import com.dci.intellij.dbn.common.options.AbstractConfiguration;
import com.dci.intellij.dbn.common.options.BasicConfiguration;
import com.dci.intellij.dbn.connection.config.ConnectionDebuggerSettings;
import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.debugger.common.config.DBMethodRunConfig;
import com.dci.intellij.dbn.debugger.common.config.DBRunConfigCategory;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class DBMethodJdwpRunConfig extends DBMethodRunConfig implements DBJdwpRunConfig {
    @Delegate(excludes = {BasicConfiguration.class, AbstractConfiguration.class})
    private ConnectionDebuggerSettings debuggerConfig = new ConnectionDebuggerSettings();

    public DBMethodJdwpRunConfig(Project project, DBMethodJdwpRunConfigFactory factory, String name, DBRunConfigCategory category) {
        super(project, factory, name, category);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new DBMethodJdwpRunConfigEditor(this);
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        return new DBMethodJdwpRunProfileState(env);
    }

    @Override
    public boolean canRun() {
        return super.canRun() && DBDebuggerType.JDWP.isSupported();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        DatabaseDebuggerManager.checkJdwpConfiguration();
        super.checkConfiguration();
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        debuggerConfig.readConfiguration(element);
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        debuggerConfig.writeConfiguration(element);
    }
}
