package com.dci.intellij.dbn.common.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface Presentable {
    @NotNull
    String getName();

    @Nullable
    default String getDescription() {return null;}

    @Nullable
    default Icon getIcon() {return null;}

    Presentable UNKNOWN = new Presentable() {
        @NotNull
        @Override
        public String getName() {
            return "Unknown";
        }

        @Nullable
        @Override
        public String getDescription() {
            return null;
        }

        @Nullable
        @Override
        public Icon getIcon() {
            return null;
        }
    };
}
