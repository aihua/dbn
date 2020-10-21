package com.dci.intellij.dbn.object.filter.quick;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.browser.model.BrowserTreeEventListener;
import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.options.setting.SettingsSupport;
import com.dci.intellij.dbn.common.state.PersistentStateElement;
import com.dci.intellij.dbn.common.ui.tree.TreeEventType;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.list.DBObjectList;
import com.dci.intellij.dbn.object.filter.ConditionOperator;
import com.dci.intellij.dbn.object.filter.quick.ui.ObjectQuickFilterDialog;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import gnu.trove.THashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@State(
    name = ObjectQuickFilterManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ObjectQuickFilterManager extends AbstractProjectComponent implements PersistentStateComponent<Element> {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ObjectQuickFilterManager";

    private Map<CacheKey, ObjectQuickFilter> cachedFilters = new THashMap<CacheKey, ObjectQuickFilter>();
    private ConditionOperator lastUsedOperator = ConditionOperator.EQUAL;

    private ObjectQuickFilterManager(Project project) {
        super(project);
    }

    public void openFilterDialog(DBObjectList objectList) {
        ObjectQuickFilterDialog dialog = new ObjectQuickFilterDialog(getProject(), objectList);
        dialog.show();
    }

    public void applyFilter(DBObjectList objectList, @Nullable ObjectQuickFilter filter) {
        objectList.setQuickFilter(filter);
        ProjectEvents.notify(getProject(),
                BrowserTreeEventListener.TOPIC,
                (listener) -> listener.nodeChanged(objectList, TreeEventType.STRUCTURE_CHANGED));

        CacheKey cacheKey = new CacheKey(objectList);
        if (filter == null || filter.isEmpty()) {
            cachedFilters.remove(cacheKey);
        } else {
            cachedFilters.put(cacheKey, filter);
        }
    }

    public void applyCachedFilter(DBObjectList objectList) {
        CacheKey cacheKey = new CacheKey(objectList);
        ObjectQuickFilter filter = cachedFilters.get(cacheKey);
        if (filter != null) {
            objectList.setQuickFilter(filter);
        }
    }

    public ConditionOperator getLastUsedOperator() {
        return lastUsedOperator;
    }

    public void setLastUsedOperator(ConditionOperator lastUsedOperator) {
        this.lastUsedOperator = lastUsedOperator;
    }

    private class CacheKey implements PersistentStateElement {
        private ConnectionId connectionId;
        private String schemaName;
        private DBObjectType objectType;

        public CacheKey() {
        }

        public CacheKey(DBObjectList objectList) {
            connectionId = objectList.getConnectionHandler().getConnectionId();
            BrowserTreeNode treeParent = objectList.getParent();
            if (treeParent instanceof DBSchema) {
                schemaName = treeParent.getName();
            } else {
                schemaName = "";
            }
            objectType = objectList.getObjectType();
        }

        public ConnectionId getConnectionId() {
            return connectionId;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public DBObjectType getObjectType() {
            return objectType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (!connectionId.equals(cacheKey.connectionId)) return false;
            if (!schemaName.equals(cacheKey.schemaName)) return false;
            return objectType == cacheKey.objectType;

        }

        @Override
        public int hashCode() {
            int result = connectionId.hashCode();
            result = 31 * result + schemaName.hashCode();
            result = 31 * result + objectType.hashCode();
            return result;
        }

        @Override
        public void readState(Element element) {
            connectionId = ConnectionId.get(element.getAttributeValue("connection-id"));
            schemaName = element.getAttributeValue("schema");
            objectType = DBObjectType.get(element.getAttributeValue("object-type"));
        }

        @Override
        public void writeState(Element element) {
            element.setAttribute("connection-id", connectionId.id());
            element.setAttribute("schema", schemaName);
            element.setAttribute("object-type", objectType.getName());
        }
    }

    /***************************************
     *            ProjectComponent         *
     ***************************************/
    public static ObjectQuickFilterManager getInstance(@NotNull Project project) {
        return Failsafe.getComponent(project, ObjectQuickFilterManager.class);
    }

    @Override
    @NonNls
    @NotNull
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
        SettingsSupport.setEnum(element, "last-used-operator", lastUsedOperator);
        Element filtersElement = new Element("filters");
        element.addContent(filtersElement);

        ConnectionManager connectionManager = ConnectionManager.getInstance(getProject());
        for (CacheKey cacheKey : cachedFilters.keySet()) {

            if (connectionManager.isValidConnectionId(cacheKey.getConnectionId())) {
                ObjectQuickFilter filter = cachedFilters.get(cacheKey);
                Element filterElement = new Element("filter");
                filtersElement.addContent(filterElement);

                cacheKey.writeState(filterElement);
                filter.writeState(filterElement);
            }
        }

        return element;
    }

    @Override
    public void loadState(Element element) {
        Element filtersElement = element.getChild("filters");
        lastUsedOperator = SettingsSupport.getEnum(element, "last-used-operator", lastUsedOperator);
        if (filtersElement != null) {
            for (Element filterElement : filtersElement.getChildren()) {
                CacheKey cacheKey = new CacheKey();
                cacheKey.readState(filterElement);

                ObjectQuickFilter filter = new ObjectQuickFilter(cacheKey.getObjectType());
                filter.readState(filterElement);

                cachedFilters.put(cacheKey, filter);
            }
        }
    }
}
