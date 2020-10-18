package com.dci.intellij.dbn.editor.data.state.column.ui;

import com.dci.intellij.dbn.common.ui.dialog.DBNDialog;
import com.dci.intellij.dbn.editor.data.DatasetEditor;
import com.dci.intellij.dbn.editor.data.DatasetLoadInstructions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dci.intellij.dbn.editor.data.DatasetLoadInstruction.DELIBERATE_ACTION;
import static com.dci.intellij.dbn.editor.data.DatasetLoadInstruction.PRESERVE_CHANGES;
import static com.dci.intellij.dbn.editor.data.DatasetLoadInstruction.REBUILD;
import static com.dci.intellij.dbn.editor.data.DatasetLoadInstruction.USE_CURRENT_FILTER;

public class DatasetColumnSetupDialog extends DBNDialog<DatasetColumnSetupForm> {
    private static final DatasetLoadInstructions LOAD_INSTRUCTIONS = new DatasetLoadInstructions(USE_CURRENT_FILTER, PRESERVE_CHANGES, DELIBERATE_ACTION, REBUILD);
    private DatasetEditor datasetEditor;

    public DatasetColumnSetupDialog(@NotNull DatasetEditor datasetEditor) {
        super(datasetEditor.getProject(), "Column setup", true);
        this.datasetEditor = datasetEditor;
        setModal(true);
        setResizable(true);
        getCancelAction().putValue(Action.NAME, "Cancel");
        init();
    }

    @NotNull
    @Override
    protected DatasetColumnSetupForm createForm() {
        return new DatasetColumnSetupForm(this, datasetEditor);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction(),
                getHelpAction()
        };
    }

    @Override
    protected void doOKAction() {
        boolean changed = getForm().applyChanges();
        if (changed) {
            datasetEditor.loadData(LOAD_INSTRUCTIONS);
        }
        super.doOKAction();
    }

    @Override
    protected void disposeInner() {
        datasetEditor = null;
        super.disposeInner();
    }
}
