package com.dci.intellij.dbn.execution.compiler.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.ui.dialog.DBNDialog;
import com.dci.intellij.dbn.execution.compiler.CompileType;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CompilerTypeSelectionDialog extends DBNDialog<CompilerTypeSelectionForm> {
    private CompileType selection;  // TODO dialog result - Disposable.nullify(...)
    private DBObjectRef<DBSchemaObject> objectRef;

    public CompilerTypeSelectionDialog(Project project, @Nullable DBSchemaObject object) {
        super(project, "Compile type", true);
        setModal(true);
        setResizable(false);
        objectRef = DBObjectRef.of(object);
        //setVerticalStretch(0);
        init();
    }

    @NotNull
    @Override
    protected CompilerTypeSelectionForm createForm() {
        DBSchemaObject object = DBObjectRef.get(objectRef);
        return new CompilerTypeSelectionForm(this, object);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                new CompileKeep(),
                new CompileNormalAction(),
                new CompileDebugAction(),
                getCancelAction(),
                //getHelpAction()
        };
    }

    private class CompileKeep extends AbstractAction {
        private CompileKeep() {
            super("Keep current");
            //super("Keep current", Icons.OBEJCT_COMPILE_KEEP);
            putValue(DEFAULT_ACTION, Boolean.TRUE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selection = CompileType.KEEP;
            doOKAction();
        }
    }

    private class CompileNormalAction extends AbstractAction {
        private CompileNormalAction() {
            super("Normal", Icons.OBEJCT_COMPILE);
            //putValue(DEFAULT_ACTION, Boolean.TRUE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selection = CompileType.NORMAL;
            doOKAction();
        }
    }

    private class CompileDebugAction extends AbstractAction {
        private CompileDebugAction() {
            super("Debug", Icons.OBEJCT_COMPILE_DEBUG);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selection = CompileType.DEBUG;
            doOKAction();
        }
    }

    public CompileType getSelection() {
        return selection;
    }
}
