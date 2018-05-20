package com.dci.intellij.dbn.execution.statement;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.dispose.FailsafeUtil;
import com.dci.intellij.dbn.common.message.MessageCallback;
import com.dci.intellij.dbn.common.notification.NotificationUtil;
import com.dci.intellij.dbn.common.thread.BackgroundTask;
import com.dci.intellij.dbn.common.thread.RunnableTask;
import com.dci.intellij.dbn.common.thread.SimpleLaterInvocator;
import com.dci.intellij.dbn.common.thread.SimpleTask;
import com.dci.intellij.dbn.common.util.DocumentUtil;
import com.dci.intellij.dbn.common.util.EditorUtil;
import com.dci.intellij.dbn.common.util.EventUtil;
import com.dci.intellij.dbn.common.util.MessageUtil;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionProvider;
import com.dci.intellij.dbn.connection.SessionId;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.connection.mapping.FileConnectionMappingManager;
import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.dci.intellij.dbn.editor.console.SQLConsoleEditor;
import com.dci.intellij.dbn.editor.ddl.DDLFileEditor;
import com.dci.intellij.dbn.execution.ExecutionContext;
import com.dci.intellij.dbn.execution.common.options.ExecutionEngineSettings;
import com.dci.intellij.dbn.execution.statement.options.StatementExecutionSettings;
import com.dci.intellij.dbn.execution.statement.processor.StatementExecutionBasicProcessor;
import com.dci.intellij.dbn.execution.statement.processor.StatementExecutionCursorProcessor;
import com.dci.intellij.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dci.intellij.dbn.execution.statement.result.ui.PendingTransactionDialog;
import com.dci.intellij.dbn.execution.statement.variables.StatementExecutionVariable;
import com.dci.intellij.dbn.execution.statement.variables.StatementExecutionVariablesBundle;
import com.dci.intellij.dbn.execution.statement.variables.StatementExecutionVariablesCache;
import com.dci.intellij.dbn.execution.statement.variables.ui.StatementExecutionInputsDialog;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement.MatchType;
import com.dci.intellij.dbn.language.common.psi.ChameleonPsiElement;
import com.dci.intellij.dbn.language.common.psi.ExecVariablePsiElement;
import com.dci.intellij.dbn.language.common.psi.ExecutablePsiElement;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.dci.intellij.dbn.language.common.psi.RootPsiElement;
import com.dci.intellij.dbn.object.DBSchema;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiDocumentTransactionListener;
import gnu.trove.THashSet;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dci.intellij.dbn.execution.ExecutionStatus.EXECUTING;
import static com.dci.intellij.dbn.execution.ExecutionStatus.PROMPTED;
import static com.dci.intellij.dbn.execution.ExecutionStatus.QUEUED;

@State(
    name = StatementExecutionManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class StatementExecutionManager extends AbstractProjectComponent implements PersistentStateComponent<Element> {
    public static final String COMPONENT_NAME = "DBNavigator.Project.StatementExecutionManager";

    private static final String[] OPTIONS_MULTIPLE_STATEMENT_EXEC = new String[]{"Execute All", "Execute All from Caret", "Cancel"};

    private final Map<FileEditor, List<StatementExecutionProcessor>> fileExecutionProcessors = new HashMap<FileEditor, List<StatementExecutionProcessor>>();
    private final StatementExecutionVariablesCache variablesCache = new StatementExecutionVariablesCache();
    private Map<SessionId, StatementExecutionQueue> executionQueues = new HashMap<SessionId, StatementExecutionQueue>();

    private static final AtomicInteger RESULT_SEQUENCE = new AtomicInteger(0);

    private StatementExecutionManager(Project project) {
        super(project);
        EventUtil.subscribe(project, this, PsiDocumentTransactionListener.TOPIC, psiDocumentTransactionListener);
        EventUtil.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener);
    }

    public StatementExecutionQueue getExecutionQueue(SessionId sessionId) {
        StatementExecutionQueue executionQueue = executionQueues.get(sessionId);
        if (executionQueue == null) {
            synchronized (this) {
                executionQueue = executionQueues.get(sessionId);
                if (executionQueue == null) {
                    executionQueue = new StatementExecutionQueue(StatementExecutionManager.this) {
                        @Override
                        protected void execute(StatementExecutionProcessor processor) {
                            process(processor);
                        }
                    };
                    executionQueues.put(sessionId, executionQueue);
                }
            }
        }
        return executionQueue;
    }

    public static StatementExecutionManager getInstance(@NotNull Project project) {
        return FailsafeUtil.getComponent(project, StatementExecutionManager.class);
    }

    public void cacheVariable(VirtualFile virtualFile, StatementExecutionVariable variable) {
        variablesCache.cacheVariable(virtualFile, variable);
    }

    public StatementExecutionVariablesCache getVariablesCache() {
        return variablesCache;
    }

    private PsiDocumentTransactionListener psiDocumentTransactionListener = new PsiDocumentTransactionListener() {
        @Override
        public void transactionStarted(@NotNull Document document, @NotNull PsiFile file) {}

        @Override
        public void transactionCompleted(@NotNull Document document, @NotNull PsiFile file) {
            Project project = file.getProject();
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile.isInLocalFileSystem()) {
                List<FileEditor> scriptFileEditors = EditorUtil.getScriptFileEditors(project, virtualFile);
                for (FileEditor scriptFileEditor : scriptFileEditors) {
                    refreshEditorExecutionProcessors(scriptFileEditor);
                }
            } else {
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                FileEditor[] fileEditors = fileEditorManager.getAllEditors(virtualFile);
                for (FileEditor fileEditor : fileEditors) {
                    if (fileEditor instanceof DDLFileEditor || fileEditor instanceof SQLConsoleEditor) {
                        refreshEditorExecutionProcessors(fileEditor);
                    }
                }
            }
        }
    };

    private FileEditorManagerListener fileEditorManagerListener = new FileEditorManagerAdapter() {
        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {

        }
    };

    private void refreshEditorExecutionProcessors(FileEditor textEditor) {
        Collection<StatementExecutionProcessor> executionProcessors = getExecutionProcessors(textEditor);
        if (!executionProcessors.isEmpty()) {
            for (StatementExecutionProcessor executionProcessor : executionProcessors) {
                executionProcessor.unbind();
            }

            bindExecutionProcessors(textEditor, MatchType.STRONG);
            bindExecutionProcessors(textEditor, MatchType.CACHED);
            bindExecutionProcessors(textEditor, MatchType.SOFT);

            List<StatementExecutionProcessor> removeList = null;
            for (StatementExecutionProcessor executionProcessor : executionProcessors) {
                if (executionProcessor.getCachedExecutable() == null) {
                    if (removeList == null) removeList = new ArrayList<StatementExecutionProcessor>();
                    removeList.add(executionProcessor);
                }
            }

            if (removeList != null) {
                executionProcessors.removeAll(removeList);
            }
        }
    }

    @NotNull
    private List<StatementExecutionProcessor> getExecutionProcessors(FileEditor textEditor) {
        List<StatementExecutionProcessor> executionProcessors = fileExecutionProcessors.get(textEditor);
        if (executionProcessors == null) {
            executionProcessors = new CopyOnWriteArrayList<StatementExecutionProcessor>();
            fileExecutionProcessors.put(textEditor, executionProcessors);
        }
        return executionProcessors;
    }

    private void bindExecutionProcessors(FileEditor fileEditor, MatchType matchType) {
        Editor editor = EditorUtil.getEditor(fileEditor);
        PsiFile psiFile = DocumentUtil.getFile(editor);
        if (psiFile != null) {
            PsiElement child = psiFile.getFirstChild();
            while (child != null) {
                if (child instanceof RootPsiElement) {
                    RootPsiElement root = (RootPsiElement) child;
                    for (ExecutablePsiElement executable: root.getExecutablePsiElements()) {
                        if (matchType == MatchType.CACHED) {
                            StatementExecutionProcessor executionProcessor = executable.getExecutionProcessor();
                            if (executionProcessor != null && !executionProcessor.isBound() && executionProcessor.isQuery() == executable.isQuery()) {
                                executionProcessor.bind(executable);
                            }
                        } else {
                            StatementExecutionProcessor executionProcessor = findExecutionProcessor(executable, fileEditor, matchType);
                            if (executionProcessor != null) {
                                executionProcessor.bind(executable);
                            }
                        }
                    }
                }
                child = child.getNextSibling();
            }
        }
    }

    private StatementExecutionProcessor findExecutionProcessor(ExecutablePsiElement executablePsiElement, FileEditor fileEditor, MatchType matchType) {
        Collection<StatementExecutionProcessor> executionProcessors = getExecutionProcessors(fileEditor);

        for (StatementExecutionProcessor executionProcessor : executionProcessors) {
            if (!executionProcessor.isBound()) {
                ExecutablePsiElement execPsiElement = executionProcessor.getExecutionInput().getExecutablePsiElement();
                if (execPsiElement != null && execPsiElement.matches(executablePsiElement, matchType)) {
                    return executionProcessor;
                }
            }
        }
        return null;
    }

    /*********************************************************
     *                       Execution                       *
     *********************************************************/
    public void debugExecute(@NotNull StatementExecutionProcessor executionProcessor, @NotNull DBNConnection connection) throws SQLException {
        try {
            executionProcessor.execute(connection, true);
        } finally {
            DBLanguagePsiFile file = executionProcessor.getPsiFile();
            DocumentUtil.refreshEditorAnnotations(file);
        }
    }

    public void executeStatement(final @NotNull StatementExecutionProcessor executionProcessor) {
        executeStatements(executionProcessor.asList(), executionProcessor.getVirtualFile());
    }

    private void executeStatements(final List<StatementExecutionProcessor> executionProcessors, final VirtualFile virtualFile) {
        final int size = executionProcessors.size();
        if (size > 0) {
            final FileConnectionMappingManager connectionMappingManager = FileConnectionMappingManager.getInstance(getProject());
            ConnectionProvider connectionProvider = new ConnectionProvider() {
                @Nullable
                @Override
                public ConnectionHandler getConnectionHandler() {
                    return connectionMappingManager.getConnectionHandler(virtualFile);
                }
            };

            ConnectionAction executionTask = new ConnectionAction("the statement execution", connectionProvider) {
                @Override
                protected void execute() {
                    SimpleTask executionCallback = new SimpleTask() {
                        @Override
                        protected void execute() {
                            for (final StatementExecutionProcessor executionProcessor : executionProcessors) {
                                ExecutionContext context = executionProcessor.getExecutionContext();
                                StatementExecutionInput executionInput = executionProcessor.getExecutionInput();
                                SessionId sessionId = executionInput.getTargetSessionId();
                                if (context.isNot(EXECUTING) && context.isNot(QUEUED)) {
                                    if (sessionId == SessionId.POOL) {
                                        new BackgroundTask(getProject(), "Executing statement", true, true) {
                                            @Override
                                            protected void execute(@NotNull ProgressIndicator progressIndicator) {
                                                process(executionProcessor);
                                            }
                                        }.start();
                                    } else {
                                        StatementExecutionQueue executionQueue = getExecutionQueue(sessionId);
                                        if (!executionQueue.contains(executionProcessor)) {
                                            executionQueue.queue(executionProcessor);
                                        }
                                    }
                                }
                            }
                        }
                    };

                    promptExecutionDialogs(executionProcessors, DBDebuggerType.NONE, executionCallback);
                }
            };

            DBLanguagePsiFile file =  executionProcessors.get(0).getPsiFile();
            connectionMappingManager.selectConnectionAndSchema(file, executionTask);
        }
    }

    private void process(StatementExecutionProcessor executionProcessor) {
        try {
            StatementExecutionInput executionInput = executionProcessor.getExecutionInput();
            DBSchema schema = executionInput.getTargetSchema();
            ConnectionHandler connectionHandler = FailsafeUtil.get(executionProcessor.getConnectionHandler());
            DBNConnection connection = connectionHandler.getConnection(executionInput.getTargetSessionId(), schema);
            executionProcessor.execute(connection, false);

        } catch (SQLException e) {
            NotificationUtil.sendErrorNotification(getProject(), "Error executing " + executionProcessor.getStatementName(), e.getMessage());
        } finally {
            DocumentUtil.refreshEditorAnnotations(executionProcessor.getPsiFile());
        }
    }

    public void executeStatementAtCursor(final FileEditor fileEditor) {
        final Editor editor = EditorUtil.getEditor(fileEditor);
        if (editor != null) {
            StatementExecutionProcessor executionProcessor = getExecutionProcessorAtCursor(fileEditor);
            if (executionProcessor != null) {
                executeStatement(executionProcessor);
            } else {
                MessageUtil.showQuestionDialog(
                        getProject(),
                        "Multiple statement execution",
                        "No statement found under the caret. \nExecute all statements in the file or just the ones after the cursor?",
                        OPTIONS_MULTIPLE_STATEMENT_EXEC, 0, new MessageCallback() {
                            @Override
                            protected void execute() {
                                int option = getData();
                                if (option == 0 || option == 1) {
                                    int offset = option == 0 ? 0 : editor.getCaretModel().getOffset();
                                    List<StatementExecutionProcessor> executionProcessors = getExecutionProcessorsFromOffset(fileEditor, offset);
                                    final VirtualFile virtualFile = DocumentUtil.getVirtualFile(editor);
                                    executeStatements(executionProcessors, virtualFile);
                                }
                            }
                        });
            }
        }

    }

    public void promptExecutionDialog(@NotNull StatementExecutionProcessor executionProcessor, final DBDebuggerType debuggerType, @NotNull final RunnableTask callback) {
        promptExecutionDialogs(executionProcessor.asList(), debuggerType, callback);

    }

    private void promptExecutionDialogs(@NotNull final List<StatementExecutionProcessor> processors, final DBDebuggerType debuggerType, @NotNull final RunnableTask callback) {
        new SimpleLaterInvocator() {
            @Override
            protected void execute() {
                if (promptExecutionDialogs(processors, debuggerType)) {
                    callback.start();
                }
            }
        }.start();

    }

    private boolean promptExecutionDialogs(@NotNull List<StatementExecutionProcessor> executionProcessors, DBDebuggerType debuggerType) {
        Map<String, StatementExecutionVariable> variableCache = new HashMap<String, StatementExecutionVariable>();
        boolean reuseVariables = false;
        boolean bulkExecution = executionProcessors.size() > 1;

        StatementExecutionSettings executionSettings = ExecutionEngineSettings.getInstance(getProject()).getStatementExecutionSettings();

        for (StatementExecutionProcessor executionProcessor : executionProcessors) {
            executionProcessor.initExecutionInput(bulkExecution);
            StatementExecutionInput executionInput = executionProcessor.getExecutionInput();
            Set<ExecVariablePsiElement> bucket = new THashSet<ExecVariablePsiElement>();
            ExecutablePsiElement executablePsiElement = executionInput.getExecutablePsiElement();
            if (executablePsiElement != null) {
                executablePsiElement.collectExecVariablePsiElements(bucket);
            }

            StatementExecutionVariablesBundle executionVariables = executionInput.getExecutionVariables();
            if (bucket.isEmpty()) {
                executionVariables = null;
                executionInput.setExecutionVariables(null);
            } else {
                if (executionVariables == null){
                    executionVariables = new StatementExecutionVariablesBundle(bucket);
                    executionInput.setExecutionVariables(executionVariables);
                }
                executionVariables.initialize(bucket);
            }

            if (executionVariables != null) {
                if (reuseVariables) {
                    executionVariables.populate(variableCache, true);
                }

                if (!(reuseVariables && executionVariables.isProvided())) {
                    StatementExecutionInputsDialog dialog = new StatementExecutionInputsDialog(executionProcessor, debuggerType, bulkExecution);
                    dialog.show();
                    if (dialog.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
                        return false;
                    } else {
                        reuseVariables = dialog.isReuseVariables();
                        if (reuseVariables) {
                            Set<StatementExecutionVariable> variables = executionVariables.getVariables();
                            for (StatementExecutionVariable variable : variables) {
                                variableCache.put(variable.getName().toUpperCase(), variable);
                            }
                        } else {
                            variableCache.clear();
                        }
                    }
                }
            } else if (executionSettings.isPromptExecution() || debuggerType.isDebug()) {
                StatementExecutionInputsDialog dialog = new StatementExecutionInputsDialog(executionProcessor, debuggerType, bulkExecution);
                dialog.show();
                if (dialog.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
                    return false;
                }
            }
        }

        return true;
    }

    public void promptPendingTransactionDialog(final StatementExecutionProcessor executionProcessor) {
        final ExecutionContext context = executionProcessor.getExecutionContext();
        context.set(PROMPTED, true);
        new SimpleLaterInvocator() {
            @Override
            protected void execute() {
                try {
                    PendingTransactionDialog dialog = new PendingTransactionDialog(executionProcessor);
                    dialog.show();
                } finally {
                    executionProcessor.postExecute();
                    context.set(PROMPTED, false);
                }
            }
        }.start();
    }

    @Nullable
    private StatementExecutionProcessor getExecutionProcessorAtCursor(FileEditor fileEditor) {
        Editor editor = EditorUtil.getEditor(fileEditor);
        if (editor != null) {
            DBLanguagePsiFile file = (DBLanguagePsiFile) DocumentUtil.getFile(editor);
            String selection = editor.getSelectionModel().getSelectedText();
            if (selection != null) {
                return new StatementExecutionCursorProcessor(fileEditor, file, selection, RESULT_SEQUENCE.incrementAndGet());
            }

            ExecutablePsiElement executablePsiElement = PsiUtil.lookupExecutableAtCaret(editor, true);
            if (executablePsiElement != null) {
                return getExecutionProcessor(fileEditor, executablePsiElement, true);
            }
        }
        return null;
    }

    public List<StatementExecutionProcessor> getExecutionProcessorsFromOffset(FileEditor fileEditor, int offset) {
        List<StatementExecutionProcessor> executionProcessors = new ArrayList<StatementExecutionProcessor>();
        Editor editor = EditorUtil.getEditor(fileEditor);

        if (editor != null) {
            DBLanguagePsiFile file = (DBLanguagePsiFile) DocumentUtil.getFile(editor);
            if (file != null) {
                PsiElement child = file.getFirstChild();
                while (child != null) {
                    if (child instanceof ChameleonPsiElement) {
                        ChameleonPsiElement chameleonPsiElement = (ChameleonPsiElement) child;
                        for (ExecutablePsiElement executable : chameleonPsiElement.getExecutablePsiElements()) {
                            StatementExecutionProcessor executionProcessor = getExecutionProcessor(fileEditor, executable, true);
                            executionProcessors.add(executionProcessor);
                        }

                    }
                    if (child instanceof RootPsiElement) {
                        RootPsiElement root = (RootPsiElement) child;

                        for (ExecutablePsiElement executable: root.getExecutablePsiElements()) {
                            if (executable.getTextOffset() > offset) {
                                StatementExecutionProcessor executionProcessor = getExecutionProcessor(fileEditor, executable, true);
                                executionProcessors.add(executionProcessor);
                            }
                        }
                    }
                    child = child.getNextSibling();
                }
            }
        }
        return executionProcessors;
    }

    @Nullable
    public StatementExecutionProcessor getExecutionProcessor(FileEditor fileEditor, ExecutablePsiElement executablePsiElement, boolean create) {
        List<StatementExecutionProcessor> executionProcessors = getExecutionProcessors(fileEditor);
        for (StatementExecutionProcessor executionProcessor : executionProcessors) {
            if (executablePsiElement == executionProcessor.getCachedExecutable()) {
                return executionProcessor;
            }
        }

        return create ? createExecutionProcessor(fileEditor, executionProcessors, executablePsiElement) : null;
    }

    private StatementExecutionProcessor createExecutionProcessor(FileEditor fileEditor, List<StatementExecutionProcessor> executionProcessors, ExecutablePsiElement executablePsiElement) {
        StatementExecutionBasicProcessor executionProcessor =
                executablePsiElement.isQuery() ?
                        new StatementExecutionCursorProcessor(fileEditor, executablePsiElement, RESULT_SEQUENCE.incrementAndGet()) :
                        new StatementExecutionBasicProcessor(fileEditor, executablePsiElement, RESULT_SEQUENCE.incrementAndGet());
        executionProcessors.add(executionProcessor);
        executablePsiElement.setExecutionProcessor(executionProcessor);
        return executionProcessor;
    }

    /*********************************************************
     *                    ProjectComponent                   *
     *********************************************************/
    @NotNull
    @NonNls
    public String getComponentName() {
        return COMPONENT_NAME;
    }


    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("state");
        variablesCache.writeState(element);
        return element;
    }

    @Override
    public void loadState(Element element) {
        if (element != null) {
            variablesCache.readState(element);
        }
    }
}
