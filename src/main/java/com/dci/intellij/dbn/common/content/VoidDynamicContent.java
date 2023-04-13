package com.dci.intellij.dbn.common.content;

import com.dci.intellij.dbn.common.content.dependency.ContentDependencyAdapter;
import com.dci.intellij.dbn.common.content.dependency.VoidContentDependencyAdapter;
import com.dci.intellij.dbn.common.content.loader.DynamicContentLoader;
import com.dci.intellij.dbn.common.content.loader.VoidDynamicContentLoader;
import com.dci.intellij.dbn.common.dispose.StatefulDisposableBase;
import com.dci.intellij.dbn.common.property.Property;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.DatabaseEntity;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class VoidDynamicContent extends StatefulDisposableBase implements DynamicContent{
    public static final VoidDynamicContent INSTANCE = new VoidDynamicContent();

    private final List<?> elements = Collections.emptyList();

    private VoidDynamicContent() {}

    @Override
    public void load() {}

    @Override
    public void loadInBackground() {}


    @Override
    public void reload() {}

    @Override
    public void refresh() {}

    @Override
    public byte getSignature() {
        return 0;
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public boolean isMaster() {
        return true;
    }

    @Override
    public boolean isLoading() {
        return false;
    }

    @Override
    public boolean isLoadingInBackground() {
        return false;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public void markDirty() {}

    @Override
    public DynamicContentType getContentType() {
        return null;
    }

    @NotNull
    @Override
    public Project getProject() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentDescription() {
        return "Empty Content";
    }

    @Override
    public List getAllElements() {
        return elements;
    }

    @Override
    public List getElements() {
        return elements;
    }

    @Override
    public List getElements(String name) {
        return Collections.emptyList();
    }

    @Override
    public DynamicContentElement getElement(String name, short overload) {
        return null;
    }

    @Override
    public void setElements(@Nullable List elements) {

    }

    @Override
    public int size() {
        return 0;
    }

    @NotNull
    @Override
    public <E extends DatabaseEntity> E getParentEntity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DynamicContentLoader getLoader() {
        return VoidDynamicContentLoader.INSTANCE;
    }

    @Override
    public ContentDependencyAdapter getDependencyAdapter() {
        return VoidContentDependencyAdapter.INSTANCE;
    }

    @NotNull
    @Override
    public ConnectionHandler getConnection() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public String getName() {
        return "Empty Content";
    }

    @Override
    public boolean set(Property status, boolean value) {
        return false;
    }

    @Override
    public boolean is(Property status) {
        return false;
    }

    @Override
    protected void disposeInner() {}
}
