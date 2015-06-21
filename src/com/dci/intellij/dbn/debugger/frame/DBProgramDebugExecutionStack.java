package com.dci.intellij.dbn.debugger.frame;

import com.dci.intellij.dbn.database.common.debug.DebuggerRuntimeInfo;
import com.dci.intellij.dbn.database.common.debug.ExecutionBacktraceInfo;
import com.dci.intellij.dbn.debugger.DBProgramDebugProcess;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;

import java.util.ArrayList;
import java.util.List;

public class DBProgramDebugExecutionStack extends XExecutionStack {
    private DBProgramDebugStackFrame topStackFrame;
    private DBProgramDebugProcess debugProcess;

    protected DBProgramDebugExecutionStack(DBProgramDebugProcess debugProcess) {
        super(debugProcess.getName(), debugProcess.getIcon());
        this.debugProcess = debugProcess;
        ExecutionBacktraceInfo backtraceInfo = debugProcess.getBacktraceInfo();
        int frameNumber = backtraceInfo == null ? 1 : backtraceInfo.getFrames().size() + 1;
        topStackFrame = new DBProgramDebugStackFrame(debugProcess, debugProcess.getRuntimeInfo(), frameNumber);

    }



    @Override
    public XStackFrame getTopFrame() {
        return topStackFrame;
    }

    @Override
    public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
        List<DBProgramDebugStackFrame> frames = new ArrayList<DBProgramDebugStackFrame>();
        ExecutionBacktraceInfo backtraceInfo = debugProcess.getBacktraceInfo();
        if (backtraceInfo != null) {
            int frameNumber = backtraceInfo.getFrames().size() + 1;
            for (DebuggerRuntimeInfo runtimeInfo : backtraceInfo.getFrames()) {
                if (!runtimeInfo.equals(debugProcess.getRuntimeInfo())) {
                    DBProgramDebugStackFrame frame = new DBProgramDebugStackFrame(debugProcess, runtimeInfo, frameNumber);
                    frames.add(frame);
                }
                frameNumber--;
            }
            container.addStackFrames(frames, true) ;
        }
    }
}
