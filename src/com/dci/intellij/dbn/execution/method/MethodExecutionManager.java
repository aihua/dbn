package com.dci.intellij.dbn.execution.method;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.routine.ParametricRunnable;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.common.util.MessageUtil;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.DatabaseExecutionInterface;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.database.common.execution.MethodExecutionProcessor;
import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.dci.intellij.dbn.execution.ExecutionContext;
import com.dci.intellij.dbn.execution.ExecutionManager;
import com.dci.intellij.dbn.execution.method.browser.MethodBrowserSettings;
import com.dci.intellij.dbn.execution.method.browser.ui.MethodExecutionBrowserDialog;
import com.dci.intellij.dbn.execution.method.history.ui.MethodExecutionHistoryDialog;
import com.dci.intellij.dbn.execution.method.ui.MethodExecutionHistory;
import com.dci.intellij.dbn.execution.method.ui.MethodExecutionInputDialog;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.ui.ObjectTreeModel;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static com.dci.intellij.dbn.common.message.MessageCallback.conditional;
import static com.dci.intellij.dbn.execution.ExecutionStatus.CANCELLED;
import static com.dci.intellij.dbn.execution.ExecutionStatus.EXECUTING;

@State(
    name = MethodExecutionManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class MethodExecutionManager extends AbstractProjectComponent implements PersistentStateComponent<Element> {
    public static final String COMPONENT_NAME = "DBNavigator.Project.MethodExecutionManager";

    private final MethodBrowserSettings browserSettings = new MethodBrowserSettings();
    private final MethodExecutionHistory executionHistory = new MethodExecutionHistory(getProject());
    private final MethodExecutionArgumentValuesCache argumentValuesCache = new MethodExecutionArgumentValuesCache();

    private MethodExecutionManager(Project project) {
        super(project);
    }

    public static MethodExecutionManager getInstance(@NotNull Project project) {
        return Failsafe.getComponent(project, MethodExecutionManager.class);
    }

    public MethodBrowserSettings getBrowserSettings() {
        return browserSettings;
    }

    public MethodExecutionHistory getExecutionHistory() {
        return executionHistory;
    }

    public MethodExecutionInput getExecutionInput(DBMethod method) {
        return executionHistory.getExecutionInput(method);
    }

    public MethodExecutionInput getExecutionInput(DBObjectRef<DBMethod> methodRef) {
        return executionHistory.getExecutionInput(methodRef);
    }

    public MethodExecutionArgumentValuesCache getArgumentValuesCache() {
        return argumentValuesCache;
    }

    public void startMethodExecution(@NotNull MethodExecutionInput executionInput, @NotNull DBDebuggerType debuggerType) {
        promptExecutionDialog(executionInput, debuggerType, () -> MethodExecutionManager.this.execute(executionInput));
    }

    public void startMethodExecution(@NotNull DBMethod method, @NotNull DBDebuggerType debuggerType) {
        promptExecutionDialog(method, debuggerType, () -> MethodExecutionManager.this.execute(method));
    }

    private void promptExecutionDialog(@NotNull DBMethod method, @NotNull DBDebuggerType debuggerType, Runnable callback) {
        MethodExecutionInput executionInput = getExecutionInput(method);
        promptExecutionDialog(executionInput, debuggerType, callback);
    }

    public void promptExecutionDialog(MethodExecutionInput executionInput, @NotNull DBDebuggerType debuggerType, Runnable callback) {
        ConnectionAction.invoke("the method execution", false, executionInput,
                (action) -> Progress.prompt(executionInput.getProject(), "Loading method details", true,
                        (progress) -> {
                            Project project = getProject();
                            ConnectionHandler connectionHandler = action.getConnectionHandler();
                            if (connectionHandler.isValid()) {
                                DBMethod method = executionInput.getMethod();
                                if (method == null) {
                                    String message =
                                            "Can not execute method " +
                                                    executionInput.getMethodRef().getPath() + ".\nMethod not found!";
                                    MessageUtil.showErrorDialog(project, message);
                                } else {
                                    // load the arguments in background
                                    executionInput.getMethod().getArguments();
                                    Dispatch.run(() -> {
                                        MethodExecutionInputDialog executionDialog = new MethodExecutionInputDialog(executionInput, debuggerType);
                                        executionDialog.show();
                                        if (executionDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                                            callback.run();
                                        }
                                    });
                                }
                            } else {
                                String message =
                                        "Can not execute method " + executionInput.getMethodRef().getPath() + ".\n" +
                                                "No connectivity to '" + connectionHandler.getQualifiedName() + "'. " +
                                                "Please check your connection settings and try again.";
                                MessageUtil.showErrorDialog(project, message);
                            }
                        }));
    }


    public void showExecutionHistoryDialog(
            @Nullable MethodExecutionInput selection,
            boolean editable,
            boolean debug,
            @Nullable ParametricRunnable<MethodExecutionInput, RuntimeException> callback) {

        Project project = getProject();
        Progress.prompt(project, "Loading method execution history", true,
                (progress) -> {
                    MethodExecutionInput selectedInput = CommonUtil.nvln(selection, executionHistory.getLastSelection());
                    if (selectedInput != null) {
                        // initialize method arguments while in background
                        DBMethod method = selectedInput.getMethod();
                        if (Failsafe.check(method)) {
                            method.getArguments();
                        }
                    }

                    if (!progress.isCanceled()) {
                        Dispatch.run(() -> {
                            MethodExecutionHistoryDialog executionHistoryDialog = new MethodExecutionHistoryDialog(project, selectedInput, editable, debug);
                            executionHistoryDialog.show();
                            MethodExecutionInput newlySelected = executionHistoryDialog.getSelectedExecutionInput();
                            if (newlySelected != null && callback != null) {
                                if (executionHistoryDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                                    callback.run(newlySelected);
                                }
                            }
                        });
                    }
                });
    }

    public void execute(DBMethod method) {
        MethodExecutionInput executionInput = getExecutionInput(method);
        execute(executionInput);
    }

    public void execute(MethodExecutionInput executionInput) {
        cacheArgumentValues(executionInput);
        executionHistory.setSelection(executionInput.getMethodRef());
        DBMethod method = executionInput.getMethod();
        ExecutionContext context = executionInput.getExecutionContext();
        context.set(EXECUTING, true);

        if (method == null) {
            DBObjectRef<DBMethod> methodRef = executionInput.getMethodRef();
            MessageUtil.showErrorDialog(getProject(), "Could not resolve " + methodRef.getQualifiedNameWithType() + "\".");
        } else {
            Project project = method.getProject();
            ConnectionHandler connectionHandler = Failsafe.nn(method.getConnectionHandler());
            DatabaseExecutionInterface executionInterface = connectionHandler.getInterfaceProvider().getDatabaseExecutionInterface();
            MethodExecutionProcessor executionProcessor = executionInterface.createExecutionProcessor(method);

            Progress.prompt(project, "Executing method", true,
                    (progress) -> {
                        try {
                            progress.setText("Executing " + method.getQualifiedNameWithType());
                            executionProcessor.execute(executionInput, DBDebuggerType.NONE);
                            if (context.isNot(CANCELLED)) {
                                ExecutionManager executionManager = ExecutionManager.getInstance(project);
                                executionManager.addExecutionResult(executionInput.getExecutionResult());
                                context.set(EXECUTING, false);
                            }

                            context.set(CANCELLED, false);
                        } catch (SQLException e) {
                            context.set(EXECUTING, false);
                            if (context.isNot(CANCELLED)) {
                                MessageUtil.showErrorDialog(project,
                                        "Method execution error",
                                        "Error executing " + method.getQualifiedNameWithType() + ".\n" + e.getMessage().trim(),
                                        new String[]{"Try Again", "Cancel"}, 0,
                                        (option) -> conditional(option == 0,
                                                () -> startMethodExecution(executionInput, DBDebuggerType.NONE)));
                            }
                        }
                    });
        }
    }

    private void cacheArgumentValues(MethodExecutionInput executionInput) {
        ConnectionHandler connectionHandler = executionInput.getExecutionContext().getTargetConnection();
        if (connectionHandler != null) {
            Set<MethodExecutionArgumentValue> argumentValues = executionInput.getArgumentValues();
            for (MethodExecutionArgumentValue argumentValue : argumentValues) {
                argumentValuesCache.cacheVariable(connectionHandler.getConnectionId(), argumentValue.getName(), argumentValue.getValue());
            }
        }
    }

    public void debugExecute(
            @NotNull MethodExecutionInput executionInput,
            @NotNull DBNConnection connection,
            DBDebuggerType debuggerType) throws SQLException {

        DBMethod method = executionInput.getMethod();
        if (method != null) {
            ConnectionHandler connectionHandler = method.getConnectionHandler();
            DatabaseExecutionInterface executionInterface = connectionHandler.getInterfaceProvider().getDatabaseExecutionInterface();
            MethodExecutionProcessor executionProcessor = debuggerType == DBDebuggerType.JDWP ?
                    executionInterface.createExecutionProcessor(method) :
                    executionInterface.createDebugExecutionProcessor(method);

            executionProcessor.execute(executionInput, connection, debuggerType);
            ExecutionContext context = executionInput.getExecutionContext();
            if (context.isNot(CANCELLED)) {
                ExecutionManager executionManager = ExecutionManager.getInstance(method.getProject());
                executionManager.addExecutionResult(executionInput.getExecutionResult());
            }
            context.set(CANCELLED, false);
        }
    }

    public void promptMethodBrowserDialog(
            @Nullable MethodExecutionInput executionInput,  boolean debug,
            @Nullable ParametricRunnable.Basic<MethodExecutionInput> callback) {

        Project project = getProject();
        Progress.prompt(project, "Loading executable elements", true,
                (progress) -> {
                    MethodExecutionManager executionManager = MethodExecutionManager.getInstance(project);
                    MethodBrowserSettings settings = executionManager.getBrowserSettings();
                    DBMethod currentMethod = executionInput == null ? null : executionInput.getMethod();
                    if (currentMethod != null) {
                        currentMethod.getArguments();
                        settings.setConnectionHandler(currentMethod.getConnectionHandler());
                        settings.setSchema(currentMethod.getSchema());
                        settings.setMethod(currentMethod);
                    }

                    DBSchema schema = settings.getSchema();
                    ObjectTreeModel objectTreeModel = !debug || DatabaseFeature.DEBUGGING.isSupported(schema) ?
                            new ObjectTreeModel(schema, settings.getVisibleObjectTypes(), settings.getMethod()) :
                            new ObjectTreeModel(null, settings.getVisibleObjectTypes(), null);

                    Dispatch.run(() -> {
                        Failsafe.nn(project);
                        MethodExecutionBrowserDialog browserDialog = new MethodExecutionBrowserDialog(project, objectTreeModel, true);
                        browserDialog.show();
                        if (browserDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                            DBMethod method = browserDialog.getSelectedMethod();
                            MethodExecutionInput methodExecutionInput = executionManager.getExecutionInput(method);
                            if (callback != null && methodExecutionInput != null) {
                                callback.run(methodExecutionInput);
                            }
                        }
                    });
                });
    }


    public void setExecutionInputs(List<MethodExecutionInput> executionInputs) {
        executionHistory.setExecutionInputs(executionInputs);
    }

    public void cleanupExecutionHistory(List<ConnectionId> connectionIds) {
        executionHistory.cleanupHistory(connectionIds);
    }

    /*********************************************************
     *                    ProjectComponent                   *
     *********************************************************/
    @Override
    @NotNull
    @NonNls
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("state");
        Element browserSettingsElement = new Element("method-browser");
        element.addContent(browserSettingsElement);
        browserSettings.writeConfiguration(browserSettingsElement);


        executionHistory.writeState(element);
        argumentValuesCache.writeState(element);
        return element;
    }

    @Override
    public void loadState(@NotNull Element element) {
        Element browserSettingsElement = element.getChild("method-browser");
        if (browserSettingsElement != null) {
            browserSettings.readConfiguration(browserSettingsElement);
        }

        executionHistory.readState(element);
        argumentValuesCache.readState(element);
    }




    @Override
    public void disposeInner() {
        Disposer.dispose(executionHistory);
        super.disposeInner();
    }
}
