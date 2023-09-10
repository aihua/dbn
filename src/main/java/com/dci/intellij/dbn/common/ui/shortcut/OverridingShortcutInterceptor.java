package com.dci.intellij.dbn.common.ui.shortcut;

import com.dci.intellij.dbn.common.compatibility.Compatibility;
import com.dci.intellij.dbn.common.exception.ProcessDeferredException;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;

import java.util.Objects;

import static com.dci.intellij.dbn.common.dispose.Checks.isNotValid;

public abstract class OverridingShortcutInterceptor extends ShortcutInterceptor {
    public OverridingShortcutInterceptor(String delegateActionId) {
        super(delegateActionId);
    }

/*
    // TODO alternative invocation as of 212.* IDE builds
    @Override
    @Compatibility
    public void beforeActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event) {
        attemptDelegation(action, event);
    }
*/

    @Override
    @Compatibility
    public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        attemptDelegation(action, event);
    }

    @Override
    public void beforeEditorTyping(char c, DataContext dataContext) {
    }

    private void attemptDelegation(AnAction action, AnActionEvent event) {
        if (isNotValid(action)) return;
        if (isNotValid(event)) return;
        if (Objects.equals(delegateActionClass, action.getClass())) return; // action is being invoked (no delegation needed)
        if (!matchesDelegateShortcuts(event)) return; // event not matching delegate shortcut
        if (!canDelegateExecute(event)) return; // delegate action may be disabled
        if (!isValidContext(event)) return;

        invokeDelegateAction(event);
        throw new ProcessDeferredException("Shortcut override - Event delegated to " + getDelegateActionId());
    }
}
