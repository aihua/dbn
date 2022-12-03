package com.dci.intellij.dbn.execution.explain.result.ui;

import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.execution.explain.result.ExplainPlanEntry;
import com.dci.intellij.dbn.execution.explain.result.ExplainPlanResult;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import lombok.Getter;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

public class ExplainPlanTreeTableModel implements TreeTableModel, Disposable {
    private final ExplainPlanResult result;

    public ExplainPlanTreeTableModel(ExplainPlanResult result) {
        this.result = result;

        Disposer.register(this, result);
    }

    private final Column[] COLUMNS = new Column[]{
            Column.create("OPERATION", TreeTableModel.class, false, entry -> {
                String options = entry.getOperationOptions();
                return this; /*entry.getOperation() + (StringUtil.isEmpty(options) ? "" : "(" + options + ")");*/
            }),
/*
            Column.create("OBJECT", DBObjectRef.class, false, entry -> entry.getObjectRef()),
            Column.create("DEPTH", BigDecimal.class, false, entry -> entry.getDepth()),
            Column.create("POSITION", BigDecimal.class, false, entry -> entry.getPosition()),
*/
            Column.create("COST", BigDecimal.class, false, entry -> entry.getCost()),
            Column.create("CARDINALITY", BigDecimal.class, false, entry -> entry.getCardinality()),
            Column.create("BYTES", BigDecimal.class, false, entry -> entry.getBytes()),
            Column.create("CPU_COST", BigDecimal.class, false, entry -> entry.getCpuCost()),
            Column.create("IO_COST", BigDecimal.class, false, entry -> entry.getIoCost()),
            Column.create("ACCESS_PREDICATES", String.class, true, entry -> entry.getAccessPredicates()),
            Column.create("FILTER_PREDICATES", String.class, true, entry -> entry.getFilterPredicates()),
            Column.create("PROJECTION", String.class, true, entry -> entry.getProjection()),
    };

    public Project getProject() {
        return result.getProject();
    }

    /***************************************************************
     *                         TableModel                          *
     ***************************************************************/

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column].getName();
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return COLUMNS[column].getClazz();
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if (node instanceof ExplainPlanEntry) {
            ExplainPlanEntry entry = (ExplainPlanEntry) node;
            return COLUMNS[column].getValue(entry);
        }
        return null;
    }

    @Override public boolean isCellEditable(Object node, int column) {return false;}
    @Override public void setValueAt(Object aValue, Object node, int column) {}
    @Override public void setTree(JTree tree) {}
    boolean isLargeValue(int column) {
        return COLUMNS[column].isLarge();
    }

    /***************************************************************
     *                          TreeModel                          *
     ***************************************************************/
    @Override
    public Object getRoot() {
        return result.getRoot();
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof ExplainPlanEntry) {
            ExplainPlanEntry entry = (ExplainPlanEntry) parent;
            return entry.getChildren().get(index);
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof ExplainPlanEntry) {
            ExplainPlanEntry entry = (ExplainPlanEntry) parent;
            List<ExplainPlanEntry> children = entry.getChildren();
            return children == null ? 0 : children.size();
        }
        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {}

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof ExplainPlanEntry && child instanceof ExplainPlanEntry) {
            ExplainPlanEntry parentEntry = (ExplainPlanEntry) parent;
            ExplainPlanEntry childEntry = (ExplainPlanEntry) child;
            return parentEntry.getChildren().indexOf(childEntry);
        }
        return -1;
    }

    @Override public void addTreeModelListener(TreeModelListener l) {}
    @Override public void removeTreeModelListener(TreeModelListener l) {}

    @Getter
    private static abstract class Column {
        private final String name;
        private final Class<?> clazz;
        private boolean large;

        public Column(String name, Class<?> clazz) {
            this.name = name;
            this.clazz = clazz;
        }

        public Column(String name, Class<?> clazz, boolean large) {
            this.name = name;
            this.clazz = clazz;
            this.large = large;
        }

        public abstract Object getValue(ExplainPlanEntry entry);

        public static Column create(String name, Class<?> clazz, boolean large, Function<ExplainPlanEntry, Object> valueProvider) {
            return new Column(name, clazz, large) {
                @Override
                public Object getValue(ExplainPlanEntry entry) {
                    return valueProvider.apply(entry);
                }
            };
        }
    }

    @Override
    public void dispose() {
    }
}
