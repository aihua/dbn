package com.dci.intellij.dbn.debugger.jdbc.evaluation;

import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.common.debug.VariableInfo;
import com.dci.intellij.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dci.intellij.dbn.debugger.common.evaluation.DBDebuggerEvaluator;
import com.dci.intellij.dbn.debugger.common.frame.DBDebugValue;
import com.dci.intellij.dbn.debugger.jdbc.DBJdbcDebugProcess;
import com.dci.intellij.dbn.debugger.jdbc.frame.DBJdbcDebugStackFrame;
import com.dci.intellij.dbn.debugger.jdbc.frame.DBJdbcDebugValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import com.intellij.xdebugger.frame.presentation.XNumericValuePresentation;
import com.intellij.xdebugger.frame.presentation.XStringValuePresentation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DBJdbcDebuggerEvaluator extends DBDebuggerEvaluator<DBJdbcDebugStackFrame, DBJdbcDebugValue> {

    public DBJdbcDebuggerEvaluator(DBJdbcDebugStackFrame frame) {
        super(frame);
    }

    @Override
    public void computePresentation(@NotNull DBJdbcDebugValue debugValue, @NotNull final XValueNode node, @NotNull XValuePlace place) {
        List<String> childVariableNames = debugValue.getChildVariableNames();

        try {
            DBJdbcDebugProcess debugProcess = debugValue.getDebugProcess();
            String variableName = debugValue.getVariableName();
            DBDebugValue parentValue = debugValue.getParentValue();
            String dbVariableName = (parentValue == null ? variableName : parentValue.getVariableName() + "." + variableName).toUpperCase();
            int frameIndex = debugValue.getStackFrame().getFrameIndex();

            DBNConnection conn = debugProcess.getDebuggerConnection();
            DatabaseDebuggerInterface debuggerInterface = debugProcess.getDebuggerInterface();

            VariableInfo variableInfo = debuggerInterface.getVariableInfo(dbVariableName, frameIndex, conn);
            if (variableInfo.getError() != null && frameIndex > 0) {
                // TODO why is the variable lookup not following the "one based" frame indexing?
                variableInfo = debuggerInterface.getVariableInfo(dbVariableName, frameIndex - 1, conn);
            }

            String value = variableInfo.getValue();
            String type = variableInfo.getError();

            if (type != null) {
                type = type.toLowerCase();
                value = "";
            }
            if (childVariableNames != null) {
                type = "record";
            }

            debugValue.setValue(value);
            debugValue.setType(type);
        } catch (Exception e) {
            conditionallyLog(e);
            debugValue.setValue("");
            debugValue.setType(e.getMessage());
        } finally {
            updateValuePresentation(debugValue, node);
        }
    }

    private static void updateValuePresentation(@NotNull DBJdbcDebugValue debugValue, @NotNull XValueNode node) {
        Icon icon = debugValue.getIcon();
        String value = debugValue.getDisplayValue();
        boolean hasChildren = debugValue.hasChildren();

        if (debugValue.isNumeric()) {
            node.setPresentation(icon, new XNumericValuePresentation(value), hasChildren);
        } else if (debugValue.isLiteral()) {
            node.setPresentation(icon, new XStringValuePresentation(value), hasChildren);
        } else {
            node.setPresentation(
                    icon,
                    debugValue.getType(),
                    Commons.nvl(value, "null"),
                    hasChildren);
        }
    }
}
