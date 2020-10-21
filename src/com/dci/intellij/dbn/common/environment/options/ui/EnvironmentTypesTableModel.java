package com.dci.intellij.dbn.common.environment.options.ui;

import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.environment.EnvironmentTypeBundle;
import com.dci.intellij.dbn.common.environment.options.listener.EnvironmentConfigLocalListener;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.ui.table.DBNEditableTableModel;
import com.dci.intellij.dbn.common.util.Safe;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;

public class EnvironmentTypesTableModel extends DBNEditableTableModel {
    private EnvironmentTypeBundle environmentTypes;
    private final ProjectRef project;

    EnvironmentTypesTableModel(Project project, EnvironmentTypeBundle environmentTypes) {
        this.project = ProjectRef.of(project);
        this.environmentTypes = new EnvironmentTypeBundle(environmentTypes);
        addTableModelListener(defaultModelListener);
    }

    public EnvironmentTypeBundle getEnvironmentTypes() {
        return environmentTypes;
    }

    public void setEnvironmentTypes(EnvironmentTypeBundle environmentTypes) {
        this.environmentTypes = new EnvironmentTypeBundle(environmentTypes);
        notifyListeners(0, environmentTypes.size(), -1);
    }

    @Override
    public int getRowCount() {
        return environmentTypes.size();
    }
    
    private final TableModelListener defaultModelListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            Project project = getProject();
            ProjectEvents.notify(project,
                    EnvironmentConfigLocalListener.TOPIC,
                    (listener) -> listener.settingsChanged(environmentTypes));
        }
    };

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnIndex == 0 ? "Name" :
               columnIndex == 1 ? "Description" :
               columnIndex == 2 ? "Readonly Data" :
               columnIndex == 3 ? "Readonly Code" :
               columnIndex == 4 ? "Color" : null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? String.class :
            columnIndex == 1 ? String.class :
            columnIndex == 2 ? Boolean.class :
            columnIndex == 3 ? Boolean.class:
            columnIndex == 4 ? Color.class : String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        EnvironmentType environmentType = getEnvironmentType(rowIndex);
        return
           columnIndex == 0 ? environmentType.getName() :
           columnIndex == 1 ? environmentType.getDescription() :
           columnIndex == 2 ? environmentType.isReadonlyData() :
           columnIndex == 3 ? environmentType.isReadonlyCode() :
           columnIndex == 4 ? environmentType.getColor() : null;
    }

    @Override
    public void setValueAt(Object o, int rowIndex, int columnIndex) {
        Object actualValue = getValueAt(rowIndex, columnIndex);
        if (!Safe.equal(actualValue, o)) {
            EnvironmentType environmentType = environmentTypes.get(rowIndex);
            if (columnIndex == 0) {
                environmentType.setName((String) o);
            } else if (columnIndex == 1) {
                environmentType.setDescription((String) o);
            } else if (columnIndex == 2) {
                environmentType.setReadonlyData((Boolean) o);
            } else if (columnIndex == 3) {
                environmentType.setReadonlyCode((Boolean) o);
            } else if (columnIndex == 4) {
                Color color = (Color) o;
                environmentType.setColor(color);
            }

            notifyListeners(rowIndex, rowIndex, columnIndex);
        }
    }

    private EnvironmentType getEnvironmentType(int rowIndex) {
        while (environmentTypes.size() <= rowIndex) {
            environmentTypes.add(new EnvironmentType());
        }
        return environmentTypes.get(rowIndex);
    }

    @Override
    public void insertRow(int rowIndex) {
        environmentTypes.add(rowIndex, new EnvironmentType());
        notifyListeners(rowIndex, environmentTypes.size()-1, -1);
    }

    @Override
    public void removeRow(int rowIndex) {
        if (environmentTypes.size() > rowIndex) {
            environmentTypes.remove(rowIndex);
            notifyListeners(rowIndex, environmentTypes.size()-1, -1);
        }
    }

    public void validate() throws ConfigurationException {
        for (EnvironmentType environmentType : environmentTypes) {
            if (StringUtil.isEmpty(environmentType.getName())) {
                throw new ConfigurationException("Please provide names for all environment types.");
            }
        }
    }
}
