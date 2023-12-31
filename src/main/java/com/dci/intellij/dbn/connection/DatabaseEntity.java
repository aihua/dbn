package com.dci.intellij.dbn.connection;

import com.dci.intellij.dbn.common.content.DynamicContent;
import com.dci.intellij.dbn.common.content.DynamicContentType;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.ui.Presentable;
import com.dci.intellij.dbn.common.util.Unsafe;
import com.dci.intellij.dbn.connection.context.DatabaseContextBase;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.DBObjectBundle;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DatabaseEntity extends DatabaseContextBase, StatefulDisposable, Presentable {

    @NotNull
    default String getQualifiedName() {
        return getName();
    }

    @NotNull
    Project getProject();

    @Nullable
    default <E extends DatabaseEntity> E getParentEntity() {
        return null;
    }

    //@NotNull
    default <E extends DatabaseEntity> E ensureParentEntity() {
        return Failsafe.nn(getParentEntity());
    }

    @Nullable
    default <E extends DatabaseEntity> E getUndisposedEntity() {
        return Unsafe.cast(this);
    }

    @Nullable
    default DynamicContent<?> getDynamicContent(DynamicContentType<?> dynamicContentType) {
        return null;
    }

    default DynamicContentType<?> getDynamicContentType() {
        return null;
    }

    @NotNull
    @Override
    default ConnectionHandler getConnection() {
        throw new UnsupportedOperationException();
    };

    default boolean isObject() {
        return this instanceof DBObject;
    }

    default boolean isObjectBundle() {
        return this instanceof DBObjectBundle;
    }
}
