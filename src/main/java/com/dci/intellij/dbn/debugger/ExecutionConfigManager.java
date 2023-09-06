package com.dci.intellij.dbn.debugger;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.component.PersistentState;
import com.dci.intellij.dbn.common.component.ProjectComponentBase;
import com.dci.intellij.dbn.common.text.TextContent;
import com.dci.intellij.dbn.common.util.Lists;
import com.dci.intellij.dbn.common.util.Naming;
import com.dci.intellij.dbn.debugger.common.config.*;
import com.dci.intellij.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dci.intellij.dbn.object.DBMethod;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static com.dci.intellij.dbn.common.component.Components.projectService;
import static com.dci.intellij.dbn.common.text.TextContent.plain;
import static com.dci.intellij.dbn.debugger.ExecutionConfigManager.COMPONENT_NAME;

@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ExecutionConfigManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ExecutionConfigManager";


    public static final TextContent GENERIC_METHOD_RUNNER_HINT =
            plain("This is the generic Database Method debug runner. " +
                    "This is used when debugging is invoked on a given method. " +
                    "No specific method information can be specified here.");

    public static final TextContent GENERIC_STATEMENT_RUNNER_HINT =
            plain("This is the generic Database Statement debug runner. " +
                    "This is used when debugging is invoked on a given SQL statement. " +
                    "No specific statement information can be specified here.");


    private ExecutionConfigManager(Project project) {
        super(project, COMPONENT_NAME);
    }


    public static ExecutionConfigManager getInstance(@NotNull Project project) {
        return projectService(project, ExecutionConfigManager.class);
    }

    public DBMethodRunConfigType getMethodConfigurationType() {
        ConfigurationType[] configurationTypes = Extensions.getExtensions(ConfigurationType.CONFIGURATION_TYPE_EP);
        return ContainerUtil.findInstance(configurationTypes, DBMethodRunConfigType.class);
    }

    public DBStatementRunConfigType getStatementConfigurationType() {
        ConfigurationType[] configurationTypes = Extensions.getExtensions(ConfigurationType.CONFIGURATION_TYPE_EP);
        return ContainerUtil.findInstance(configurationTypes, DBStatementRunConfigType.class);
    }

    public String createMethodConfigurationName(DBMethod method) {
        DBMethodRunConfigType configurationType = getMethodConfigurationType();
        List<RunnerAndConfigurationSettings> configurationSettings = getRunManager().getConfigurationSettingsList(configurationType);

        String name = method.getName();
        while (nameExists(configurationSettings, name)) {
            name = Naming.nextNumberedIdentifier(name, true);
        }
        return name;
    }

    private static boolean nameExists(List<RunnerAndConfigurationSettings> configurationSettings, String name) {
        return Lists.anyMatch(configurationSettings, configurationSetting -> Objects.equals(configurationSetting.getName(), name));
    }

    @NotNull
    public RunnerAndConfigurationSettings createConfiguration(@NotNull DBMethod method, DBDebuggerType debuggerType) {
        DBMethodRunConfigType configType = getMethodConfigurationType();
        DBMethodRunConfigFactory configFactory = configType.getConfigurationFactory(debuggerType);
        DBMethodRunConfig config = configFactory.createConfiguration(method);

        return getRunManager().createConfiguration(config, configFactory);
    }

    public RunnerAndConfigurationSettings createConfiguration(@NotNull StatementExecutionProcessor executionProcessor, DBDebuggerType debuggerType) {
        DBStatementRunConfigType configType = getStatementConfigurationType();
        DBStatementRunConfigFactory configFactory = configType.getConfigurationFactory(debuggerType);
        DBStatementRunConfig config = configFactory.createConfiguration(executionProcessor);

        return getRunManager().createConfiguration(config, configFactory);
    }

    @NotNull
    private RunManager getRunManager() {
        return RunManagerEx.getInstance(ensureProject());
    }

    @Deprecated // TODO move to stateless run configuration (decommission after a few releases)
    public void removeRunConfigurations() {
        RunManager runManager = getRunManager();
        List<RunnerAndConfigurationSettings> runConfigurations = runManager.getAllSettings();
        for (RunnerAndConfigurationSettings runConfiguration : runConfigurations) {
            RunConfiguration configuration = runConfiguration.getConfiguration();
            if (configuration instanceof DBRunConfig) {
                runManager.removeConfiguration(runConfiguration);
            }
        }
    }

    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element state) {

    }
}
