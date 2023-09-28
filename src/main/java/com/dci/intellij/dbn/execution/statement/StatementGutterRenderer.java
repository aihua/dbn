package com.dci.intellij.dbn.execution.statement;

import com.dci.intellij.dbn.common.compatibility.Workaround;
import com.dci.intellij.dbn.common.util.Traces;
import com.dci.intellij.dbn.execution.statement.action.StatementGutterAction;
import com.dci.intellij.dbn.language.common.psi.ExecutablePsiElement;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class StatementGutterRenderer extends GutterIconRenderer {
    private final StatementGutterAction action;
    private final int hashCode;

    public StatementGutterRenderer(ExecutablePsiElement executablePsiElement) {
        this.action = new StatementGutterAction(executablePsiElement);
        hashCode = Objects.hashCode(executablePsiElement);
    }

    @Override
    @NotNull
    public Icon getIcon() {
        return action.getIcon();
    }

    @Override
    public boolean isNavigateAction() {
        return true;
    }

    @Override
    @Nullable
    @Workaround // TODO workaround for Idea 15 bug (showing gutter actions as intentions)
    public AnAction getClickAction() {
        return Traces.isCalledThrough(ShowIntentionsPass.class) ? null : action;
    }

    @Override
    @Nullable
    public String getTooltipText() {
        return action.getTooltipText();
    }

    @NotNull
    @Override
    public Alignment getAlignment() {
        return Alignment.RIGHT;
    }

    @Override
    public boolean equals(Object o) {
        // prevent double gutter actions
        if (o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatementGutterRenderer that = (StatementGutterRenderer) o;
        return Objects.equals(
                this.action.getExecutablePsiElement(),
                that.action.getExecutablePsiElement());
    }

    @Override
    public int hashCode() {
        // prevent double gutter actions
        return hashCode;
    }
}
