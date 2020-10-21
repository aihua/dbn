package com.dci.intellij.dbn.debugger.common.process;

import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.notification.NotificationGroup;
import com.dci.intellij.dbn.common.notification.NotificationSupport;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.util.Unsafe;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.debugger.common.config.DBRunConfig;
import com.dci.intellij.dbn.debugger.common.config.ui.CompileDebugDependenciesDialog;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.execution.ExecutionInput;
import com.dci.intellij.dbn.execution.compiler.CompileManagerListener;
import com.dci.intellij.dbn.execution.compiler.CompileType;
import com.dci.intellij.dbn.execution.compiler.CompilerAction;
import com.dci.intellij.dbn.execution.compiler.CompilerActionSource;
import com.dci.intellij.dbn.execution.compiler.DatabaseCompilerManager;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.history.LocalHistory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dci.intellij.dbn.common.message.MessageCallback.conditional;
import static com.dci.intellij.dbn.common.util.MessageUtil.options;
import static com.dci.intellij.dbn.common.util.MessageUtil.showWarningDialog;

public abstract class DBProgramRunner<T extends ExecutionInput> extends GenericProgramRunner {
    public static final String INVALID_RUNNER_ID = "DBNInvalidRunner";

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return doExecute(environment.getProject(), environment.getExecutor(), state, null, environment);
    }

    @Override
    @Nullable
    protected RunContentDescriptor doExecute(@NotNull Project project, @NotNull RunProfileState state, RunContentDescriptor contentToReuse, @NotNull ExecutionEnvironment env) {
        return Unsafe.call(() -> doExecute(project, env.getExecutor(), state, contentToReuse, env));
    }

    private RunContentDescriptor doExecute(
            Project project,
            Executor executor,
            RunProfileState state,
            RunContentDescriptor contentToReuse,
            ExecutionEnvironment environment) throws ExecutionException {

        DBRunConfig runProfile = (DBRunConfig) environment.getRunProfile();
        ConnectionHandler connectionHandler = runProfile.getConnectionHandler();
        ConnectionAction.invoke("the debug execution", false, connectionHandler,
                (action) -> Progress.prompt(project, "Checking debug privileges", true,
                        (progress)-> {
                            performPrivilegeCheck(
                                    project,
                                    (T) runProfile.getExecutionInput(),
                                    environment,
                                    null);
                        }),
                null,
                (action) -> {
                    DatabaseDebuggerManager databaseDebuggerManager = DatabaseDebuggerManager.getInstance(project);
                    return databaseDebuggerManager.checkForbiddenOperation(connectionHandler,
                            "Another debug session is active on this connection. You can only run one debug session at the time.");
                });
        return null;
    }

    private void performPrivilegeCheck(
            Project project,
            T executionInput,
            ExecutionEnvironment environment,
            Callback callback) {
        DBRunConfig runProfile = (DBRunConfig) environment.getRunProfile();
        ConnectionHandler connectionHandler = runProfile.getConnectionHandler();
        if (connectionHandler != null) {
            DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
            List<String> missingPrivileges = debuggerManager.getMissingDebugPrivileges(connectionHandler);
            if (missingPrivileges.size() > 0) {
                StringBuilder buffer = new StringBuilder();
                buffer.append("The current user (").append(connectionHandler.getUserName()).append(") does not have sufficient privileges to perform debug operations on this database.\n");
                buffer.append("Please contact your administrator to grant the required privileges. ");
                buffer.append("Missing privileges:\n");
                for (String missingPrivilege : missingPrivileges) {
                    buffer.append(" - ").append(missingPrivilege).append("\n");
                }

                showWarningDialog(
                        project, "Insufficient privileges", buffer.toString(),
                        options("Continue anyway", "Cancel"), 0,
                        (option) -> conditional(option == 0,
                                () -> performInitialize(
                                        connectionHandler,
                                        executionInput,
                                        environment,
                                        callback)));
            } else {
                performInitialize(
                        connectionHandler,
                        executionInput,
                        environment,
                        callback);
            }
        }
    }

    private void performInitialize(
            @NotNull ConnectionHandler connectionHandler,
            @NotNull T executionInput,
            @NotNull ExecutionEnvironment environment,
            @Nullable Callback callback) {
        DBRunConfig runProfile = (DBRunConfig) environment.getRunProfile();
        if (runProfile.isCompileDependencies()) {
            Project project = connectionHandler.getProject();

            Progress.prompt( project,
                    "Initializing debug environment", true,
                    (progress) -> {
                        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
                        progress.setText2("Loading method dependencies");

                        if (!project.isDisposed() && !progress.isCanceled()) {
                            List<DBMethod> methods = runProfile.getMethods();
                            List<DBSchemaObject> dependencies = debuggerManager.loadCompileDependencies(methods, progress);
                            if (!progress.isCanceled()) {
                                if (dependencies.size() > 0) {
                                    performCompile(
                                            connectionHandler,
                                            executionInput,
                                            environment,
                                            callback,
                                            dependencies);
                                } else {
                                    performExecution(
                                            executionInput,
                                            environment,
                                            callback);
                                }
                            }
                        }
                    });
        }
    }

    private void performCompile(
            @NotNull ConnectionHandler connectionHandler,
            @NotNull T executionInput,
            @NotNull ExecutionEnvironment environment,
            @Nullable Callback callback,
            List<DBSchemaObject> dependencies) {

        Dispatch.run(() -> {
            Project project = connectionHandler.getProject();
            DBRunConfig runConfiguration = (DBRunConfig) environment.getRunProfile();
            CompileDebugDependenciesDialog dependenciesDialog = new CompileDebugDependenciesDialog(runConfiguration, dependencies);
            dependenciesDialog.show();
            DBObjectRef<DBSchemaObject>[] selectedDependencies =  dependenciesDialog.getSelection();

            if (dependenciesDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE){
                if (selectedDependencies.length > 0) {

                    Progress.prompt(project, "Compiling dependencies", true,
                            (progress) -> {
                                DatabaseCompilerManager compilerManager = DatabaseCompilerManager.getInstance(project);
                                for (DBObjectRef<DBSchemaObject> objectRef : selectedDependencies) {
                                    DBSchemaObject schemaObject = objectRef.ensure();
                                    Progress.check(progress);

                                    progress.setText("Compiling " + objectRef.getQualifiedNameWithType());
                                    DBContentType contentType = schemaObject.getContentType();
                                    CompilerAction compilerAction = new CompilerAction(CompilerActionSource.BULK_COMPILE, contentType);
                                    compilerManager.compileObject(schemaObject, CompileType.DEBUG, compilerAction);
                                }
                                ProjectEvents.notify(project,
                                        CompileManagerListener.TOPIC,
                                        (listener) -> listener.compileFinished(connectionHandler, null));
                                Progress.check(progress);

                                performExecution(
                                        executionInput,
                                        environment,
                                        callback);
                            });
                } else {
                    performExecution(
                            executionInput,
                            environment,
                            callback);
                }
            }
        });
    }

    private void performExecution(
            T executionInput,
            ExecutionEnvironment environment,
            Callback callback) {
        Dispatch.run(() -> {
            ConnectionHandler connectionHandler = executionInput.getConnectionHandler();
            Project project = environment.getProject();

            promptExecutionDialog(executionInput,
                    () -> {
                        DBDebugProcessStarter debugProcessStarter = createProcessStarter(connectionHandler);
                        try {
                            XDebugSession session = XDebuggerManager.getInstance(project).startSession(
                                    environment,
                                    debugProcessStarter);

                            RunContentDescriptor descriptor = session.getRunContentDescriptor();

                            if (callback != null) callback.processStarted(descriptor);
                            Executor executor = environment.getExecutor();
                            if (true /*LocalHistoryConfiguration.getInstance().ADD_LABEL_ON_RUNNING*/) {
                                RunProfile runProfile = environment.getRunProfile();
                                LocalHistory.getInstance().putSystemLabel(project, executor.getId() + " " + runProfile.getName());
                            }

                            ExecutionManager.getInstance(project).getContentManager().showRunContent(executor, descriptor);

                            ProcessHandler processHandler = descriptor.getProcessHandler();
                            if (processHandler != null) {
                                processHandler.startNotify();
                                ExecutionConsole executionConsole = descriptor.getExecutionConsole();
                                if (executionConsole instanceof ConsoleView) {
                                    ConsoleView consoleView = (ConsoleView) executionConsole;
                                    consoleView.attachToProcess(processHandler);
                                }
                            }

                        } catch (ExecutionException e) {
                            NotificationSupport.sendErrorNotification(
                                    project,
                                    NotificationGroup.DEBUGGER,
                                    "Error initializing environment: {0}", e);
                        }
                    });
        });
    }

    protected abstract DBDebugProcessStarter createProcessStarter(ConnectionHandler connectionHandler);

    protected abstract void promptExecutionDialog(T executionInput, Runnable callback);
}
