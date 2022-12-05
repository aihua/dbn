package com.dci.intellij.dbn.object.action;

import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.object.common.list.DBObjectList;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class ObjectsReloadAction extends DumbAwareAction {

    private final DBObjectList<?> objectList;

    ObjectsReloadAction(DBObjectList<?> objectList) {
        super((objectList.isLoaded() ? "Reload " : "Load ") + objectList.getName());
        this.objectList = objectList;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String listName = objectList.getName();
        boolean loaded = objectList.isLoaded();

        String description = loaded ? "reloading the " + listName : "loading the " + listName;
        ConnectionAction.invoke(description, true, objectList, action -> reloadObjectList());
    }

    private void reloadObjectList() {
        Background.run(objectList.getProject(), () -> {
            objectList.getConnection().getMetaDataCache().reset();
            objectList.reload();
        });
    }
}