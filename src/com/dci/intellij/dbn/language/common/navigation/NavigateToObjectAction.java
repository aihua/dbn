package com.dci.intellij.dbn.language.common.navigation;

import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class NavigateToObjectAction extends AnAction {
    private final DBObjectRef objectRef;
    public NavigateToObjectAction(DBObject object, DBObjectType objectType) {
        super("Navigate to " + objectType.getName(), null, objectType.getIcon());
        objectRef = DBObjectRef.of(object);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (objectRef != null) {
            DBObject object = objectRef.get();
            if (object != null) {
                object.navigate(true);
            }
        }

    }
}