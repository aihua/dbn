package com.dci.intellij.dbn.debugger.jdwp.frame;

import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.util.CollectionUtil;
import com.dci.intellij.dbn.debugger.DBDebugUtil;
import com.dci.intellij.dbn.debugger.jdwp.ManagedThreadCommand;
import com.dci.intellij.dbn.debugger.jdwp.process.DBJdwpDebugProcess;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.intellij.debugger.impl.PrioritizedTask.Priority.LOW;

@Getter
public class DBJdwpDebugExecutionStack extends XExecutionStack {
    private final DBJdwpDebugSuspendContext suspendContext;
    private final List<DBJdwpDebugStackFrame> stackFrames = CollectionUtil.createConcurrentList();

    private final Latent<DBJdwpDebugStackFrame> topStackFrame = Latent.basic(() -> {
        XExecutionStack underlyingStack = getUnderlyingStack();
        XStackFrame topFrame = underlyingStack == null ? null : underlyingStack.getTopFrame();
        return getFrame((JavaStackFrame) topFrame);
    });

    DBJdwpDebugExecutionStack(DBJdwpDebugSuspendContext suspendContext) {
        // WORKAROUND hide the single value "threads" dropdown
        // super(suspendContext.getDebugProcess().getName(), suspendContext.getDebugProcess().getIcon());
        super("", null);
        this.suspendContext = suspendContext;
    }

    @NotNull
    private DebugProcessImpl getDebugProcess() {
        return getSuspendContext().getDebugProcess().getDebuggerSession().getProcess();
    }

    @Nullable
    private XExecutionStack getUnderlyingStack() {
        return suspendContext.getUnderlyingContext().getActiveExecutionStack();
    }

    @Override
    public XStackFrame getTopFrame() {
        return topStackFrame.get();
    }

    private DBJdwpDebugStackFrame getFrame(JavaStackFrame underlyingFrame) {
        for (DBJdwpDebugStackFrame stackFrame : stackFrames) {
            if (Objects.equals(stackFrame.getUnderlyingFrame(), underlyingFrame)) {
                return stackFrame;
            }
        }

        DBJdwpDebugProcess<?> debugProcess = suspendContext.getDebugProcess();
        DBJdwpDebugStackFrame stackFrame = new DBJdwpDebugStackFrame(debugProcess, underlyingFrame, stackFrames.size());
        stackFrames.add(stackFrame);
        return stackFrame;
    }

    @Override
    public void computeStackFrames(final int firstFrameIndex, final XStackFrameContainer container) {
        XExecutionStack underlyingStack = getUnderlyingStack();
        if (underlyingStack == null) return;

        ManagedThreadCommand.schedule(getDebugProcess(), LOW, () ->
                computeStackFrames(firstFrameIndex, container, underlyingStack));
    }

    private void computeStackFrames(int firstFrameIndex, XStackFrameContainer container, XExecutionStack underlyingStack) {
        XStackFrameContainer fakeContainer = new XStackFrameContainer() {
            @Override
            public void addStackFrames(@NotNull List<? extends XStackFrame> stackFrames, boolean last) {
                if (stackFrames.isEmpty()) return;

                List<DBJdwpDebugStackFrame> frames = new ArrayList<>();
                for (XStackFrame underlyingFrame : stackFrames) {
                    DBJdwpDebugStackFrame frame = getFrame((JavaStackFrame) underlyingFrame);
                    XSourcePosition sourcePosition = frame.getSourcePosition();
                    //VirtualFile virtualFile = DBDebugUtil.getSourceCodeFile(sourcePosition);
                    //DBSchemaObject object = DBDebugUtil.getObject(sourcePosition);
                    frames.add(frame);
                    last = last || DBDebugUtil.getObject(sourcePosition) == null;
                }

                container.addStackFrames(frames, last) ;
            }

            @Override
            public boolean isObsolete() {
                return container.isObsolete();
            }

            @Override
            public void errorOccurred(@NotNull String errorMessage) {

            }
        };

        underlyingStack.computeStackFrames(firstFrameIndex, fakeContainer);
    }
}
