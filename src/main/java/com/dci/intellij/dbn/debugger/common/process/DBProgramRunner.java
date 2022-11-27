package com.dci.intellij.dbn.debugger.common.process;

import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.notification.NotificationGroup;
import com.dci.intellij.dbn.common.notification.NotificationSupport;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.debugger.common.config.DBRunConfig;
import com.dci.intellij.dbn.debugger.common.config.ui.CompileDebugDependenciesDialog;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.execution.ExecutionInput;
import com.dci.intellij.dbn.execution.compiler.*;
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
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dci.intellij.dbn.common.message.MessageCallback.when;
import static com.dci.intellij.dbn.common.util.Messages.options;
import static com.dci.intellij.dbn.common.util.Messages.showWarningDialog;
import static com.dci.intellij.dbn.common.util.Unsafe.cast;

public abstract class DBProgramRunner<T extends ExecutionInput> extends GenericProgramRunner {
    public static final String INVALID_RUNNER_ID = "DBNInvalidRunner";

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return doExecute(environment.getProject(), environment.getExecutor(), state, null, environment);
    }

    @Override
    @Nullable
    @SneakyThrows
    protected RunContentDescriptor doExecute(@NotNull Project project, @NotNull RunProfileState state, RunContentDescriptor contentToReuse, @NotNull ExecutionEnvironment env) {
        return doExecute(project, env.getExecutor(), state, contentToReuse, env);
    }

    private RunContentDescriptor doExecute(
            Project project,
            Executor executor,
            RunProfileState state,
            RunContentDescriptor contentToReuse,
            ExecutionEnvironment environment) throws ExecutionException {

        DBRunConfig runProfile = (DBRunConfig) environment.getRunProfile();
        ConnectionHandler connection = runProfile.getConnection();
        if (connection == null) return null;

        ConnectionAction.invoke("the debug execution", false, connection,
                action -> Progress.prompt(project, connection, true,
                        "Checking debug privileges",
                        "Checking debug privileges for user \"" + connection.getUserName() + "\"",
                        progress -> performPrivilegeCheck(
                                project,
                                cast(runProfile.getExecutionInput()),
                                environment,
                                null)),
                null,
                action -> {
                    DatabaseDebuggerManager databaseDebuggerManager = DatabaseDebuggerManager.getInstance(project);
                    return databaseDebuggerManager.checkForbiddenOperation(connection,
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
        ConnectionHandler connection = runProfile.getConnection();
        if (connection == null) return;

        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
        List<String> missingPrivileges = debuggerManager.getMissingDebugPrivileges(connection);
        if (missingPrivileges.size() > 0) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("The current user (").append(connection.getUserName()).append(") does not have sufficient privileges to perform debug operations on this database.\n");
            buffer.append("Please contact your administrator to grant the required privileges. ");
            buffer.append("Missing privileges:\n");
            for (String missingPrivilege : missingPrivileges) {
                buffer.append(" - ").append(missingPrivilege).append("\n");
            }

            showWarningDialog(
                    project, "Insufficient privileges", buffer.toString(),
                    options("Continue anyway", "Cancel"), 0,
                    option -> when(option == 0, () ->
                            performInitialization(
                                    connection,
                                    executionInput,
                                    environment,
                                    callback)));
        } else {
            performInitialization(
                    connection,
                    executionInput,
                    environment,
                    callback);
        }
    }

    private void performInitialization(
            @NotNull ConnectionHandler connection,
            @NotNull T executionInput,
            @NotNull ExecutionEnvironment environment,
            @Nullable Callback callback) {
        DBRunConfig runProfile = (DBRunConfig) environment.getRunProfile();
        if (runProfile.isCompileDependencies()) {
            Project project = connection.getProject();

            Progress.prompt(project, connection, true,
                    "Initializing debug environment",
                    "Loading method dependencies",
                    progress -> {
                DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);

                if (!project.isDisposed() && !progress.isCanceled()) {
                    List<DBMethod> methods = runProfile.getMethods();
                    List<DBSchemaObject> dependencies = debuggerManager.loadCompileDependencies(methods);
                    if (!progress.isCanceled()) {
                        if (dependencies.size() > 0) {
                            performCompile(
                                    connection,
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
            @NotNull ConnectionHandler connection,
            @NotNull T executionInput,
            @NotNull ExecutionEnvironment environment,
            @Nullable Callback callback,
            List<DBSchemaObject> dependencies) {

        Dispatch.run(() -> {
            Project project = connection.getProject();
            DBRunConfig runConfiguration = (DBRunConfig) environment.getRunProfile();
            CompileDebugDependenciesDialog dependenciesDialog = new CompileDebugDependenciesDialog(runConfiguration, dependencies);
            dependenciesDialog.show();
            DBObjectRef<DBSchemaObject>[] selectedDependencies =  dependenciesDialog.getSelection();

            if (dependenciesDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE){
                if (selectedDependencies.length > 0) {

                    Progress.prompt(project, connection, true,
                            "Compiling dependencies",
                            "Compiling dependencies for program execution",
                            progress -> {
                        DatabaseCompilerManager compilerManager = DatabaseCompilerManager.getInstance(project);
                        for (DBObjectRef<DBSchemaObject> objectRef : selectedDependencies) {
                            DBSchemaObject schemaObject = objectRef.ensure();
                            Progress.check(progress);

                            progress.setText2("Compiling " + objectRef.getQualifiedNameWithType());
                            DBContentType contentType = schemaObject.getContentType();
                            CompilerAction compilerAction = new CompilerAction(CompilerActionSource.BULK_COMPILE, contentType);
                            compilerManager.compileObject(schemaObject, CompileType.DEBUG, compilerAction);
                        }
                        ProjectEvents.notify(project,
                                CompileManagerListener.TOPIC,
                                (listener) -> listener.compileFinished(connection, null));
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
            ConnectionHandler connection = executionInput.getConnection();
            Project project = environment.getProject();

            promptExecutionDialog(executionInput,
                    () -> {
                        DBDebugProcessStarter debugProcessStarter = createProcessStarter(connection);
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

    protected abstract DBDebugProcessStarter createProcessStarter(ConnectionHandler connection);

    protected abstract void promptExecutionDialog(T executionInput, Runnable callback);
}
