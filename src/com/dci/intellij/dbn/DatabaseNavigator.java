package com.dci.intellij.dbn;

import com.dci.intellij.dbn.common.component.ApplicationComponent;
import com.dci.intellij.dbn.execution.ExecutionManager;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.extensions.PluginId;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.getBoolean;
import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.setBoolean;

@State(
    name = DatabaseNavigator.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseNavigator implements ApplicationComponent, PersistentStateComponent<Element> {
    public static final String COMPONENT_NAME = "DBNavigator.Application.Settings";
    public static final String STORAGE_FILE = "dbnavigator.xml";

    private static final String SQL_PLUGIN_ID = "com.intellij.sql";
    public static final PluginId DBN_PLUGIN_ID = PluginId.getId("DBN");
    /*static {
        Extensions.getRootArea().
                getExtensionPoint(CodeStyleSettingsProvider.EXTENSION_POINT_NAME).
                registerExtension(new SQLCodeStyleSettingsProvider());
    }*/

    @Override
    @NotNull
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    public static boolean DEBUG = false;
    public static boolean SLOW = false;
    public static boolean DEVELOPER = false;

    private boolean showPluginConflictDialog;

    public DatabaseNavigator() {
        new NotificationGroup("Database Navigator", NotificationDisplayType.TOOL_WINDOW, true, ExecutionManager.TOOL_WINDOW_ID);
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
        return ApplicationManager.getApplication().getComponent(DatabaseNavigator.class);
    }

    public boolean isSlowDatabaseModeEnabled() {
        return DEVELOPER && SLOW;
    }

    public void setSlowDatabaseModeEnabled(boolean slowDatabaseModeEnabled) {
        SLOW = slowDatabaseModeEnabled;
    }

    public String getName() {
        return null;
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("state");
        setBoolean(element, "enable-debug-mode", DEBUG);
        setBoolean(element, "enable-developer-mode", DEVELOPER);
        setBoolean(element, "show-plugin-conflict-dialog", showPluginConflictDialog);
        return element;
    }

    @Override
    public void loadState(@NotNull Element element) {
        DEBUG = getBoolean(element, "enable-debug-mode", false);
        DEVELOPER = getBoolean(element, "enable-developer-mode", false);
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

