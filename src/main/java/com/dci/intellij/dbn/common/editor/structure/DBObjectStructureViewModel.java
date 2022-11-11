package com.dci.intellij.dbn.common.editor.structure;

import com.intellij.ide.structureView.FileEditorPositionListener;
import com.intellij.ide.structureView.ModelListener;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.Grouper;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public abstract class DBObjectStructureViewModel implements StructureViewModel {
    protected Set<FileEditorPositionListener> fileEditorPositionListeners = new HashSet<FileEditorPositionListener>();
    protected Set<ModelListener> modelListeners = new HashSet<ModelListener>();

    @Override
    public void addEditorPositionListener(@NotNull FileEditorPositionListener listener) {
        fileEditorPositionListeners.add(listener);
    }

    @Override
    public void removeEditorPositionListener(@NotNull FileEditorPositionListener listener) {
        fileEditorPositionListeners.remove(listener);
    }

    @Override
    public void addModelListener(@NotNull ModelListener modelListener) {
        modelListeners.add(modelListener);
    }

    @Override
    public void removeModelListener(@NotNull ModelListener modelListener) {
        modelListeners.remove(modelListener);
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean shouldEnterElement(Object o) {
        return false;
    }

    @Override
    @NotNull
    public Grouper[] getGroupers() {
        return Grouper.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public Sorter[] getSorters() {
        return new Sorter[0];
    }

    @Override
    @NotNull
    public Filter[] getFilters() {
        return new Filter[0];
    }

    public void rebuild() {
        for (ModelListener modelListener : modelListeners) {
            modelListener.onModelChanged();
        }

        for (FileEditorPositionListener positionListener : fileEditorPositionListeners) {
            positionListener.onCurrentElementChanged();
        }
    }
}
