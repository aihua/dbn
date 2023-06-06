package com.dci.intellij.dbn.debugger.jdwp.config.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.GroupPopupAction;
import com.dci.intellij.dbn.common.action.ProjectAction;
import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.ui.form.DBNHeaderForm;
import com.dci.intellij.dbn.common.ui.form.DBNHintForm;
import com.dci.intellij.dbn.common.ui.util.UserInterface;
import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.dci.intellij.dbn.debugger.ExecutionConfigManager;
import com.dci.intellij.dbn.debugger.common.config.DBRunConfigCategory;
import com.dci.intellij.dbn.debugger.common.config.ui.DBProgramRunConfigurationEditorForm;
import com.dci.intellij.dbn.debugger.jdwp.config.DBMethodJdwpRunConfig;
import com.dci.intellij.dbn.execution.method.MethodExecutionInput;
import com.dci.intellij.dbn.execution.method.MethodExecutionManager;
import com.dci.intellij.dbn.execution.method.ui.MethodExecutionInputForm;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.*;
import java.awt.*;

import static com.dci.intellij.dbn.common.dispose.Disposer.replace;

public class DBMethodJdwpRunConfigEditorForm extends DBProgramRunConfigurationEditorForm<DBMethodJdwpRunConfig>{
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JPanel methodArgumentsPanel;
    private JPanel selectMethodActionPanel;
    private JPanel hintPanel;
    private JPanel configPanel;
    private MethodExecutionInputForm inputForm;
    private final DBJdwpDebugAttributesForm configForm = new DBJdwpDebugAttributesForm(this);

    public DBMethodJdwpRunConfigEditorForm(DBMethodJdwpRunConfig configuration) {
        super(configuration.getProject());
        readConfiguration(configuration);
        if (configuration.getCategory() != DBRunConfigCategory.CUSTOM) {
            selectMethodActionPanel.setVisible(false);
            methodArgumentsPanel.setVisible(false);
            headerPanel.setVisible(false);
            hintPanel.setVisible(true);
            DBNHintForm hintForm = new DBNHintForm(this, ExecutionConfigManager.GENERIC_METHOD_RUNNER_HINT, null, true);
            hintPanel.add(hintForm.getComponent());
        } else {
            ActionToolbar actionToolbar = Actions.createActionToolbar(selectMethodActionPanel,"", true, new SelectMethodAction());
            selectMethodActionPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
            hintPanel.setVisible(false);
        }

        configPanel.add(configForm.getMainPanel());
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public class SelectMethodAction extends GroupPopupAction {
        SelectMethodAction()  {
            super("Select method", "Select method", Icons.DBO_METHOD);
        }

        @Override
        protected AnAction[] getActions(AnActionEvent e) {
            return new AnAction[]{
                    new MethodHistoryOpenAction(),
                    new MethodBrowserOpenAction()
            };
        }
    }

    public class MethodBrowserOpenAction extends ProjectAction {
        MethodBrowserOpenAction() {
            super("Method Browser");
        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
            MethodExecutionManager executionManager = MethodExecutionManager.getInstance(project);
            executionManager.promptMethodBrowserDialog(getExecutionInput(), true,
                    (executionInput) -> setExecutionInput(executionInput, true));
        }
    }
    public class MethodHistoryOpenAction extends ProjectAction {
        MethodHistoryOpenAction() {
            super("Execution History", null, Icons.METHOD_EXECUTION_HISTORY);
        }


        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
            MethodExecutionManager methodExecutionManager = MethodExecutionManager.getInstance(project);
            methodExecutionManager.showExecutionHistoryDialog(getExecutionInput(), false, true,
                    (executionInput) -> setExecutionInput(executionInput, true));
        }
    }

    @Nullable
    public MethodExecutionInput getExecutionInput() {
        return inputForm == null ? null : inputForm.getExecutionInput();
    }

    @Override
    public void writeConfiguration(DBMethodJdwpRunConfig configuration) throws ConfigurationException {
        if (inputForm != null) {
            inputForm.updateExecutionInput();
            configuration.setExecutionInput(getExecutionInput());
        }
        configForm.writeConfiguration(configuration);
    }

    @Override
    public void readConfiguration(DBMethodJdwpRunConfig configuration) {
        setExecutionInput(configuration.getExecutionInput(), false);
        configForm.readConfiguration(configuration);
    }

    public void setExecutionInput(@Nullable MethodExecutionInput executionInput, boolean touchForm) {
        Progress.modal(getProject(), executionInput, false,
                "Loading data dictionary",
                "Loading method information",
                progress -> {
                    // initialize method and arguments
                    if (executionInput != null) {
                        DBMethod method = executionInput.getMethod();
                        if (method != null) {
                            method.getArguments();
                        }
                    }

                    Dispatch.run(() -> {
                        methodArgumentsPanel.removeAll();
                        inputForm = replace(inputForm, null);

                        String headerTitle = "No method selected";
                        Icon headerIcon = null;
                        Color headerBackground = Colors.getPanelBackground();

                        if (executionInput != null) {
                            DBObjectRef<DBMethod> methodRef = executionInput.getMethodRef();
                            headerTitle = methodRef.getPath();
                            headerIcon = methodRef.getObjectType().getIcon();
                            DBMethod method = executionInput.getMethod();
                            if (method != null) {
                                inputForm = new MethodExecutionInputForm(this, executionInput, false, DBDebuggerType.JDWP);
                                methodArgumentsPanel.add(inputForm.getComponent(), BorderLayout.CENTER);
                                if (touchForm) inputForm.touch();
                                headerIcon = method.getOriginalIcon();
                                if (getEnvironmentSettings(method.getProject()).getVisibilitySettings().getDialogHeaders().value()) {
                                    headerBackground = method.getEnvironmentType().getColor();
                                }
                            }
                        }

                        DBNHeaderForm headerForm = new DBNHeaderForm(
                                this, headerTitle,
                                headerIcon,
                                headerBackground
                        );
                        headerPanel.removeAll();
                        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

                        UserInterface.repaint(mainPanel);
                    });
                });
    }
}
