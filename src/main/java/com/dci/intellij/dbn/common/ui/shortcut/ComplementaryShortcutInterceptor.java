package com.dci.intellij.dbn.common.ui.shortcut;

import com.dci.intellij.dbn.common.compatibility.Compatibility;
import com.dci.intellij.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;

import java.util.Objects;

import static com.dci.intellij.dbn.common.dispose.Checks.isNotValid;

public abstract class ComplementaryShortcutInterceptor extends ShortcutInterceptor {

    public ComplementaryShortcutInterceptor(String delegateActionId) {
        super(delegateActionId);
    }

/*
    // TODO alternative invocation as of 212.* IDE builds
    @Override
    @Compatibility
    public void afterActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event, @NotNull AnActionResult result) {
        attemptDelegation(action, event);
    }
*/

    @Override
    @Compatibility
    public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        attemptDelegation(action, event);
    }

    @Override
    @Compatibility
    public void beforeActionPerformed(AnAction anAction, DataContext dataContext, AnActionEvent anActionEvent) {

    }


    private void attemptDelegation(AnAction action, AnActionEvent event) {
        if (isNotValid(action)) return;
        if (isNotValid(event)) return;
        if (Objects.equals(delegateActionClass, action.getClass())) return; // action invoked already
        if (!Actions.isConsumed(event)) return; // event not consumed, there is still hope
        if (!matchesDelegateShortcuts(event)) return; // event not matching delegate shortcut
        if (!canDelegateExecute(event)) return; // delegate action may be disabled
        if (!isValidContext(event)) return;

        invokeDelegateAction(event);
    }

}
