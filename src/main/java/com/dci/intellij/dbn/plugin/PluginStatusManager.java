package com.dci.intellij.dbn.plugin;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.component.ApplicationComponentBase;
import com.dci.intellij.dbn.common.component.PersistentState;
import com.dci.intellij.dbn.common.project.Projects;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dci.intellij.dbn.common.component.Components.applicationService;
import static com.dci.intellij.dbn.common.options.setting.Settings.*;
import static com.dci.intellij.dbn.common.project.Projects.getDefaultProject;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dci.intellij.dbn.plugin.DBPluginStatus.*;
import static com.dci.intellij.dbn.plugin.PluginStatusManager.COMPONENT_NAME;

@Getter
@Setter
@State(
    name = COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class PluginStatusManager extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.PluginStatusManager";

    private Map<String, DBPluginStatusPair> pluginStatuses = new ConcurrentHashMap<>();

    public PluginStatusManager() {
        super(COMPONENT_NAME);

        // reevaluate statuses just before closing
        Projects.projectClosing(project -> evaluatePluginStatus(project));
    }

    public static PluginStatusManager getInstance() {
        return applicationService(PluginStatusManager.class);
    }

    public void evaluatePluginStatus(Project project) {
        evaluateAndCapturePluginStatus(getDefaultProject());
        evaluateAndCapturePluginStatus(project);
    }

    private void evaluateAndCapturePluginStatus(Project project) {
        String projectPath = project.isDefault() ? "DEFAULT" : project.getBasePath();
        DBPluginStatus dbn = evaluateDbnPluginStatus(project);
        DBPluginStatus sql = evaluateSqlPluginStatus(project);
        pluginStatuses.put(projectPath, new DBPluginStatusPair(dbn, sql));

        // global case - conditionally reset the status of SQL plugin to MISSING for all projects
        if (sql == MISSING) pluginStatuses.values().forEach(s -> s.setSql(MISSING));
    }

    public DBPluginStatus getDbnPluginStatus() {
        return pluginStatuses.values().stream().map(s -> s.getDbn()).max(Comparator.naturalOrder()).orElse(UNKNOWN);
    }

    public DBPluginStatus getSqlPluginStatus() {
        return pluginStatuses.values().stream().map(s -> s.getSql()).max(Comparator.naturalOrder()).orElse(UNKNOWN);
    }

    private static DBPluginStatus evaluateDbnPluginStatus(Project project) {
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        List<ConnectionHandler> connections = connectionManager.getConnectionBundle().getAllConnections();
        if (!connections.isEmpty()) return ACTIVE;

        return PASSIVE;
    }

    private static DBPluginStatus evaluateSqlPluginStatus(Project project) {
        try {
            IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(DatabaseNavigator.SQL_PLUGIN_ID);
            if (pluginDescriptor == null) return MISSING; // not installed

            ClassLoader pluginClassLoader = pluginDescriptor.getPluginClassLoader();
            if (pluginClassLoader == null) return MISSING;

            Class<?> psiFacadeClass = pluginClassLoader.loadClass("com.intellij.database.psi.DbPsiFacade");
            Method getInstanceMethod = psiFacadeClass.getMethod("getInstance", Project.class);

            Object psiFacade = getInstanceMethod.invoke(psiFacadeClass, project);
            Method getDataSourcesMethod = psiFacadeClass.getMethod("getDataSources");
            List configs = (List) getDataSourcesMethod.invoke(psiFacade);
            if (!configs.isEmpty()) return ACTIVE; // connection configs found

            return PASSIVE;
        } catch (Throwable e) {
            conditionallyLog(e);
        }
        return PASSIVE;
    }

    /**************************************************************************
     *                       PersistentStateComponent                         *
     **************************************************************************/

    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element statusesElement = newElement(element, "plugin-statuses");
        for (String projectPath : pluginStatuses.keySet()) {
            DBPluginStatusPair statusPair = pluginStatuses.get(projectPath);
            Element statusElement = newElement(statusesElement, "status");
            setStringAttribute(statusElement, "project", projectPath);
            setEnumAttribute(statusElement, "dbn", statusPair.getDbn());
            setEnumAttribute(statusElement, "sql", statusPair.getSql());
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element statusesElement = element.getChild("plugin-statuses");
        if (statusesElement == null) return;

        List<Element> statusElements = statusesElement.getChildren();
        for (Element statusElement : statusElements) {
            String projectPath = stringAttribute(statusElement, "project");
            DBPluginStatus dbnPluginStatus = enumAttribute(statusElement, "dbn", DBPluginStatus.class);
            DBPluginStatus sqlPluginStatus = enumAttribute(statusElement, "sql", DBPluginStatus.class);
            pluginStatuses.put(projectPath, new DBPluginStatusPair(dbnPluginStatus, sqlPluginStatus));
        }
    }
}
