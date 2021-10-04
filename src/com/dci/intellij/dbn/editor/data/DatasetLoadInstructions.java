package com.dci.intellij.dbn.editor.data;

import com.dci.intellij.dbn.common.property.PropertyHolderImpl;

public class DatasetLoadInstructions extends PropertyHolderImpl<DatasetLoadInstruction>{


    public DatasetLoadInstructions(DatasetLoadInstruction ... instructions) {
        for (DatasetLoadInstruction instruction : instructions) {
            set(instruction, true);
        }
    }

    public static DatasetLoadInstructions clone(DatasetLoadInstructions source) {
        DatasetLoadInstructions instructions = new DatasetLoadInstructions();
        instructions.computed(source.computed());
        return instructions;
    }

    @Override
    protected DatasetLoadInstruction[] properties() {
        return DatasetLoadInstruction.values();
    }

    public boolean isUseCurrentFilter() {
        return is(DatasetLoadInstruction.USE_CURRENT_FILTER);
    }

    public boolean isPreserveChanges() {
        return is(DatasetLoadInstruction.PRESERVE_CHANGES);
    }

    public boolean isDeliberateAction() {
        return is(DatasetLoadInstruction.DELIBERATE_ACTION);
    }

    public boolean isRebuild() {
        return is(DatasetLoadInstruction.REBUILD);
    }

    public void setUseCurrentFilter(boolean value) {
        set(DatasetLoadInstruction.USE_CURRENT_FILTER, value);
    }

    public void setKeepChanges(boolean value) {
        set(DatasetLoadInstruction.PRESERVE_CHANGES, value);
    }

    public void setDeliberateAction(boolean value) {
        set(DatasetLoadInstruction.DELIBERATE_ACTION, value);
    }

    public void setRebuild(boolean value) {
        set(DatasetLoadInstruction.REBUILD, value);
    }
}
