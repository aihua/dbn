package com.dci.intellij.dbn.execution.method.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.execution.method.MethodExecutionManager;
import com.dci.intellij.dbn.execution.method.ui.MethodExecutionHistory;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.DBProgram;
import com.dci.intellij.dbn.object.action.ObjectListShowAction;
import com.dci.intellij.dbn.object.common.DBObject;
import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ProgramMethodDebugAction extends ObjectListShowAction {
    public ProgramMethodDebugAction(DBProgram program) {
        super("Debug...", program);
        getTemplatePresentation().setIcon(Icons.METHOD_EXECUTION_DEBUG);
    }

    @Nullable
    @Override
    public List<? extends DBObject> getRecentObjectList() {
        DBProgram program = (DBProgram) getSourceObject();
        MethodExecutionManager methodExecutionManager = MethodExecutionManager.getInstance(program.getProject());
        MethodExecutionHistory executionHistory = methodExecutionManager.getExecutionHistory();
        return executionHistory.getRecentlyExecutedMethods(program);
    }


    @Override
    public List<? extends DBObject> getObjectList() {
        DBProgram program = (DBProgram) getSourceObject();
        List objects = new ArrayList();
        objects.addAll(program.getProcedures());
        objects.addAll(program.getFunctions());
        return objects;
    }

    @Override
    public String getTitle() {
        return "Select method to debug";
    }

    @Override
    public String getEmptyListMessage() {
        DBProgram program = (DBProgram) getSourceObject();
        return "The " + program.getQualifiedNameWithType() + " has no methods.";
    }

     @Override
     public String getListName() {
        return "executable elements";
    }

    @Override
    protected AnAction createObjectAction(DBObject object) {
        return new MethodDebugAction((DBProgram) getSourceObject(), (DBMethod) object);
    }
}