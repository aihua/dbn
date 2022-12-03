package com.dci.intellij.dbn.object.action;

import com.dci.intellij.dbn.editor.data.filter.global.DataDependencyPath;
import com.dci.intellij.dbn.editor.data.filter.global.DataDependencyPathBuilder;
import com.dci.intellij.dbn.object.DBTable;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class TestAction extends AnAction {
    private final DBObject object;
    public TestAction(DBObject object) {
        super("Test", "Test", null);
        this.object = object;
        setDefaultIcon(true);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        new Thread(() -> {
            if (object instanceof DBTable) {
                DBTable table = (DBTable) object;
                DBTable target = table.getSchema().getChildObject(DBObjectType.TABLE, "ALLOCATIONS", (short) 0, false);
                DataDependencyPath[] shortestPath = new DataDependencyPath[1];
                DataDependencyPathBuilder.buildDependencyPath(null, table.getColumns().get(0), target.getColumns().get(0), shortestPath);
                System.out.println();
            }
        }).start();
    }
}