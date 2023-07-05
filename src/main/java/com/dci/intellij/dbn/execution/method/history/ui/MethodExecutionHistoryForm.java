package com.dci.intellij.dbn.execution.method.history.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.Lookups;
import com.dci.intellij.dbn.common.action.ProjectAction;
import com.dci.intellij.dbn.common.action.ToggleAction;
import com.dci.intellij.dbn.common.dispose.DisposableContainers;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.util.UserInterface;
import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.dci.intellij.dbn.execution.method.MethodExecutionInput;
import com.dci.intellij.dbn.execution.method.MethodExecutionManager;
import com.dci.intellij.dbn.execution.method.ui.MethodExecutionHistory;
import com.dci.intellij.dbn.execution.method.ui.MethodExecutionInputForm;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.options.ConfigId;
import com.dci.intellij.dbn.options.ProjectSettingsManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.util.List;
import java.util.Map;

import static com.dci.intellij.dbn.common.ui.util.Splitters.makeRegular;

public class MethodExecutionHistoryForm extends DBNFormBase {
    private JPanel mainPanel;
    private JTree executionInputsTree;
    private JPanel actionsPanel;
    private JPanel argumentsPanel;
    private JPanel contentPanel;
    private JSplitPane contentSplitPane;
    private ChangeListener changeListener;
    private final boolean debug;

    private final Map<DBObjectRef<DBMethod>, MethodExecutionInputForm> methodExecutionForms = DisposableContainers.map(this);

    MethodExecutionHistoryForm(MethodExecutionHistoryDialog parent, MethodExecutionInput selectedExecutionInput, boolean debug) {
        super(parent);
        this.debug = debug;
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel,"", true,
                new ShowGroupedTreeAction(),
                new DeleteHistoryEntryAction(),
                Actions.SEPARATOR,
                new ProjectSettingsOpenAction());
        actionsPanel.add(actionToolbar.getComponent());
        makeRegular(contentSplitPane);

        MethodExecutionHistory executionHistory = getExecutionHistory();
        if (selectedExecutionInput != null &&
                !selectedExecutionInput.isObsolete() &&
                !selectedExecutionInput.isInactive() &&
                (!debug || DatabaseFeature.DEBUGGING.isSupported(selectedExecutionInput))) {
            showMethodExecutionPanel(selectedExecutionInput);
            setSelectedInput(selectedExecutionInput);
        }
        List<MethodExecutionInput> executionInputs = executionHistory.getExecutionInputs();
        getTree().init(executionInputs, executionHistory.isGroupEntries());
        executionInputsTree.getSelectionModel().addTreeSelectionListener(treeSelectionListener);
    }

    private MethodExecutionHistory getExecutionHistory() {
        return MethodExecutionManager.getInstance(ensureProject()).getExecutionHistory();
    }

    @NotNull
    MethodExecutionHistoryDialog getParentDialog() {
        return ensureParentComponent();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    List<MethodExecutionInput> getExecutionInputs() {
        MethodExecutionHistoryTreeModel model = (MethodExecutionHistoryTreeModel) executionInputsTree.getModel();
        return model.getExecutionInputs();
    }

    private void createUIComponents() {
        MethodExecutionHistory executionHistory = getExecutionHistory();
        boolean group = executionHistory.isGroupEntries();
        executionInputsTree = new MethodExecutionHistoryTree(this, group, debug);
    }

    public MethodExecutionHistoryTree getTree() {
        return (MethodExecutionHistoryTree) executionInputsTree;
    }

    private void showMethodExecutionPanel(MethodExecutionInput executionInput) {
        argumentsPanel.removeAll();
        if (executionInput != null &&
                !executionInput.isObsolete() &&
                !executionInput.isInactive()) {
            DBObjectRef<DBMethod> methodRef = executionInput.getMethodRef();
            MethodExecutionInputForm methodExecutionInputForm = methodExecutionForms.get(methodRef);
            if (methodExecutionInputForm == null) {
                methodExecutionInputForm = new MethodExecutionInputForm(this, executionInput, true, DBDebuggerType.NONE);
                methodExecutionInputForm.addChangeListener(getChangeListener());
                methodExecutionForms.put(methodRef, methodExecutionInputForm);
            }
            argumentsPanel.add(methodExecutionInputForm.getComponent(), BorderLayout.CENTER);
        }

        UserInterface.repaint(argumentsPanel);
    }

    private ChangeListener getChangeListener() {
        if (changeListener == null) {
            changeListener = e -> getParentDialog().setSaveButtonEnabled(true);
        }
        return changeListener;
    }

    void updateMethodExecutionInputs() {
        for (MethodExecutionInputForm methodExecutionComponent : methodExecutionForms.values()) {
            methodExecutionComponent.updateExecutionInput();
        }
    }

    void setSelectedInput(MethodExecutionInput selectedExecutionInput) {
        getTree().setSelectedInput(selectedExecutionInput);
    }

    public class DeleteHistoryEntryAction extends ProjectAction {
        DeleteHistoryEntryAction() {
            super("Delete", null, Icons.ACTION_REMOVE);
        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
            getTree().removeSelectedEntries();
        }

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(!getTree().isSelectionEmpty());
            presentation.setVisible(getParentDialog().isEditable());
        }
    }

    public static class ProjectSettingsOpenAction extends ProjectAction {
        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
            ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(project);
            settingsManager.openProjectSettings(ConfigId.EXECUTION_ENGINE);
        }

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
            Presentation presentation = e.getPresentation();
            presentation.setIcon(Icons.ACTION_OPTIONS);
            presentation.setText("Settings");
        }
    }

    public class ShowGroupedTreeAction extends ToggleAction {
        ShowGroupedTreeAction() {
            super("Group by Program", "Show grouped by program", Icons.ACTION_GROUP);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return getTree().isGrouped();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            getTemplatePresentation().setText(state ? "Ungroup" : "Group by Program");
            MethodExecutionHistoryTree historyTree = getTree();
            List<MethodExecutionInput> executionInputs = historyTree.getModel().getExecutionInputs();
            historyTree.init(executionInputs, state);
            Project project = Lookups.getProject(e);
            if (project != null) {
                MethodExecutionManager executionManager = MethodExecutionManager.getInstance(project);
                executionManager.getExecutionHistory().setGroupEntries(state);
            }
        }
    }


    private final TreeSelectionListener treeSelectionListener = e -> {
        MethodExecutionInput executionInput = getTree().getSelectedExecutionInput();
        if (executionInput != null) {
            ConnectionAction.invoke("loading the execution history", true, executionInput,
                    action -> Progress.prompt(getProject(), action, false,
                            "Loading method details",
                            "Loading details of " + executionInput.getMethodRef().getQualifiedNameWithType(),
                            progress -> {
                                DBMethod method = executionInput.getMethod();
                                if (method != null) {
                                    method.getArguments();
                                }

                                Dispatch.run(() -> {
                                    MethodExecutionHistoryDialog dialog = getParentDialog();
                                    showMethodExecutionPanel(executionInput);
                                    dialog.setSelectedExecutionInput(executionInput);
                                    dialog.updateMainButtons(executionInput);
                                    if (method != null) {
                                        MethodExecutionHistory executionHistory = getExecutionHistory();
                                        executionHistory.setSelection(executionInput.getMethodRef());
                                    }
                                });
                            }));
        } else {
            MethodExecutionHistoryDialog dialog = getParentDialog();
            dialog.updateMainButtons(null);
        }
    };
}
