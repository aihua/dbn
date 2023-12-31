package com.dci.intellij.dbn.editor.data.state.sorting;

import com.dci.intellij.dbn.data.sorting.SortingState;
import com.dci.intellij.dbn.object.DBDataset;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;

public class DatasetSortingState extends SortingState {
    private final DBObjectRef<DBDataset> datasetRef;

    public DatasetSortingState(DBDataset dataset) {
        this.datasetRef = DBObjectRef.of(dataset);
    }

    public DBDataset getDataset() {
        return DBObjectRef.get(datasetRef);
    }


}
