package com.dci.intellij.dbn.execution.statement.processor;

import com.dci.intellij.dbn.common.dispose.Checks;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.StatefulDisposableBase;
import com.dci.intellij.dbn.common.editor.BasicTextEditor;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.load.ProgressMonitor;
import com.dci.intellij.dbn.common.message.MessageType;
import com.dci.intellij.dbn.common.navigation.NavigationInstructions;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.ref.WeakRef;
import com.dci.intellij.dbn.common.thread.CancellableDatabaseCall;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.thread.Read;
import com.dci.intellij.dbn.common.util.Documents;
import com.dci.intellij.dbn.common.util.Safe;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.*;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.jdbc.DBNStatement;
import com.dci.intellij.dbn.connection.mapping.FileConnectionContextManager;
import com.dci.intellij.dbn.connection.session.DatabaseSession;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.EditorProviderId;
import com.dci.intellij.dbn.execution.ExecutionManager;
import com.dci.intellij.dbn.execution.ExecutionOption;
import com.dci.intellij.dbn.execution.compiler.*;
import com.dci.intellij.dbn.execution.logging.DatabaseLoggingManager;
import com.dci.intellij.dbn.execution.statement.*;
import com.dci.intellij.dbn.execution.statement.result.StatementExecutionBasicResult;
import com.dci.intellij.dbn.execution.statement.result.StatementExecutionResult;
import com.dci.intellij.dbn.execution.statement.result.StatementExecutionStatus;
import com.dci.intellij.dbn.execution.statement.variables.StatementExecutionVariablesBundle;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.PsiElementRef;
import com.dci.intellij.dbn.language.common.PsiFileRef;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.psi.*;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.common.list.DBObjectList;
import com.dci.intellij.dbn.object.common.list.DBObjectListContainer;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static com.dci.intellij.dbn.common.navigation.NavigationInstruction.*;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dci.intellij.dbn.execution.ExecutionStatus.*;
import static com.dci.intellij.dbn.object.common.property.DBObjectProperty.COMPILABLE;

public class StatementExecutionBasicProcessor extends StatefulDisposableBase implements StatementExecutionProcessor {
    private final ProjectRef project;
    private final WeakRef<FileEditor> fileEditor;
    private PsiFileRef<DBLanguagePsiFile> psiFile;
    private PsiElementRef<ExecutablePsiElement> cachedExecutable;
    private EditorProviderId editorProviderId;
    private transient CancellableDatabaseCall<StatementExecutionResult> databaseCall;

    private StatementExecutionInput executionInput;
    private StatementExecutionResult executionResult;
    protected int index;

    private final String name;
    private final Icon icon;

    private String stickyResultName;

    private final Latent<String> resultName = Latent.basic(() -> {
        if (stickyResultName != null) {
            return stickyResultName;
        }
        String resultName = null;
        ExecutablePsiElement executablePsiElement = executionInput.getExecutablePsiElement();
        if (executablePsiElement!= null) {
            resultName = executablePsiElement.createSubjectList();
        }
        if (Strings.isEmptyOrSpaces(resultName)) {
            resultName = "Result " + index;
        }
        return resultName;
    });

    public StatementExecutionBasicProcessor(@NotNull Project project, @NotNull FileEditor fileEditor, @NotNull ExecutablePsiElement psiElement, int index) {
        DBLanguagePsiFile psiFile = psiElement.getFile();

        this.project = ProjectRef.of(project);
        this.fileEditor = WeakRef.of(fileEditor);
        this.psiFile = PsiFileRef.of(psiFile);

        this.cachedExecutable = PsiElementRef.of(psiElement);
        this.name = psiFile.getName();
        this.icon = psiFile.getIcon();
        this.index = index;
        executionInput = new StatementExecutionInput(psiElement.getText(), psiElement.prepareStatementText(), this);
        initEditorProviderId(fileEditor);
    }

    StatementExecutionBasicProcessor(@NotNull Project project, @NotNull FileEditor fileEditor, @NotNull DBLanguagePsiFile psiFile, String sqlStatement, int index) {
        this.project = ProjectRef.of(project);
        this.fileEditor = WeakRef.of(fileEditor);
        this.psiFile = PsiFileRef.of(psiFile);
        this.name = psiFile.getName();
        this.icon = psiFile.getIcon();
        this.index = index;
        sqlStatement = sqlStatement.trim();
        executionInput = new StatementExecutionInput(sqlStatement, sqlStatement, this);

        initEditorProviderId(fileEditor);
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return icon;
    }

    private void initEditorProviderId(FileEditor fileEditor) {
        if (fileEditor instanceof BasicTextEditor) {
            BasicTextEditor<?> basicTextEditor = (BasicTextEditor<?>) fileEditor;
            editorProviderId = basicTextEditor.getEditorProviderId();
        }
    }

    @Override
    public boolean isDirty(){
        return Read.call(this, p -> {
            StatementExecutionInput executionInput = p.executionInput;
            if (p.getPsiFile() == null ||
                    getConnection() != executionInput.getConnection() || // connection changed since execution
                    getTargetSchema() != executionInput.getTargetSchemaId()) { // current schema changed since execution)
                return true;
            } else {
                ExecutablePsiElement executablePsiElement = executionInput.getExecutablePsiElement();
                ExecutablePsiElement cachedExecutable = p.getCachedExecutable();
                return executablePsiElement == null ||
                        cachedExecutable == null ||
                        !cachedExecutable.isValid() ||
                        !cachedExecutable.matches(executablePsiElement, BasePsiElement.MatchType.STRONG);
            }
        });
    }

    @Override
    public void bind(ExecutablePsiElement executablePsiElement) {
        cachedExecutable = PsiElementRef.of(executablePsiElement);
        executablePsiElement.setExecutionProcessor(this);
    }

    @Override
    public void unbind() {
        cachedExecutable = null;
    }

    @Override
    public boolean isBound() {
        return getCachedExecutable() != null;
    }

    @Nullable
    @Override
    public DBLanguagePsiFile getPsiFile() {
        DBLanguagePsiFile psiFile = this.psiFile.get();
        if (psiFile == null) {
            ExecutablePsiElement executablePsiElement = WeakRef.get(cachedExecutable);
            if (executablePsiElement != null && executablePsiElement.isValid()) {
                psiFile = executablePsiElement.getFile();
                this.psiFile = PsiFileRef.of(psiFile);
            }
        }
        return psiFile;
    }

    @Nullable
    @Override
    public VirtualFile getVirtualFile() {
        DBLanguagePsiFile psiFile = getPsiFile();
        return psiFile == null ? null : psiFile.getVirtualFile();
    }

    @Nullable
    @Override
    public FileEditor getFileEditor() {
        return fileEditor.get();
    }

    @Override
    @Nullable
    public EditorProviderId getEditorProviderId() {
        return editorProviderId;
    }

    @Override
    @Nullable
    public ExecutablePsiElement getCachedExecutable() {
        return cachedExecutable == null ? null : cachedExecutable.get();
    }

    public static boolean contains(PsiElement parent, BasePsiElement childElement, BasePsiElement.MatchType matchType) {
        PsiElement child = parent.getFirstChild();
        while (child != null) {
            if (child == childElement) {
                return true;
            }
            if (child instanceof ChameleonPsiElement) {
                if (contains(child, childElement, matchType)) {
                    return true;
                }
            } else if(child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                if (basePsiElement.matches(childElement, matchType)) {
                    return true;
                }
            }
            child = child.getNextSibling();
        }
        return false;
    }

    @Override
    public String toString() {
        return executionInput.getOriginalStatementText();
    }

    @Override
    public StatementExecutionInput getExecutionInput() {
        return executionInput;
    }

    @Override
    public StatementExecutionContext getExecutionContext() {
        return executionInput.getExecutionContext();
    }

    @Override
    public StatementExecutionContext initExecutionContext() {
        return executionInput.initExecutionContext();
    }

    @Nullable
    @Override
    public StatementExecutionResult getExecutionResult() {
        if (!Checks.isValid(executionResult)) {
            executionResult = null;
        }
        return executionResult;
    }

    @Override
    public void initExecutionInput(boolean bulkExecution) {
        // overwrite the input if it was leniently bound
        ExecutablePsiElement cachedExecutable = getCachedExecutable();
        if (cachedExecutable != null) {
            executionInput.setOriginalStatementText(cachedExecutable.getText());
            executionInput.setExecutableStatementText(cachedExecutable.prepareStatementText());
            executionInput.setTargetConnection(getConnection());
            executionInput.setTargetSchemaId(getTargetSchema());
            executionInput.setTargetSession(getTargetSession());
            executionInput.setBulkExecution(bulkExecution);
        }

    }

    @Override
    public void execute() throws SQLException {
        execute(null, false);
    }

    @Override
    public void execute(@Nullable DBNConnection connection, boolean debug) throws SQLException {
        ProgressMonitor.setProgressText("Executing " + getStatementName());
        try {
            StatementExecutionContext context = initExecutionContext();
            context.set(EXECUTING, true);

            resultName.reset();
            Documents.refreshEditorAnnotations(getPsiFile());

            String statementText = initStatementText();
            SQLException executionException = null;
            if (statementText != null) {
                try {
                    assertNotCancelled();
                    initConnection(context, connection);
                    initTimeout(context, debug);
                    initLogging(context, debug);

                    executionResult = executeStatement(statementText);

                    // post execution activities
                    if (executionResult != null) {
                        executionResult.calculateExecDuration();
                        consumeLoggerOutput(context);
                        notifyDataDefinitionChanges(context);
                        notifyDataManipulationChanges(context);
                        notifySchemaSelectionChanges(context);
                    }
                } catch (SQLException e) {
                    conditionallyLog(e);
                    Resources.cancel(context.getStatement());
                    if (context.isNot(CANCEL_REQUESTED)) {
                        executionException = e;
                        executionResult = createErrorExecutionResult(e.getMessage());
                        executionResult.calculateExecDuration();
                        consumeLoggerOutput(context);
                    }
                } finally {
                    disableLogging(context);
                }
            }

            assertNotCancelled();
            if (executionResult != null) {
                Project project = getProject();
                ExecutionManager executionManager = ExecutionManager.getInstance(project);
                executionManager.addExecutionResult(executionResult,
                        NavigationInstructions.create(FOCUS, SCROLL, SELECT));
            }

            if (executionException != null && debug) {
                throw executionException;
            }

        } catch (ProcessCanceledException e){
            conditionallyLog(e);
        } finally {
            postExecute();
        }
    }

    private void assertNotCancelled() {
        StatementExecutionContext context = getExecutionContext();
        if (context.is(CANCELLED) && context.is(CANCEL_REQUESTED)) {
            throw new ProcessCanceledException();
        }
    }

    @Override
    public void postExecute() {
        StatementExecutionContext context = getExecutionContext();
        if (context.isNot(PROMPTED)) {
            DBNConnection conn = context.getConnection();
            if (conn != null && conn.isPoolConnection()) {
                Resources.cancel(context.getStatement());
                ConnectionHandler connection = Failsafe.nn(getConnection());
                connection.freePoolConnection(conn);
            }
            context.reset();
        }
    }

    private String initStatementText() {
        ConnectionHandler connection = getTargetConnection();
        String statementText = executionInput.getExecutableStatementText();
        StatementExecutionVariablesBundle executionVariables = executionInput.getExecutionVariables();
        if (executionVariables != null) {
            statementText = executionVariables.prepareStatementText(connection, statementText, false);
            executionInput.setExecutableStatementText(statementText);

            if (executionVariables.hasErrors()) {
                executionResult = createErrorExecutionResult("Could not bind all variables.");
                return null; // cancel execution
            }
        }
        return statementText;
    }

    private void initTimeout(StatementExecutionContext context, boolean debug) {
        int timeout = debug ?
                executionInput.getDebugExecutionTimeout() :
                executionInput.getExecutionTimeout();
        context.setTimeout(timeout);
    }

    private void initConnection(StatementExecutionContext context, DBNConnection conn) throws SQLException {
        ConnectionHandler connection = getTargetConnection();
        if (conn == null) {
            SchemaId schema = getTargetSchema();
            conn = connection.getMainConnection(schema);
        }
        context.setConnection(conn);
    }

    private void initLogging(StatementExecutionContext context, boolean debug) {
        boolean logging = false;
        if (!debug && executionInput.getOptions().is(ExecutionOption.ENABLE_LOGGING) && executionInput.isDatabaseLogProducer()) {
            ConnectionHandler connection = getTargetConnection();
            DBNConnection conn = context.getConnection();

            DatabaseLoggingManager loggingManager = DatabaseLoggingManager.getInstance(getProject());
            logging = loggingManager.enableLogger(connection, conn);
        }
        context.setLogging(logging);

    }

    @Nullable
    private StatementExecutionResult executeStatement(String statementText) throws SQLException {
        StatementExecutionContext context = getExecutionContext();
        assertNotCancelled();

        ConnectionHandler connection = getTargetConnection();
        DBNConnection conn = context.getConnection();
        DBNStatement statement = conn.createStatement();

        statement.setFetchSize(executionInput.getResultSetFetchBlockSize());
        context.setStatement(statement);

        int timeout = context.getTimeout();
        statement.setQueryTimeout(timeout);
        assertNotCancelled();

        databaseCall = new CancellableDatabaseCall<StatementExecutionResult>(connection, conn, timeout, TimeUnit.SECONDS) {
            @Override
            public StatementExecutionResult execute() throws Exception {
                try {
                    statement.execute(statementText);
                    return createExecutionResult(statement, executionInput);
                } finally {
                    databaseCall = null;
                }
            }

            @Override
            public void cancel() {
                try {
                    Resources.cancel(statement);
                } finally {
                    databaseCall = null;
                }
            }
        };
        return databaseCall.start();
    }

    @Override
    public void cancelExecution() {
        StatementExecutionContext context = getExecutionContext();

        context.set(CANCELLED, true);
        context.set(CANCEL_REQUESTED, true);
        StatementExecutionManager executionManager = getExecutionManager();
        StatementExecutionInput executionInput = getExecutionInput();
        SessionId sessionId = executionInput.getTargetSessionId();
        ConnectionId connectionId = executionInput.getConnectionId();
        StatementExecutionQueue queue = Failsafe.nn(executionManager.getExecutionQueue(connectionId, sessionId));
        queue.cancelExecution(this);
        Progress.background(
                getProject(),
                getConnection(),
                false,
                "Cancelling execution",
                "Cancelling statement execution",
                progress -> Safe.run(databaseCall, call -> call.cancelSilently()));
    }

    private void consumeLoggerOutput(StatementExecutionContext context) {
        boolean logging = context.isLogging();
        executionResult.setLoggingActive(logging);
        if (logging) {
            Project project = getProject();
            DBNConnection conn = context.getConnection();
            ConnectionHandler connection = getTargetConnection();

            DatabaseLoggingManager loggingManager = DatabaseLoggingManager.getInstance(project);
            String logOutput = loggingManager.readLoggerOutput(connection, conn);
            executionResult.setLoggingOutput(logOutput);
        }
    }

    private void notifyDataManipulationChanges(StatementExecutionContext context) {
        DBNConnection conn = context.getConnection();
        ConnectionHandler connection = getTargetConnection();
        ExecutablePsiElement psiElement = executionInput.getExecutablePsiElement();
        boolean notifyChanges = false;
        boolean resetChanges = false;

        if (psiElement != null) {
            if (psiElement.isTransactional()) {
                notifyChanges = true;
            }
            else if (psiElement.isTransactionalCandidate()) {
                if (connection.hasPendingTransactions(conn)) {
                    notifyChanges = true;
                }
            }
            else if (psiElement.isTransactionControl()) {
                resetChanges = true;
            }
        } else{
            if (executionResult.getUpdateCount() > 0) {
                notifyChanges = true;
            } else if (connection.hasPendingTransactions(conn)) {
                notifyChanges = true;
            }
        }

        if (notifyChanges) {
            if (conn.isPoolConnection()) {
                StatementExecutionManager executionManager = getExecutionManager();
                executionManager.promptPendingTransactionDialog(this);
            } else {
                VirtualFile virtualFile = getVirtualFile();
                if (virtualFile != null) {
                    conn.notifyDataChanges(virtualFile);
                }
            }
        } else if (resetChanges) {
            if (!conn.isPoolConnection()) {
                conn.resetDataChanges();
            }
        }
    }

    private void notifySchemaSelectionChanges(StatementExecutionContext context) {
        DatabaseSession targetSession = getTargetSession();
        if (targetSession != null && !targetSession.isPool()) {
            ExecutablePsiElement executablePsiElement = getCachedExecutable();
            if (executablePsiElement != null && executablePsiElement.isSchemaChange()) {
                SchemaId schemaId = executablePsiElement.getSchemaChangeTargetId();
                if (schemaId != null) {
                    FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(getProject());
                    contextManager.setDatabaseSchema(getVirtualFile(), schemaId);
                }
            }
        }
    }

    @NotNull
    private StatementExecutionManager getExecutionManager() {
        return StatementExecutionManager.getInstance(getProject());
    }

    private void notifyDataDefinitionChanges(StatementExecutionContext context) {
        Project project = getProject();
        if (isDataDefinitionStatement()) {
            DBSchemaObject affectedObject = getAffectedObject();
            if (affectedObject != null) {
                ProjectEvents.notify(project,
                        DataDefinitionChangeListener.TOPIC,
                        (listener) -> listener.dataDefinitionChanged(affectedObject));
            } else {
                DBSchema affectedSchema = getAffectedSchema();
                IdentifierPsiElement subjectPsiElement = getSubjectPsiElement();
                if (affectedSchema != null && subjectPsiElement != null) {
                    DBObjectType objectType = subjectPsiElement.getObjectType();
                    ProjectEvents.notify(project,
                            DataDefinitionChangeListener.TOPIC,
                            (listener) -> listener.dataDefinitionChanged(affectedSchema, objectType));
                }
            }
        }
    }

    private void disableLogging(StatementExecutionContext context) {
        DBNConnection conn = context.getConnection();
        ConnectionHandler connection = getTargetConnection();
        if (context.isLogging()) {
            Project project = getProject();
            DatabaseLoggingManager loggingManager = DatabaseLoggingManager.getInstance(project);
            loggingManager.disableLogger(connection, conn);
        }
    }

    @Override
    @Nullable
    public StatementExecutionVariablesBundle getExecutionVariables() {
        return executionInput.getExecutionVariables();
    }

    @NotNull
    protected StatementExecutionResult createExecutionResult(DBNStatement statement, final StatementExecutionInput executionInput) throws SQLException {
        StatementExecutionBasicResult executionResult = new StatementExecutionBasicResult(this, getResultName(), statement.getUpdateCount());
        Resources.close(statement);
        attachDdlExecutionInfo(executionInput, executionResult);
        return executionResult;
    }

    private void attachDdlExecutionInfo(StatementExecutionInput executionInput, final StatementExecutionBasicResult executionResult) {
        boolean isDdlStatement = isDataDefinitionStatement();
        boolean hasCompilerErrors = false;
        ConnectionHandler connection = executionInput.getConnection();
        if (isDdlStatement && DatabaseFeature.OBJECT_INVALIDATION.isSupported(connection)) {
            BasePsiElement compilablePsiElement = getCompilableBlockPsiElement();
            if (compilablePsiElement == null)  return;

            hasCompilerErrors = Read.call(() -> {
                DBContentType contentType = getCompilableContentType();
                CompilerAction compilerAction = new CompilerAction(CompilerActionSource.DDL, contentType, getVirtualFile(), getFileEditor());
                compilerAction.setSourceStartOffset(compilablePsiElement.getTextOffset());

                DBSchemaObject object = getAffectedObject();
                CompilerResult compilerResult = null;
                if (object == null) {
                    DBSchema schema = getAffectedSchema();
                    IdentifierPsiElement subjectPsiElement = getSubjectPsiElement();
                    if (schema != null && subjectPsiElement != null) {
                        DBObjectType objectType = subjectPsiElement.getObjectType();
                        String objectName = subjectPsiElement.getUnquotedText().toString().toUpperCase();
                        compilerResult = new CompilerResult(compilerAction, connection, schema, objectType, objectName);
                    }
                } else {
                    compilerResult = new CompilerResult(compilerAction, object);
                }

                if (compilerResult != null) {
                    if (object != null) {
                        Project project = getProject();
                        DatabaseCompilerManager compilerManager = DatabaseCompilerManager.getInstance(project);
                        if (object.is(COMPILABLE)) {
                            CompileType compileType = compilerManager.getCompileType(object, contentType);
                            if (compileType == CompileType.DEBUG) {
                                compilerManager.compileObject(object, compileType, compilerAction);
                            }
                            ProjectEvents.notify(project,
                                    CompileManagerListener.TOPIC,
                                    (listener) -> listener.compileFinished(connection, object));
                        }
                        object.refresh();
                    }

                    executionResult.setCompilerResult(compilerResult);
                    return compilerResult.hasErrors();
                }
                return false;
            });
        }

        if (hasCompilerErrors) {
            String message = executionInput.getStatementDescription() + " executed with warnings";
            executionResult.updateExecutionMessage(MessageType.WARNING, message);
            executionResult.setExecutionStatus(StatementExecutionStatus.WARNING);

        } else {
            String message = executionInput.getStatementDescription() + " executed successfully";
            int updateCount = executionResult.getUpdateCount();
            if (!isDdlStatement && updateCount > -1) {
                message = message + ": " + updateCount + (updateCount != 1 ? " rows" : " row") + " affected";
            }
            executionResult.updateExecutionMessage(MessageType.INFO, message);
            executionResult.setExecutionStatus(StatementExecutionStatus.SUCCESS);
        }
    }


    private StatementExecutionResult createErrorExecutionResult(String cause) {
        StatementExecutionResult executionResult = new StatementExecutionBasicResult(this, getResultName(), 0);
        executionResult.updateExecutionMessage(MessageType.ERROR, "Error executing " + getStatementName() + '.', cause);
        executionResult.setExecutionStatus(StatementExecutionStatus.ERROR);
        return executionResult;
    }

    @Override
    @Nullable
    public ConnectionHandler getConnection() {
        DBLanguagePsiFile psiFile = getPsiFile();
        return psiFile == null ? null : psiFile.getConnection();
    }

    @Override
    @NotNull
    public ConnectionHandler getTargetConnection() {
        return Failsafe.nn(getConnection());
    }

    @Override
    @Nullable
    public SchemaId getTargetSchema() {
        DatabaseSession targetSession = getTargetSession();
        if (targetSession != null && targetSession.isPool()) {
            ExecutablePsiElement cachedExecutable = getCachedExecutable();
            if (cachedExecutable != null) {
                SchemaId schemaId = cachedExecutable.getContextSchema();
                if (schemaId != null) {
                    return schemaId;
                }
            }
        }

        DBLanguagePsiFile psiFile = getPsiFile();
        return psiFile == null ? null : psiFile.getSchemaId();
    }

    @Override
    @Nullable
    public DatabaseSession getTargetSession() {
        DBLanguagePsiFile psiFile = getPsiFile();
        return psiFile == null ? null : psiFile.getSession();
    }

    @Override
    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @Override
    @NotNull
    public String getResultName() {
        return resultName.get();
    }

    public void setResultName(String resultName, boolean sticky) {
        this.resultName.set(resultName);
        stickyResultName = sticky ? resultName : null;
    }

    @Override
    public String getStatementName() {
        ExecutablePsiElement executablePsiElement = executionInput.getExecutablePsiElement();
        return executablePsiElement == null ? "SQL statement" : executablePsiElement.getSpecificElementType().getDescription();
    }

    public int getIndex() {
        return index;
    }

    public boolean canExecute() {
        return !isDisposed();
    }

    @Override
    public void navigateToResult() {
        StatementExecutionResult executionResult = getExecutionResult();
        if (executionResult != null) {
            ExecutionManager executionManager = ExecutionManager.getInstance(getProject());
            executionManager.selectExecutionResult(executionResult);
        }
    }

    @Override
    public void navigateToEditor(NavigationInstructions instructions) {
        FileEditor fileEditor = getFileEditor();
        ExecutablePsiElement cachedExecutable = getCachedExecutable();
        if (cachedExecutable != null) {
            if (fileEditor != null) {
                cachedExecutable.navigateInEditor(fileEditor, instructions);
            } else {
                cachedExecutable.navigate(instructions.isFocus());
            }
        }
    }

    /********************************************************
     *                    Disposable                        *
     ********************************************************/
    private boolean isDataDefinitionStatement() {
        ExecutablePsiElement cachedExecutable = getCachedExecutable();
        return cachedExecutable != null && cachedExecutable.is(ElementTypeAttribute.DATA_DEFINITION);
    }

    @Nullable
    private DBSchemaObject getAffectedObject() {
        if (!isDataDefinitionStatement()) return null;

        IdentifierPsiElement subjectPsiElement = getSubjectPsiElement();
        if (subjectPsiElement == null) return null;

        SchemaId targetSchema = getTargetSchema();
        if (targetSchema == null) return null;

        ConnectionHandler connection = getConnection();
        if (connection == null) return null;

        DBObject schemaObject = connection.getSchema(targetSchema);
        if (schemaObject == null) return null;

        DBObjectListContainer childObjects = schemaObject.getChildObjects();
        if (childObjects == null) return null;

        DBObjectType objectType = subjectPsiElement.getObjectType();
        DBObjectList objectList = childObjects.getObjectList(objectType);
        if (objectList == null) return null;

        return (DBSchemaObject) objectList.getObject(subjectPsiElement.getText());
    }

    @Nullable
    private DBSchema getAffectedSchema() {
        if (isDataDefinitionStatement()) {
            IdentifierPsiElement subjectPsiElement = getSubjectPsiElement();
            if (subjectPsiElement != null) {
                PsiElement parent = subjectPsiElement.getParent();
                if (parent instanceof QualifiedIdentifierPsiElement) {
                    QualifiedIdentifierPsiElement qualifiedIdentifierPsiElement = (QualifiedIdentifierPsiElement) parent;
                    DBObject parentObject = qualifiedIdentifierPsiElement.lookupParentObjectFor(subjectPsiElement.getElementType());
                    if (parentObject instanceof DBSchema) {
                        return (DBSchema) parentObject;
                    }
                }
            }
        }
        ConnectionHandler connection = getConnection();
        SchemaId targetSchema = getTargetSchema();
        return connection == null || targetSchema == null ? null : connection.getSchema(targetSchema);
    }


    @Nullable
    private IdentifierPsiElement getSubjectPsiElement() {
        ExecutablePsiElement cachedExecutable = getCachedExecutable();
        return cachedExecutable == null ? null : (IdentifierPsiElement) cachedExecutable.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
    }

    private BasePsiElement getCompilableBlockPsiElement() {
        ExecutablePsiElement cachedExecutable = getCachedExecutable();
        return cachedExecutable == null ? null : cachedExecutable.findFirstPsiElement(ElementTypeAttribute.COMPILABLE_BLOCK);
    }

    private DBContentType getCompilableContentType() {
        BasePsiElement compilableBlockPsiElement = getCompilableBlockPsiElement();
        if (compilableBlockPsiElement != null) {
            //if (compilableBlockPsiElement.is(ElementTypeAttribute.OBJECT_DEFINITION)) return DBContentType.CODE;
            if (compilableBlockPsiElement.is(ElementTypeAttribute.OBJECT_SPECIFICATION)) return DBContentType.CODE_SPEC;
            if (compilableBlockPsiElement.is(ElementTypeAttribute.OBJECT_DECLARATION)) return DBContentType.CODE_BODY;
        }
        return DBContentType.CODE;
    }

    @Override
    public boolean isQuery() {
        return false;
    }

    @Override
    public int getExecutableLineNumber() {
        ExecutablePsiElement cachedExecutable = getCachedExecutable();
        if (cachedExecutable != null) {
            Document document = Documents.getDocument(cachedExecutable.getFile());
            if (document != null) {
                int textOffset = cachedExecutable.getTextOffset();
                return document.getLineNumber(textOffset);
            }
        }

        return 0;
    }


    @Override
    protected void disposeInner() {
        nullify();
    }
}
