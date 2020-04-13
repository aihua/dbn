package com.dci.intellij.dbn.execution.compiler;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.option.InteractiveOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public enum CompileType implements InteractiveOption {
    NORMAL("Normal", Icons.OBEJCT_COMPILE, true),
    DEBUG("Debug", Icons.OBEJCT_COMPILE_DEBUG, true),
    KEEP("Keep existing", null/*Icons.OBEJCT_COMPILE_KEEP*/, true),
    ASK("Ask", null/*Icons.OBEJCT_COMPILE_ASK*/, false);

    private final String name;
    private final Icon icon;
    private final boolean persistable;

    CompileType(String name, Icon icon, boolean persistable) {
        this.name = name;
        this.icon = icon;
        this.persistable = persistable;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }


    @Nullable
    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public boolean isCancel() {
        return false;
    }

    @Override
    public boolean isAsk() {
        return this == ASK;
    }


    public static CompileType get(String name) {
        for (CompileType compileType : CompileType.values()) {
            if (compileType.name.equals(name) || compileType.name().equals(name)) {
                return compileType;
            }
        }
        return null;
    }}
