package com.dci.intellij.dbn.common.ui.dialog;

import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.util.TimeUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;

public abstract class DialogWithTimeout extends DBNDialog<DialogWithTimeoutForm>{
    private final Timer timeoutTimer;
    private int secondsLeft;

    protected DialogWithTimeout(Project project, String title, boolean canBeParent, int timeoutSeconds) {
        super(project, title, canBeParent);
        secondsLeft = timeoutSeconds;
        timeoutTimer = new Timer("DBN - Timeout Dialog Task [" + getProject().getName() + "]");
        timeoutTimer.schedule(new TimeoutTask(), TimeUtil.Millis.ONE_SECOND, TimeUtil.Millis.ONE_SECOND);
    }

    @NotNull
    @Override
    protected DialogWithTimeoutForm createComponent() {
        return new DialogWithTimeoutForm(secondsLeft);
    }

    @Override
    protected void init() {
        getComponent().setContentComponent(createContentComponent());
        super.init();
    }

    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            try {
                if (secondsLeft > 0) {
                    secondsLeft = secondsLeft -1;
                    getComponent().updateTimeLeft(secondsLeft);
                    if (secondsLeft == 0) {
                        Dispatch.run(() -> doDefaultAction());
                    }
                }
            } catch (ProcessCanceledException ignore) {}
        }
    }

    protected abstract JComponent createContentComponent();

    public abstract void doDefaultAction();

    @Override
    public void disposeInner() {
        Disposer.dispose(timeoutTimer);
        super.disposeInner();
    }

}
