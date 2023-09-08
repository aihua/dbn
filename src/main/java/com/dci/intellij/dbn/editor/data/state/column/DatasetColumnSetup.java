package com.dci.intellij.dbn.editor.data.state.column;

import com.dci.intellij.dbn.common.state.PersistentStateElement;
import com.dci.intellij.dbn.common.util.Cloneable;
import com.dci.intellij.dbn.object.DBColumn;
import com.dci.intellij.dbn.object.DBDataset;
import com.dci.intellij.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.dci.intellij.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
@EqualsAndHashCode
public class DatasetColumnSetup implements PersistentStateElement, Cloneable<DatasetColumnSetup> {
    private List<DatasetColumnState> columnStates = new ArrayList<>();

    public void init(@Nullable List<String> columnNames, @NotNull DBDataset dataset) {
        if (columnNames == null) {
            columnNames = dataset.getChildObjectNames(DBObjectType.COLUMN);
        }
        List<DatasetColumnState> columnStates = new ArrayList<>();
        for (DBColumn column : dataset.getColumns()) {
            String columnName = column.getName();
            if (!column.isHidden() && columnNames.contains(columnName)) {
                DatasetColumnState columnsState = getColumnState(columnName);
                if (columnsState == null) {
                    columnsState = new DatasetColumnState(column);
                } else {
                    columnsState.init(column);
                }
                columnStates.add(columnsState);
            }
        }

        Collections.sort(columnStates);
        this.columnStates = columnStates;
    }


    public DatasetColumnState getColumnState(String columnName) {
        for (DatasetColumnState columnsState : columnStates) {
            if (Objects.equals(columnName, columnsState.getName())) {
                return columnsState;
            }
        }
        return null;
    }

    @Override
    public void readState(Element element) {
        if (element != null) {
            for (Element child : element.getChildren()) {
                String columnName = stringAttribute(child, "name");
                DatasetColumnState columnState = getColumnState(columnName);
                if (columnState == null) {
                    columnState = new DatasetColumnState(child);
                    columnStates.add(columnState);
                } else {
                    columnState.readState(child);
                }
            }
            Collections.sort(columnStates);
        }
    }

    @Override
    public void writeState(Element element) {
        for (DatasetColumnState columnState : columnStates) {
            Element childElement = new Element("column");
            element.addContent(childElement);
            columnState.writeState(childElement);
        }
    }

    public void moveColumn(int fromIndex, int toIndex) {
        int visibleFromIndex = fromIndex;
        int visibleToIndex = toIndex;

        int visibleIndex = -1;
        for (int i=0; i< columnStates.size(); i++) {
            DatasetColumnState columnState = columnStates.get(i);
            if (columnState.isVisible()) {
                visibleIndex++;
                if (visibleIndex == fromIndex) visibleFromIndex = i;
                if (visibleIndex == toIndex) visibleToIndex = i;
            }
        }

        DatasetColumnState columnState = columnStates.remove(visibleFromIndex);
        columnStates.add(visibleToIndex, columnState);
        for (int i=0; i< columnStates.size(); i++) {
            columnStates.get(i).setPosition((short) i);
        }
    }

    public boolean isVisible(String name) {
        DatasetColumnState columnState = getColumnState(name);
        return columnState == null || columnState.isVisible();
    }

    @Override
    public DatasetColumnSetup clone() {
        DatasetColumnSetup clone = new DatasetColumnSetup();
        for (DatasetColumnState columnState : columnStates) {
            clone.columnStates.add(columnState.clone());
        }

        return clone;
    }
}
