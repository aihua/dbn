package com.dci.intellij.dbn.execution.script;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.component.PersistentState;
import com.dci.intellij.dbn.common.component.ProjectComponentBase;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.routine.Consumer;
import com.dci.intellij.dbn.common.thread.CancellableDatabaseCall;
import com.dci.intellij.dbn.common.thread.Progress;
import com.dci.intellij.dbn.common.util.Messages;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.common.util.Unsafe;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.DatabaseType;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.connection.mapping.FileConnectionContextManager;
import com.dci.intellij.dbn.database.CmdLineExecutionInput;
import com.dci.intellij.dbn.database.interfaces.DatabaseExecutionInterface;
import com.dci.intellij.dbn.execution.ExecutionManager;
import com.dci.intellij.dbn.execution.common.options.ExecutionEngineSettings;
import com.dci.intellij.dbn.execution.logging.LogOutput;
import com.dci.intellij.dbn.execution.logging.LogOutputContext;
import com.dci.intellij.dbn.execution.script.options.ScriptExecutionSettings;
import com.dci.intellij.dbn.execution.script.ui.CmdLineInterfaceInputDialog;
import com.dci.intellij.dbn.execution.script.ui.ScriptExecutionInputDialog;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.LineReader;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.jdesktop.swingx.util.OS;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.dci.intellij.dbn.common.component.Components.projectService;
import static com.dci.intellij.dbn.common.dispose.Failsafe.conditionallyLog;
import static com.dci.intellij.dbn.common.message.MessageCallback.when;
import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.*;
import static com.dci.intellij.dbn.execution.ExecutionStatus.EXECUTING;

@Getter
@Setter
@State(
    name = ScriptExecutionManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ScriptExecutionManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ScriptExecutionManager";

    private static final SecureRandom TMP_FILE_RANDOMIZER = new SecureRandom();
    private final ExecutionManager executionManager;
    private final Map<VirtualFile, Process> activeProcesses = new ConcurrentHashMap<>();
    private final Map<DatabaseType, String> recentlyUsedInterfaces = new EnumMap<>(DatabaseType.class);
    private boolean clearOutputOption = true;

    private ScriptExecutionManager(Project project) {
        super(project, COMPONENT_NAME);
        executionManager = ExecutionManager.getInstance(project);
    }

    public static ScriptExecutionManager getInstance(@NotNull Project project) {
        return projectService(project, ScriptExecutionManager.class);
    }

    public List<CmdLineInterface> getAvailableInterfaces(DatabaseType databaseType) {
        ExecutionEngineSettings executionEngineSettings = ExecutionEngineSettings.getInstance(getProject());
        CmdLineInterfaceBundle commandLineInterfaces = executionEngineSettings.getScriptExecutionSettings().getCommandLineInterfaces();
        List<CmdLineInterface> interfaces = commandLineInterfaces.getInterfaces(databaseType);
        CmdLineInterface defaultInterface = CmdLineInterface.getDefault(databaseType);
        if (defaultInterface != null) {
            interfaces.add(0, defaultInterface);
        }
        return interfaces;
    }


    public void executeScript(VirtualFile virtualFile) {
        Project project = getProject();
        if (activeProcesses.containsKey(virtualFile)) {
            Messages.showInfoDialog(project, "Information", "SQL Script \"" + virtualFile.getPath() + "\" is already running. \nWait for the execution to finish before running again.");
        } else {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);

            ConnectionHandler activeConnection = contextManager.getConnection(virtualFile);
            SchemaId currentSchema = contextManager.getDatabaseSchema(virtualFile);

            ScriptExecutionInput executionInput = new ScriptExecutionInput(getProject(), virtualFile, activeConnection, currentSchema, clearOutputOption);
            ScriptExecutionSettings scriptExecutionSettings = ExecutionEngineSettings.getInstance(project).getScriptExecutionSettings();
            int timeout = scriptExecutionSettings.getExecutionTimeout();
            executionInput.setExecutionTimeout(timeout);

            ScriptExecutionInputDialog inputDialog = new ScriptExecutionInputDialog(project,executionInput);

            inputDialog.show();
            if (inputDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                ConnectionHandler connection = executionInput.getConnection();
                SchemaId schemaId = executionInput.getSchemaId();
                CmdLineInterface cmdLineExecutable = executionInput.getCmdLineInterface();
                contextManager.setConnection(virtualFile, connection);
                contextManager.setDatabaseSchema(virtualFile, schemaId);
                if (connection != null) {
                    recentlyUsedInterfaces.put(connection.getDatabaseType(), cmdLineExecutable.getId());
                }
                clearOutputOption = executionInput.isClearOutput();

                Progress.prompt(project, connection, true,
                        "Executing script",
                        "Executing database script \"" + virtualFile.getName() + "\"",
                        progress -> {
                            try {
                                doExecuteScript(executionInput);
                            } catch (Exception e) {
                                Messages.showErrorDialog(getProject(), "Error",
                                        "Error executing SQL Script \"" + virtualFile.getPath() + "\". " + e.getMessage());
                            }
                        });
            }
        }
    }

    private void doExecuteScript(ScriptExecutionInput input) throws Exception {
        ScriptExecutionContext context = input.getExecutionContext();
        context.set(EXECUTING, true);
        ConnectionHandler connection = Failsafe.nn(input.getConnection());
        VirtualFile sourceFile = input.getSourceFile();
        activeProcesses.remove(sourceFile, null);

        Project project = getProject();
        AtomicReference<File> tempScriptFile = new AtomicReference<>();
        LogOutputContext outputContext = new LogOutputContext(connection, sourceFile, null);
        int timeout = input.getExecutionTimeout();
        executionManager.writeLogOutput(outputContext, LogOutput.createSysOutput(outputContext, " - Initializing script execution", input.isClearOutput()));

        try {
            new CancellableDatabaseCall<Object>(connection, null, timeout, TimeUnit.SECONDS) {
                @Override
                public Object execute() throws Exception {
                    SchemaId schemaId = input.getSchemaId();

                    String content = new String(sourceFile.contentsToByteArray());
                    File temporaryScriptFile = createTempScriptFile();

                    executionManager.writeLogOutput(outputContext, LogOutput.createSysOutput("Creating temporary script file " + temporaryScriptFile));
                    tempScriptFile.set(temporaryScriptFile);

                    DatabaseExecutionInterface executionInterface = connection.getInterfaces().getExecutionInterface();
                    CmdLineInterface cmdLineInterface = input.getCmdLineInterface();
                    CmdLineExecutionInput executionInput = executionInterface.createScriptExecutionInput(cmdLineInterface,
                            temporaryScriptFile.getPath(),
                            content,
                            schemaId,
                            connection.getDatabaseInfo(),
                            connection.getAuthenticationInfo()
                    );


                    FileUtil.writeToFile(temporaryScriptFile, executionInput.getTextContent());
                    if (!temporaryScriptFile.isFile() || !temporaryScriptFile.exists()) {
                        executionManager.writeLogOutput(outputContext, LogOutput.createErrOutput("Failed to create temporary script file " + temporaryScriptFile + "."));
                        throw new IllegalStateException("Failed to create temporary script file " + temporaryScriptFile + ". Check access rights at location.");
                    }

                    ProcessBuilder processBuilder = new ProcessBuilder(executionInput.getCommand());
                    processBuilder.environment().putAll(executionInput.getEnvironmentVars());
                    processBuilder.redirectErrorStream(true);
                    String password = connection.getAuthenticationInfo().getPassword();
                    String lineCommand = executionInput.getLineCommand();
                    if (Strings.isNotEmpty(password)) {
                        lineCommand = lineCommand.replace(password, "*********");
                    }
                    executionManager.writeLogOutput(outputContext, LogOutput.createSysOutput("Executing command: " + lineCommand));
                    executionManager.writeLogOutput(outputContext, LogOutput.createSysOutput(""));
                    Process process = processBuilder.start();

                    outputContext.setProcess(process);
                    activeProcesses.put(sourceFile, process);

                    outputContext.setHideEmptyLines(false);
                    outputContext.start();
                    executionManager.writeLogOutput(outputContext, LogOutput.createSysOutput(outputContext, " - Script execution started", false));

                    try (InputStream inputStream = process.getInputStream()) {
                        LineReader lineReader = new LineReader(inputStream);
                        while (outputContext.isProcessAlive()) {
                            while (outputContext.isActive()) {
                                consumeProcessOutput(lineReader, outputContext, false);
                            }
                            Unsafe.silent(() -> Thread.sleep(1000));
                        }

                        consumeProcessOutput(lineReader, outputContext, true);
                    }

                    LogOutput logOutput = LogOutput.createSysOutput(outputContext,
                            outputContext.isStopped() ?
                                    " - Script execution interrupted by user" :
                                    " - Script execution finished", false);
                    executionManager.writeLogOutput(outputContext, logOutput);
                    ProjectEvents.notify(project,
                            ScriptExecutionListener.TOPIC,
                            (listener) -> listener.scriptExecuted(project, sourceFile));
                    return null;
                }

                @Override
                public void cancel() {
                    outputContext.stop();
                }

                @Override
                public void handleTimeout() {
                    Messages.showErrorDialog(project,
                            "Script execution timeout",
                            "The script execution has timed out",
                            new String[]{"Retry", "Cancel"}, 0,
                            option -> when(option == 0, () -> executeScript(sourceFile)));

                }

                @Override
                public void handleException(Throwable e) {
                    Messages.showErrorDialog(project,
                            "Script execution error",
                            "Error executing SQL script \"" + sourceFile.getPath() + "\". \nDetails: " + e.getMessage(),
                            new String[]{"Retry", "Cancel"}, 0,
                            option -> when(option == 0, () -> executeScript(sourceFile)));
                }
            }.start();
        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
            //executionManager.writeLogOutput(outputContext, LogOutput.createSysOutput(outputContext, " - Script execution cancelled by user", false));
        } catch (Exception e) {
            executionManager.writeLogOutput(outputContext, LogOutput.createErrOutput(e.getMessage()));
            executionManager.writeLogOutput(outputContext, LogOutput.createSysOutput(outputContext, " - Script execution finished with errors", false));
            throw e;
        } finally {
            context.set(EXECUTING, false);
            outputContext.finish();
            activeProcesses.remove(sourceFile);
            File temporaryScriptFile = tempScriptFile.get();
            if (temporaryScriptFile != null && temporaryScriptFile.exists()) {
                executionManager.writeLogOutput(outputContext, LogOutput.createSysOutput("Deleting temporary script file " + temporaryScriptFile));
                FileUtil.delete(temporaryScriptFile);
            }
        }
    }

    private void consumeProcessOutput(LineReader lineReader, LogOutputContext outputContext, boolean eager) throws IOException {
        byte[] bytes = lineReader.readLine();
        while (bytes != null) {
            String line = new String(bytes);
            LogOutput stdOutput = LogOutput.createStdOutput(line);
            executionManager.writeLogOutput(outputContext, stdOutput);
            bytes = eager ? lineReader.readLine() : null;
        }
    }

    public void createCmdLineInterface(
            @NotNull DatabaseType databaseType,
            @Nullable Set<String> bannedNames,
            @NotNull Consumer<CmdLineInterface> consumer) {

        boolean updateSettings = false;
        VirtualFile virtualFile = selectCmdLineExecutable(databaseType, null);
        if (virtualFile != null) {
            Project project = getProject();
            ExecutionEngineSettings executionEngineSettings = ExecutionEngineSettings.getInstance(project);
            if (bannedNames == null) {
                bannedNames = executionEngineSettings.getScriptExecutionSettings().getCommandLineInterfaces().getInterfaceNames();
                updateSettings = true;
            }

            CmdLineInterface cmdLineInterface = new CmdLineInterface(databaseType, virtualFile.getPath(), CmdLineInterface.getDefault(databaseType).getName(), null);
            CmdLineInterfaceInputDialog dialog = new CmdLineInterfaceInputDialog(project, cmdLineInterface, bannedNames);
            dialog.show();
            if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                consumer.accept(cmdLineInterface);
                if (updateSettings) {
                    CmdLineInterfaceBundle commandLineInterfaces = executionEngineSettings.getScriptExecutionSettings().getCommandLineInterfaces();
                    commandLineInterfaces.add(cmdLineInterface);
                }
            }
        }
    }

    @Nullable
    public VirtualFile selectCmdLineExecutable(@NotNull DatabaseType databaseType, @Nullable String selectedExecutable) {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        CmdLineInterface defaultCli = CmdLineInterface.getDefault(databaseType);
        String extension = OS.isWindows() ? ".exe" : "";
        fileChooserDescriptor.
                withTitle("Select Command-Line Client").
                withDescription("Select Command-Line Interface executable (" + defaultCli.getExecutablePath() + extension + ")").
                withShowHiddenFiles(true);
        VirtualFile selectedFile = Strings.isEmpty(selectedExecutable) ? null : LocalFileSystem.getInstance().findFileByPath(selectedExecutable);
        VirtualFile[] virtualFiles = FileChooser.chooseFiles(fileChooserDescriptor, getProject(), selectedFile);
        return virtualFiles.length == 1 ? virtualFiles[0] : null;
    }

    @Nullable
    public CmdLineInterface getRecentInterface(DatabaseType databaseType) {
        String id = recentlyUsedInterfaces.get(databaseType);
        if (id != null) {
            if (Objects.equals(id, CmdLineInterface.DEFAULT_ID)) {
                return CmdLineInterface.getDefault(databaseType);
            }

            ExecutionEngineSettings executionEngineSettings = ExecutionEngineSettings.getInstance(getProject());
            CmdLineInterfaceBundle commandLineInterfaces = executionEngineSettings.getScriptExecutionSettings().getCommandLineInterfaces();
            return commandLineInterfaces.getInterface(id);

        }
        return null;
    }

    private File createTempScriptFile() throws IOException {
        File tempFile = File.createTempFile("DBN-", ".sql");
        if (!tempFile.isFile()) {
            long n = TMP_FILE_RANDOMIZER.nextLong();
            n = n == Long.MIN_VALUE ? 0 : Math.abs(n);
            String tempFileName = "DBN-" + n;

            tempFile = FileUtil.createTempFile(tempFileName, ".sql");
            if (!tempFile.isFile()) {
                String systemDir = PathManager.getSystemPath();
                File systemTempDir = new File(systemDir, "tmp");
                tempFile = new File(systemTempDir, tempFileName);
                FileUtil.createParentDirs(tempFile);
                FileUtil.delete(tempFile);
                FileUtil.createIfDoesntExist(tempFile);
            }
        }
        PosixFileAttributeView view = Files.getFileAttributeView(tempFile.toPath(), PosixFileAttributeView.class);
        if (view != null) {
            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_READ);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_READ);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            view.setPermissions(permissions);
        }

        return tempFile;
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        setBooleanAttribute(element, "clear-outputs", clearOutputOption);
        Element interfacesElement = new Element("recently-used-interfaces");
        element.addContent(interfacesElement);
        for (val entry : recentlyUsedInterfaces.entrySet()) {
            DatabaseType databaseType = entry.getKey();
            String interfaceId = entry.getValue();
            Element interfaceElement = new Element("mapping");
            interfaceElement.setAttribute("database-type", databaseType.name());
            interfaceElement.setAttribute("interface-id", interfaceId);
            interfacesElement.addContent(interfaceElement);
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        recentlyUsedInterfaces.clear();
        clearOutputOption = booleanAttribute(element, "clear-outputs", clearOutputOption);
        Element interfacesElement = element.getChild("recently-used-interfaces");
        if (interfacesElement != null) {
            for (Element child : interfacesElement.getChildren()) {
                DatabaseType databaseType = enumAttribute(child, "database-type", DatabaseType.class);
                String interfaceId = stringAttribute(child, "interface-id");
                recentlyUsedInterfaces.put(databaseType, interfaceId);
            }

        }
    }
}