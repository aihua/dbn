package com.dci.intellij.dbn.execution.logging.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.component.ComponentBase;
import com.dci.intellij.dbn.common.util.MessageUtil;
import com.dci.intellij.dbn.execution.logging.DatabaseLoggingResult;
import com.dci.intellij.dbn.execution.logging.LogOutputContext;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dci.intellij.dbn.common.message.MessageCallback.when;

public class DatabaseLogOutputKillAction extends AbstractDatabaseLoggingAction implements ComponentBase {
    public DatabaseLogOutputKillAction() {
        super("Kill Process", Icons.KILL_PROCESS);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DatabaseLoggingResult loggingResult) {
        LogOutputContext context = loggingResult.getContext();
        if (context.isActive()) {
            MessageUtil.showQuestionDialog(
                    project,
                    "Kill process",
                    "This will interrupt the script execution process. \nAre you sure you want to continue?",
                    MessageUtil.OPTIONS_YES_NO, 0,
                    option -> when(option == 0, () -> context.stop()));

        } else {
            context.stop();
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DatabaseLoggingResult loggingResult) {
        presentation.setText("Kill Process");

        LogOutputContext context = loggingResult == null ? null : loggingResult.getContext();
        boolean enabled = context != null && context.isActive();
        boolean visible = context != null && context.getProcess() != null;
        presentation.setEnabled(enabled);
        presentation.setVisible(visible);

    }
}
