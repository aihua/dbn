package com.dci.intellij.dbn.navigation.object;

import com.dci.intellij.dbn.common.consumer.ConcurrentSetCollector;
import com.dci.intellij.dbn.common.consumer.SetCollector;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.StatefulDisposableBase;
import com.dci.intellij.dbn.common.load.ProgressMonitor;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.dci.intellij.dbn.navigation.options.ObjectsLookupSettings;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.dci.intellij.dbn.options.ProjectSettingsManager;
import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.Objects;

import static com.dci.intellij.dbn.common.dispose.Failsafe.guarded;

public class DBObjectLookupModel extends StatefulDisposableBase implements ChooseByNameModel {
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final ProjectRef project;
    private final ConnectionRef selectedConnection;
    private final DBObjectRef<DBSchema> selectedSchema;
    private final ObjectsLookupSettings settings;
    private final @Getter SetCollector<DBObject> data = ConcurrentSetCollector.create();

    public DBObjectLookupModel(@NotNull Project project, @Nullable ConnectionHandler selectedConnection, DBSchema selectedSchema) {
        this.project = ProjectRef.of(project);
        this.selectedConnection = ConnectionRef.of(selectedConnection);
        this.selectedSchema = DBObjectRef.of(selectedSchema);
        settings = ProjectSettingsManager.getSettings(project).getNavigationSettings().getObjectsLookupSettings();
    }

    @Override
    public String getPromptText() {
        ConnectionHandler selectedConnection = getSelectedConnection();
        String connectionIdentifier = selectedConnection == null || selectedConnection.isVirtual() ?
                "All Connections" :
                selectedConnection.getName();
        return "Enter database object name (" + connectionIdentifier + (selectedSchema == null ? "" : " / " + selectedSchema.getObjectName()) + ")";
    }

    protected ConnectionHandler getSelectedConnection() {
        return selectedConnection.get();
    }

    @Nullable
    public DBSchema getSelectedSchema() {
        return DBObjectRef.get(selectedSchema);
    }

    @NotNull
    @Override
    public String getNotInMessage() {
        return "No database object matching criteria";
    }

    @NotNull
    @Override
    public String getNotFoundMessage() {
        return "Database object not found";
    }

    @Override
    public String getCheckBoxName() {
        return getSettings().getForceDatabaseLoad().value() ? "Load database objects" : null;
    }

    @Override
    public char getCheckBoxMnemonic() {
        return 0;
    }

    @Override
    public boolean loadInitialCheckBoxState() {
        return false;
    }

    @Override
    public void saveInitialCheckBoxState(boolean state) {
    }

    @NotNull
    @Override
    public ListCellRenderer getListCellRenderer() {
        return DBObjectListCellRenderer.INSTANCE;
    }

    @Override
    public boolean willOpenEditor() {
        return false;
    }

    @Override
    public boolean useMiddleMatching() {
        return true;
    }

    @Override
    @NotNull
    public String[] getNames(boolean checkBoxState) {
        return guarded(EMPTY_STRING_ARRAY, () -> {
            boolean databaseLoadActive = getSettings().getForceDatabaseLoad().value();
            boolean forceLoad = checkBoxState && databaseLoadActive;

            if (!forceLoad && selectedSchema != null) {
                // touch the schema for next load
                selectedSchema.ensure().getChildren();
            }
            checkCancelled();

            DBObjectLookupScanner scanner = new DBObjectLookupScanner(this, forceLoad);
            scanner.scan();

            return data.elements().
                    stream().
                    sorted(Comparator.comparing(DBObject::getQualifiedName)).
                    map(object -> object.getName()).
                    distinct().
                    toArray(String[]::new);
        });
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @Override
    @NotNull
    public Object[] getElementsByName(@NotNull String name, boolean checkBoxState, @NotNull String pattern) {
        return guarded(EMPTY_ARRAY, () -> data.elements().
                stream().
                filter(object -> Objects.equals(object.getName(), name)).
                sorted(Comparator.comparing(DBObject::getQualifiedName)).
                toArray());
    }

    protected boolean isListLookupEnabled(DBObjectType objectType) {
        boolean enabled = isObjectLookupEnabled(objectType);
        if (enabled) return true;

        for (DBObjectType childObjectType : objectType.getChildren()) {
            if (isListLookupEnabled(childObjectType)) {
                return true;
            }
        }
        return enabled;
    }

    protected boolean isObjectLookupEnabled(DBObjectType objectType) {
        return getSettings().isEnabled(objectType);
    }

    @NotNull
    public ObjectsLookupSettings getSettings() {
        return Failsafe.nn(settings);
    }

    @Override
    public String getElementName(@NotNull Object element) {
        if (element instanceof DBObject) {
            DBObject object = (DBObject) element;
            return object.getQualifiedName();
        }

        return element.toString();
    }

    @Override
    @NotNull
    public String[] getSeparators() {
        return new String[]{"."};
    }

    @Override
    public String getFullName(@NotNull Object element) {
        return getElementName(element);
    }

    @Override
    public String getHelpId() {
        return null;
    }

    @Override
    protected void disposeInner() {
        nullify();
    }

    public void checkCancelled() {
        checkDisposed();
        ProgressMonitor.checkCancelled();
    }
}
