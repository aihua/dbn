package com.dci.intellij.dbn.debugger.common.config;

import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class DBRunConfigFactory<T extends DBRunConfigType, C extends DBRunConfig> extends ConfigurationFactory {
    private final DBDebuggerType debuggerType;
    protected DBRunConfigFactory(T type, DBDebuggerType debuggerType) {
        super(type);
        this.debuggerType = debuggerType;
    }

    @NotNull
    @Override
    public RunConfiguration createConfiguration(String name, @NotNull RunConfiguration template) {
        RunConfiguration configuration = super.createConfiguration(name, template);
        if (template instanceof DBRunConfig) {
            DBRunConfig templateConfig = (DBRunConfig) template;
            DBRunConfigCategory category = templateConfig.getCategory();
            if (category == DBRunConfigCategory.TEMPLATE) {
                ((DBRunConfig)configuration).setCategory(DBRunConfigCategory.CUSTOM);
            }
        }
        return configuration;
    }

    @NotNull
    @Override
    public T getType() {
        return (T) super.getType();
    }

    public abstract C createConfiguration(Project project, String name, DBRunConfigCategory category);

    public DBDebuggerType getDebuggerType(){
        return debuggerType;
    }

    @NotNull
    @Override
    public String getId() {
        return getName();
    }
}
