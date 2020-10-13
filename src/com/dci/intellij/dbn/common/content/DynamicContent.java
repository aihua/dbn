package com.dci.intellij.dbn.common.content;

import com.dci.intellij.dbn.common.content.dependency.ContentDependencyAdapter;
import com.dci.intellij.dbn.common.content.loader.DynamicContentLoader;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.filter.Filter;
import com.dci.intellij.dbn.common.property.PropertyHolder;
import com.dci.intellij.dbn.common.util.Compactable;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.GenericDatabaseElement;
import com.dci.intellij.dbn.database.DatabaseMetadataInterface;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DynamicContent<T extends DynamicContentElement> extends StatefulDisposable, Compactable, PropertyHolder<DynamicContentStatus> {

    /**
     * Loads the content. It is typically called every time the content is queried.
     * The check shouldLoad() is made before to avoid pointless loads.
     */
    void load();

    /**
     * Ensures the content is loaded
     * Calls load() in synchronized block
     */
    void ensure();

    /**
     * Rebuilds the content. This method is called when reloading the content
     * is triggered deliberately by the user directly or by a ddl change.
     */
    void reload();

    /**
     * Soft reload. Mark sources dirty
     */
    void refresh();

    /**
     * The timestamp of the last change on the content.
     */
    short getChangeSignature();

    /**
     * A load attempt has been made already
     */
    boolean isLoaded();

    boolean isSubContent();

    boolean canLoadFast();

    /**
     * Content is currently loading
     */
    boolean isLoading();

    /**
     * The content has been loaded but with errors (e.g. because of database connectivity problems)
     */
    boolean isDirty();

    boolean isEmpty();

    boolean isMutable();

    boolean isPassive();

    void markDirty();

    DynamicContentType getContentType();

    Project getProject();

    String getContentDescription();

    @NotNull List<T> getElements();

    @Nullable List<T> getElements(String name);

    @NotNull List<T> getAllElements();

    @Nullable
    Filter<T> getFilter();


    T getElement(String name, short overload);

    void setElements(@Nullable List<T> elements);

    int size();

    @NotNull
    GenericDatabaseElement getParentElement();

    DynamicContentLoader getLoader();

    ContentDependencyAdapter getDependencyAdapter();

    ConnectionHandler getConnectionHandler();

    @NotNull
    default DatabaseMetadataInterface getMetadataInterface() {
        ConnectionHandler connectionHandler = getConnectionHandler();
        return Failsafe.nn(connectionHandler).getInterfaceProvider().getMetadataInterface();
    }

    void loadInBackground();

    void updateChangeSignature();

    String getName();
}
