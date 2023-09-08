package com.dci.intellij.dbn;

import com.dci.intellij.dbn.common.component.ApplicationComponentBase;
import com.dci.intellij.dbn.common.component.PersistentState;
import com.dci.intellij.dbn.common.event.ApplicationEvents;
import com.dci.intellij.dbn.common.file.FileTypeService;
import com.dci.intellij.dbn.diagnostics.Diagnostics;
import com.dci.intellij.dbn.editor.code.SourceCodeEditorListener;
import com.dci.intellij.dbn.editor.console.SQLConsoleEditorListener;
import com.dci.intellij.dbn.language.editor.DBLanguageFileEditorListener;
import com.dci.intellij.dbn.plugin.DBNPluginStateListener;
import com.dci.intellij.dbn.plugin.PluginConflictManager;
import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginInstaller;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.extensions.PluginId;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.dci.intellij.dbn.common.component.Components.applicationService;
import static com.dci.intellij.dbn.common.options.setting.Settings.getBoolean;
import static com.dci.intellij.dbn.common.options.setting.Settings.setBoolean;
import static com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER;

@Slf4j
@State(
    name = DatabaseNavigator.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseNavigator extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.Settings";
    public static final String STORAGE_FILE = "dbnavigator.xml";

    private static final String SQL_PLUGIN_ID = "com.intellij.sql";
    public static final PluginId DB_PLUGIN_ID = PluginId.getId("com.intellij.database");
    public static final PluginId DBN_PLUGIN_ID = PluginId.getId("DBN");
    /*static {
        Extensions.getRootArea().
                getExtensionPoint(CodeStyleSettingsProvider.EXTENSION_POINT_NAME).
                registerExtension(new SQLCodeStyleSettingsProvider());
    }*/

    private boolean showPluginConflictDialog;

    public DatabaseNavigator() {
        super(COMPONENT_NAME);
        PluginInstaller.addStateListener(new DBNPluginStateListener());
        //new NotificationGroup("Database Navigator", NotificationDisplayType.TOOL_WINDOW, true, ExecutionManager.TOOL_WINDOW_ID);

        PluginConflictManager.getInstance();
        FileTypeService.getInstance();
        ApplicationEvents.subscribe(this, FILE_EDITOR_MANAGER, new DBLanguageFileEditorListener());
        ApplicationEvents.subscribe(this, FILE_EDITOR_MANAGER, new SQLConsoleEditorListener());
        ApplicationEvents.subscribe(this, FILE_EDITOR_MANAGER, new SourceCodeEditorListener());

        registerExecutorExtension();

    }

    private static void registerExecutorExtension() {
        try {
            ExtensionsArea extensionArea = Extensions.getRootArea();
            boolean available = extensionArea.hasExtensionPoint(Executor.EXECUTOR_EXTENSION_NAME);
            if (!available) extensionArea.getExtensionPoint(Executor.EXECUTOR_EXTENSION_NAME).registerExtension(new DefaultDebugExecutor());
        } catch (Throwable e) {
            log.error("Failed to register debug executor extension", e);
        }
    }

/*
    private static boolean sqlPluginActive() {
        for (IdeaPluginDescriptor pluginDescriptor : PluginManager.getPlugins()) {
            if (pluginDescriptor.getPluginId().getIdString().equals(SQL_PLUGIN_ID)) {
                return !PluginManager.getDisabledPlugins().contains(SQL_PLUGIN_ID);
            }
        }
        return false;
    }
*/

    public static DatabaseNavigator getInstance() {
        return applicationService(DatabaseNavigator.class);
    }

    @NotNull
    public static IdeaPluginDescriptor getPluginDescriptor() {
        return Objects.requireNonNull(PluginManager.getPlugin(DBN_PLUGIN_ID));
    }

    public String getName() {
        return null;
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element diagnosticsElement = new Element("diagnostics");
        element.addContent(diagnosticsElement);
        Diagnostics.writeState(diagnosticsElement);
        setBoolean(element, "show-plugin-conflict-dialog", showPluginConflictDialog);
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element diagnosticsElement = element.getChild("diagnostics");
        Diagnostics.readState(diagnosticsElement);
        showPluginConflictDialog = getBoolean(element, "show-plugin-conflict-dialog", true);
    }

/*    private void resolvePluginConflict() {
        if (showPluginConflictDialog && sqlPluginActive()) {
            showPluginConflictDialog = false;
            Dispatch.run(() -> {
                List<String> disabledList = PluginManager.getDisabledPlugins();
                String message =
                        "Database Navigator plugin (DBN) is not compatible with the IntelliJ IDEA built-in SQL functionality. " +
                                "They both provide similar features but present quite different use-cases.\n" +
                                "In order to have access to the full functionality of Database Navigator plugin and avoid usage confusions, we strongly advise you disable the IDEA SQL plugin. \n" +
                                "You can enable it at any time from your plugin manager.\n\n" +
                                "For more details about the plugin conflict, please visit the Database Navigator support page.\n" +
                                "Note: IDEA will need to restart if you choose to make changes in the plugin configuration.\n\n" +
                                "Please pick an option to proceed.";
                String title = Constants.DBN_TITLE_PREFIX + "Plugin Conflict";
                String[] options = {
                        "Disable IDEA SQL plugin (restart)",
                        "Disable DBN plugin (restart)",
                        "Ignore and continue (not recommended)"};
                Icon icon = Messages.getWarningIcon();
                int exitCode = Messages.showDialog(message, title, options, 0, icon);
                if (exitCode == 0 || exitCode == 1) {
                    try {
                        disabledList.add(exitCode == 1 ? DBN_PLUGIN_ID.getIdString() : SQL_PLUGIN_ID);
                        PluginManager.saveDisabledPlugins(disabledList, false);
                        ApplicationManager.getApplication().restart();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }*/
}

