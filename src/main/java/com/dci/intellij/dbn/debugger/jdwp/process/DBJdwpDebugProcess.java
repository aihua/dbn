package com.dci.intellij.dbn.debugger.jdwp.process;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.exception.ProcessDeferredException;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.dci.intellij.dbn.connection.Resources;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dci.intellij.dbn.debugger.DBDebugConsoleLogger;
import com.dci.intellij.dbn.debugger.DBDebugOperation;
import com.dci.intellij.dbn.debugger.DBDebugUtil;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.debugger.common.breakpoint.DBBreakpointHandler;
import com.dci.intellij.dbn.debugger.common.breakpoint.DBBreakpointUtil;
import com.dci.intellij.dbn.debugger.common.config.DBRunConfig;
import com.dci.intellij.dbn.debugger.common.config.DBRunConfigCategory;
import com.dci.intellij.dbn.debugger.common.process.DBDebugProcess;
import com.dci.intellij.dbn.debugger.common.process.DBDebugProcessStatus;
import com.dci.intellij.dbn.debugger.common.process.DBDebugProcessStatusHolder;
import com.dci.intellij.dbn.debugger.jdwp.DBJdwpBreakpointHandler;
import com.dci.intellij.dbn.debugger.jdwp.DBJdwpSourcePath;
import com.dci.intellij.dbn.debugger.jdwp.ManagedThreadCommand;
import com.dci.intellij.dbn.debugger.jdwp.frame.DBJdwpDebugStackFrame;
import com.dci.intellij.dbn.debugger.jdwp.frame.DBJdwpDebugSuspendContext;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.execution.ExecutionContext;
import com.dci.intellij.dbn.execution.ExecutionInput;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.DBProgram;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcessAdapter;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.engine.SuspendContext;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionAdapter;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.sun.jdi.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.Inet4Address;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static com.dci.intellij.dbn.debugger.common.process.DBDebugProcessStatus.*;
import static com.intellij.debugger.impl.PrioritizedTask.Priority.LOW;

public abstract class DBJdwpDebugProcess<T extends ExecutionInput>
        extends JavaDebugProcess
        implements DBDebugProcess {

    public static final Key<DBJdwpDebugProcess> KEY = new Key<>("DBNavigator.JdwpDebugProcess");
    private final ConnectionRef connection;
    private final DBDebugProcessStatusHolder status = new DBDebugProcessStatusHolder();
    private final DBBreakpointHandler<DBJdwpDebugProcess>[] breakpointHandlers;
    private final DBDebugConsoleLogger console;
    private final String declaredBlockIdentifier;
    protected DBNConnection targetConnection;
    private int localTcpPort = 4000;

    private transient XSuspendContext lastSuspendContext;

    protected DBJdwpDebugProcess(@NotNull final XDebugSession session, DebuggerSession debuggerSession, ConnectionHandler connection, int tcpPort) {
        super(session, debuggerSession);
        this.console = new DBDebugConsoleLogger(session);
        this.connection = ConnectionRef.of(connection);

        Project project = session.getProject();
        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
        debuggerManager.registerDebugSession(connection);

        DBJdwpBreakpointHandler breakpointHandler = new DBJdwpBreakpointHandler(session, this);
        breakpointHandlers = new DBBreakpointHandler[]{breakpointHandler};
        localTcpPort = tcpPort;
        debuggerSession.getProcess().putUserData(KEY, this);

        DatabaseDebuggerInterface debuggerInterface = connection.getDebuggerInterface();
        declaredBlockIdentifier = debuggerInterface.getJdwpBlockIdentifier().replace(".", "\\");
    }

    @Override
    public boolean set(DBDebugProcessStatus status, boolean value) {
        return this.status.set(status, value);
    }

    @Override
    public boolean is(DBDebugProcessStatus status) {
        return this.status.is(status);
    }

    protected boolean shouldSuspend(XSuspendContext suspendContext) {
        if (is(TARGET_EXECUTION_TERMINATED)) {
            return false;
        } else {
            XExecutionStack executionStack = suspendContext.getActiveExecutionStack();
            if (executionStack != null) {
                XStackFrame topFrame = executionStack.getTopFrame();
                if (topFrame instanceof DBJdwpDebugStackFrame) {
                    return true;
                }
                Location location = getLocation(topFrame);
                VirtualFile virtualFile = getVirtualFile(location);
                return virtualFile != null;
            }
        }
        return true;
    }

    @Override
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Nullable
    public T getExecutionInput() {
        DBRunConfig<T> runProfile = getRunProfile();
        return runProfile == null ? null : runProfile.getExecutionInput();
    }

    DBRunConfig<T> getRunProfile() {
        return (DBRunConfig<T>) getSession().getRunProfile();
    }

    @Override
    @NotNull
    public Project getProject() {
        return getSession().getProject();
    }

    @Override
    public DatabaseDebuggerInterface getDebuggerInterface() {
        return getConnection().getDebuggerInterface();
    }

    @NotNull
    public DBNConnection getTargetConnection() {
        return Failsafe.nn(targetConnection);
    }

    @NotNull
    @Override
    public DBBreakpointHandler<DBJdwpDebugProcess>[] getBreakpointHandlers() {
        return breakpointHandlers;
    }

    public DBBreakpointHandler<DBJdwpDebugProcess> getBreakpointHandler() {
        return breakpointHandlers[0];
    }

    @Override
    public boolean checkCanInitBreakpoints() {
        return is(BREAKPOINT_SETTING_ALLOWED);
    }

    @Override
    public DBDebugConsoleLogger getConsole() {
        return console;
    }

    @Override
    public void sessionInitialized() {
        final XDebugSession session = getSession();
        if (session instanceof XDebugSessionImpl) {
            XDebugSessionImpl sessionImpl = (XDebugSessionImpl) session;
            sessionImpl.getSessionData().setBreakpointsMuted(false);
        }
        DBRunConfig<T> runProfile = getRunProfile();
        List<DBMethod> methods = runProfile.getMethods();
        if (methods.size() > 0) {
            getBreakpointHandler().registerDefaultBreakpoint(methods.get(0));
        }

        DebuggerSession debuggerSession = getDebuggerSession();
        final Project project = getProject();
        DebuggerManager debuggerManager = DebuggerManager.getInstance(project);
        ProcessHandler processHandler = debuggerSession.getProcess().getProcessHandler();
        debuggerManager.addDebugProcessListener(processHandler, new DebugProcessAdapter(){
            @Override
            public void paused(@NotNull SuspendContext suspendContext) {
                if (suspendContext instanceof XSuspendContext) {
                    XSuspendContext xSuspendContext = (XSuspendContext) suspendContext;

                    XExecutionStack[] executionStacks = xSuspendContext.getExecutionStacks();
                    for (XExecutionStack executionStack : executionStacks) {
                        //System.out.println();
                    }

                    //underlyingFrame.getDescriptor().getLocation()

                }
            }
        });

        session.addSessionListener(new XDebugSessionAdapter() {
            @Override
            public void sessionPaused() {
                XSuspendContext suspendContext = session.getSuspendContext();
                if (!shouldSuspend(suspendContext)) {
                    Dispatch.run(() -> session.resume());
                } else {
                    XExecutionStack activeExecutionStack = suspendContext.getActiveExecutionStack();
                    if (activeExecutionStack != null) {
                        XStackFrame topFrame = activeExecutionStack.getTopFrame();
                        if (topFrame instanceof JavaStackFrame) {
                            Location location = getLocation(topFrame);
                            VirtualFile virtualFile = getVirtualFile(location);
                            DBDebugUtil.openEditor(virtualFile);
                        }
                    }
                }
            }
        });

        debuggerSession.getContextManager().addListener((newContext, event) -> {
            SuspendContextImpl suspendContext = newContext.getSuspendContext();
            overwriteSuspendContext(suspendContext);
        });

        getDebuggerSession().getProcess().setXDebugProcess(this);

        DBDebugOperation.run(project, "initialize debug environment", () -> {
            try {
                console.system("Initializing debug environment");
                T executionInput = getExecutionInput();
                if (executionInput != null) {
                    ConnectionHandler connection = getConnection();
                    SchemaId schemaId = executionInput.getExecutionContext().getTargetSchema();
                    targetConnection = connection.getDebugConnection(schemaId);
                    targetConnection.setAutoCommit(false);
                    DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
                    debuggerInterface.initializeJdwpSession(targetConnection, Inet4Address.getLocalHost().getHostAddress(), String.valueOf(localTcpPort));
                    console.system("Debug session initialized (JDWP)");
                    set(BREAKPOINT_SETTING_ALLOWED, true);

                    initializeBreakpoints();
                    startTargetProgram();
                }
            } catch (Exception e) {
                set(SESSION_INITIALIZATION_THREW_EXCEPTION, true);
                console.error("Error initializing debug environment\n" + e.getMessage());
                stop();
            }
        });
    }

    private void initializeBreakpoints() {
        console.system("Registering breakpoints");
        List<DBMethod> methods = getRunProfile().getMethods();
        List<XLineBreakpoint<XBreakpointProperties>> breakpoints = DBBreakpointUtil.getDatabaseBreakpoints(getConnection());
        getBreakpointHandler().registerBreakpoints(breakpoints, methods);
    }

    private void overwriteSuspendContext(final @Nullable XSuspendContext suspendContext) {
        if (suspendContext != null && suspendContext != lastSuspendContext && !(suspendContext instanceof DBJdwpDebugSuspendContext)) {
            DebugProcessImpl debugProcess = getDebuggerSession().getProcess();
            ManagedThreadCommand.schedule(debugProcess, LOW, () -> {
                lastSuspendContext = suspendContext;
                XDebugSession session = getSession();
                if (shouldSuspend(suspendContext)) {
                    DBJdwpDebugSuspendContext dbSuspendContext = new DBJdwpDebugSuspendContext(DBJdwpDebugProcess.this, suspendContext);
                    session.positionReached(dbSuspendContext);
                }
            });
            throw new ProcessDeferredException();
        }
    }

    private void startTargetProgram() {
        // trigger in managed thread
        DebugProcessImpl debugProcess = getDebuggerSession().getProcess();
        T executionInput = getExecutionInput();
        ManagedThreadCommand.schedule(debugProcess, LOW, () -> {
            Progress.background(getProject(), getConnection(), false,
                    "Running debugger target program",
                    "Executing " + (executionInput == null ? " target program" : executionInput.getExecutionContext().getTargetName()),
                    progress -> {
                        console.system("Executing target program");
                        if (is(SESSION_INITIALIZATION_THREW_EXCEPTION)) return;
                        try {
                            set(TARGET_EXECUTION_STARTED, true);
                            executeTarget();
                        } catch (SQLException e) {
                            set(TARGET_EXECUTION_THREW_EXCEPTION, true);
                            if (isNot(DEBUGGER_STOPPING)) {
                                String message = executionInput == null ? "Error executing target program" : "Error executing " + executionInput.getExecutionContext().getTargetName();
                                console.error(message + ": " + e.getMessage());
                            }
                        } finally {
                            set(TARGET_EXECUTION_TERMINATED, true);
                            stop();
                        }
                    });
        });
    }

    protected abstract void executeTarget() throws SQLException;

    @Override
    public void stop() {
        conditional(DEBUGGER_STOPPING, () -> {
            set(BREAKPOINT_SETTING_ALLOWED, false);
            console.system("Stopping debugger...");
            getSession().stop();
            stopDebugger();
            super.stop();
        });
    }

    private void stopDebugger() {
        Progress.background(getProject(), getConnection(), false,
                "Stopping debugger",
                "Stopping debugger session",
                progress -> {
                    T executionInput = getExecutionInput();
                    if (executionInput != null && isNot(TARGET_EXECUTION_TERMINATED)) {
                        ExecutionContext context = executionInput.getExecutionContext();
                        Resources.cancel(context.getStatement());
                    }

                    ConnectionHandler connection = getConnection();
                    try {
                        DBNConnection targetConnection = getTargetConnection();
                        DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
                        debuggerInterface.disconnectJdwpSession(targetConnection);

                    } catch (SQLException e) {
                        console.error("Error stopping debugger: " + e.getMessage());
                    } finally {
                        DBRunConfig<T> runProfile = getRunProfile();
                        if (runProfile != null && runProfile.getCategory() != DBRunConfigCategory.CUSTOM) {
                            runProfile.setCanRun(false);
                        }

                        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(getProject());
                        debuggerManager.unregisterDebugSession(connection);
                        releaseTargetConnection();
                        console.system("Debugger stopped");
                    }
                });
    }


    protected void releaseTargetConnection() {
        Resources.close(targetConnection);
        targetConnection = null;
    }

    @Nullable
    public VirtualFile getVirtualFile(Location location) {
        if (location != null) {
            String sourceUrl = "<NULL>";
            try {
                sourceUrl = location.sourcePath();
                DBJdwpSourcePath sourcePath = DBJdwpSourcePath.from(sourceUrl);
                String programType = sourcePath.getProgramType();
                if (!Objects.equals(programType, "Block")) {
                    String schemaName = sourcePath.getProgramOwner();
                    String programName = sourcePath.getProgramName();
                    DBSchema schema = getConnection().getObjectBundle().getSchema(schemaName);
                    if (schema != null) {
                        DBProgram program = schema.getProgram(programName);
                        if (program != null) {
                            DBEditableObjectVirtualFile editableVirtualFile = program.getEditableVirtualFile();
                            DBContentType contentType = Objects.equals(programType, "PackageBody") ? DBContentType.CODE_BODY : DBContentType.CODE_SPEC;
                            return editableVirtualFile.getContentFile(contentType);
                        } else {
                            DBMethod method = schema.getMethod(programName, (short) 0);
                            if (method != null) {
                                return method.getEditableVirtualFile().getContentFile(DBContentType.CODE);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                getConsole().warning("Error evaluating suspend position '" + sourceUrl + "': " + Commons.nvl(e.getMessage(), e.getClass().getSimpleName()));
            }
        }
        return null;
    }

    public boolean isDeclaredBlock(@Nullable Location location) {
        try {
            if (location != null && declaredBlockIdentifier != null && declaredBlockIdentifier.length() > 0) {
                String sourcePath = location.sourcePath();
                return sourcePath.startsWith(declaredBlockIdentifier);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }



    @Nullable
    private Location getLocation(@Nullable XStackFrame stackFrame) {
        if (stackFrame instanceof JavaStackFrame) {
            return ((JavaStackFrame) stackFrame).getDescriptor().getLocation();
        }
        return null;
    }
}
