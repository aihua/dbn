package com.dci.intellij.dbn.debugger.jdwp.frame;

import com.dci.intellij.dbn.common.util.LazyValue;
import com.dci.intellij.dbn.common.util.SimpleLazyValue;
import com.dci.intellij.dbn.debugger.DBDebugUtil;
import com.dci.intellij.dbn.debugger.common.frame.DBDebugStackFrame;
import com.dci.intellij.dbn.debugger.jdwp.evaluation.DBJdwpDebuggerEvaluator;
import com.dci.intellij.dbn.debugger.jdwp.process.DBJdwpDebugProcess;
import com.dci.intellij.dbn.execution.ExecutionInput;
import com.dci.intellij.dbn.execution.statement.StatementExecutionInput;
import com.dci.intellij.dbn.language.common.psi.IdentifierPsiElement;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import com.sun.jdi.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Set;

public class DBJdwpDebugStackFrame extends DBDebugStackFrame<DBJdwpDebugProcess, DBJdwpDebugValue>{
    private long childrenComputed = 0;
    private XStackFrame underlyingFrame;
    private LazyValue<DBJdwpDebuggerEvaluator> evaluator = new SimpleLazyValue<DBJdwpDebuggerEvaluator>() {
        @Override
        protected DBJdwpDebuggerEvaluator load() {
            return new DBJdwpDebuggerEvaluator(DBJdwpDebugStackFrame.this);
        }
    };

    private LazyValue<Location> location = new SimpleLazyValue<Location>() {
        @Override
        protected Location load() {
            return null; //underlyingFrame == null ? null : underlyingFrame.getDescriptor().getLocation();
        }
    };

    public DBJdwpDebugStackFrame(DBJdwpDebugProcess debugProcess, XStackFrame underlyingFrame, int index) {
        super(debugProcess, index);
        this.underlyingFrame = underlyingFrame;
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        underlyingFrame.computeChildren(node);
/*
        if (System.currentTimeMillis() - childrenComputed > 0){
            System.out.println(System.currentTimeMillis() - childrenComputed);
            underlyingFrame.computeChildren(node);
            childrenComputed = System.currentTimeMillis();
        }
*/
    }

    @Override
    protected XSourcePosition resolveSourcePosition() {
        Location location = getLocation();
        int lineNumber = location == null ? 0 : location.lineNumber() - 1;

        DBJdwpDebugProcess debugProcess = getDebugProcess();
        if (debugProcess.isDeclaredBlock(location) || debugProcess.getOwnerName(location) == null) {
            ExecutionInput executionInput = debugProcess.getExecutionInput();
            if (executionInput instanceof StatementExecutionInput) {
                StatementExecutionInput statementExecutionInput = (StatementExecutionInput) executionInput;
                lineNumber += statementExecutionInput.getExecutableLineNumber();
            }
        }
        return XSourcePositionImpl.create(getVirtualFile(), lineNumber);
    }

    @Override
    protected VirtualFile resolveVirtualFile() {
        Location location = getLocation();
        return getDebugProcess().getVirtualFile(location);

    }

    public XStackFrame getUnderlyingFrame() {
        return underlyingFrame;
    }

    @Override
    @NotNull
    public DBJdwpDebuggerEvaluator getEvaluator() {
        return evaluator.get();
    }


    @Nullable
    public Location getLocation() {
        return location.get();
    }

    @Nullable
    @Override
    protected DBJdwpDebugValue createSuspendReasonDebugValue() {
        return null;
    }

    @NotNull
    @Override
    public DBJdwpDebugValue createDebugValue(String variableName, DBJdwpDebugValue parentValue, Set<String> childVariableNames, Icon icon) {
        return new DBJdwpDebugValue(this, parentValue, variableName, childVariableNames, icon);
    }

        @Nullable
    @Override
    public Object getEqualityObject() {
        DBSchemaObject object = DBDebugUtil.getObject(getSourcePosition());
        IdentifierPsiElement subject = getSubject();
        String subjectString = subject == null ? null : subject.getText();

        return object == null ? null : (object.getQualifiedName() + "." + subjectString).toLowerCase();
        //return underlyingFrame.getEqualityObject();
    }
}


