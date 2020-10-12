package com.dci.intellij.dbn.data.model;

import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DataModelRow<
        M extends DataModel<? extends DataModelRow<M, C>, C>,
        C extends DataModelCell<? extends DataModelRow<M, C>, M>>
        extends StatefulDisposable {

    List<C> getCells();

    @Nullable
    C getCell(String columnName);

    @Nullable
    Object getCellValue(String columnName);

    @Nullable
    C getCellAtIndex(int index);

    int getIndex();

    void setIndex(int index);

    @NotNull
    M getModel();
}
