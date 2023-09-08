package com.dci.intellij.dbn.common.util;

import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.context.DatabaseContext;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public final class Titles {

    public static final String PRODUCT_NAME = "DB Navigator";
    public static final String TITLE_PREFIX = PRODUCT_NAME + " - ";

    public static String signed(String title) {
        return TITLE_PREFIX + title;
    }

    public static String suffixed(String title, @Nullable DatabaseContext databaseContext) {
        if (databaseContext == null) return title;

        ConnectionHandler connection = databaseContext.getConnection();
        if (connection == null) return title;

        return title + " - " + connection.getName();
    }

    public static String prefixed(String title, @Nullable DatabaseContext databaseContext) {
        if (databaseContext == null) return title;

        ConnectionHandler connection = databaseContext.getConnection();
        if (connection == null) return title;

        return connection.getName()  + " - " + title;
    }
}
