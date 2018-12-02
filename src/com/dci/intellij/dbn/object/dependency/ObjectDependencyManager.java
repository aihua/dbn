package com.dci.intellij.dbn.object.dependency;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.dispose.FailsafeUtil;
import com.dci.intellij.dbn.common.options.ConfigurationUtil;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.dependency.ui.ObjectDependencyTreeDialog;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@State(
    name = ObjectDependencyManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ObjectDependencyManager extends AbstractProjectComponent implements PersistentStateComponent<Element> {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ObjectDependencyManager";

    private ObjectDependencyType lastUserDependencyType = ObjectDependencyType.INCOMING;

    private ObjectDependencyManager(final Project project) {
        super(project);
    }

    public static ObjectDependencyManager getInstance(@NotNull Project project) {
        return FailsafeUtil.getComponent(project, ObjectDependencyManager.class);
    }

    public ObjectDependencyType getLastUserDependencyType() {
        return lastUserDependencyType;
    }

    public void setLastUserDependencyType(ObjectDependencyType lastUserDependencyType) {
        this.lastUserDependencyType = lastUserDependencyType;
    }

    public void openDependencyTree(final DBSchemaObject schemaObject) {
        ConnectionAction.invoke("opening object dependency tree", schemaObject, (Integer) null, action -> {
            ObjectDependencyTreeDialog dependencyTreeDialog = new ObjectDependencyTreeDialog(getProject(), schemaObject);
            dependencyTreeDialog.show();
        });
    }

    @NonNls
    @NotNull
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    public Element getState() {
        Element element = new Element("state");
        ConfigurationUtil.setEnum(element, "last-used-dependency-type", lastUserDependencyType);
        return element;
    }

    @Override
    public void loadState(final Element element) {
        lastUserDependencyType = ConfigurationUtil.getEnum(element, "last-used-dependency-type", lastUserDependencyType);
    }

}
