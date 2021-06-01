package com.dci.intellij.dbn.execution.explain.result.ui;

import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.ui.DBNForm;
import com.dci.intellij.dbn.common.ui.listener.MouseClickedListener;
import com.dci.intellij.dbn.common.ui.tree.TreeUtil;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.data.editor.ui.UserValueHolder;
import com.dci.intellij.dbn.data.editor.ui.UserValueHolderImpl;
import com.dci.intellij.dbn.data.grid.color.BasicTableTextAttributes;
import com.dci.intellij.dbn.data.preview.LargeValuePreviewPopup;
import com.dci.intellij.dbn.execution.explain.result.ExplainPlanEntry;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.codeInsight.template.impl.TemplateColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupAdapter;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableCellRenderer;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.math.BigDecimal;

public class ExplainPlanTreeTable extends TreeTable implements StatefulDisposable {
    private static final int MAX_TREE_COLUMN_WIDTH = 900;
    private static final int MAX_COLUMN_WIDTH = 250;
    private static final int MIN_COLUMN_WIDTH = 10;

    private final ProjectRef project;
    private final SimpleTextAttributes operationAttributes;
    private JBPopup largeValuePopup;

    ExplainPlanTreeTable(DBNForm parent, ExplainPlanTreeTableModel treeTableModel) {
        super(treeTableModel);
        this.project = ProjectRef.of(parent.getProject());

        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        TextAttributes attributes = scheme.getAttributes(TemplateColors.TEMPLATE_VARIABLE_ATTRIBUTES);
        operationAttributes = new SimpleTextAttributes(null, attributes.getForegroundColor(), null, SimpleTextAttributes.STYLE_PLAIN);
        setTreeCellRenderer(treeCellRenderer);
        setDefaultRenderer(String.class, tableCellRenderer);
        setDefaultRenderer(BigDecimal.class, tableCellRenderer);
        setAutoResizeMode(AUTO_RESIZE_OFF);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        SimpleTextAttributes plainDataAttributes = BasicTableTextAttributes.get().getPlainData(false, false);
        setBackground(plainDataAttributes.getBgColor());

        Font font = getFont();
        FontRenderContext fontRenderContext = getFontMetrics(font).getFontRenderContext();
        LineMetrics lineMetrics = font.getLineMetrics("ABC", fontRenderContext);
        int fontHeight = Math.round(lineMetrics.getHeight());
        setRowHeight(fontHeight + 2);

        final TreeTableTree tree = getTree();
        tree.setOpaque(false);
        tree.setBackground(plainDataAttributes.getBgColor());
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                accommodateColumnsSize();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                accommodateColumnsSize();
            }
        });
        accommodateColumnsSize();
        TreeUtil.expandAll(tree);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                TreePath pathAtMousePosition = TreeUtil.getPathAtMousePosition(tree);
                if (pathAtMousePosition != null) {
                    Object lastPathComponent = pathAtMousePosition.getLastPathComponent();
                }
            }
        });

        addMouseListener(MouseClickedListener.create(e -> showCellValuePopup()));

        Disposer.register(parent, this);
    }

    @NotNull
    Project getProject() {
        return project.ensure();
    }

    private final ColoredTreeCellRenderer treeCellRenderer = new ColoredTreeCellRenderer() {
        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            super.processMouseMotionEvent(e);
        }

        @Override
        public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            try {
                ExplainPlanEntry entry = (ExplainPlanEntry) value;

                DBObjectRef<?> objectRef = entry.getObjectRef();
                SimpleTextAttributes selectedCellAttributes = SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES;
                if (objectRef != null) {
                    setIcon(objectRef.objectType.getIcon());
                    append(objectRef.getPath() + " - ", selected ? selectedCellAttributes : SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }

                String operation = entry.getOperation();
                String options = entry.getOperationOptions();
                append(operation, selected ? selectedCellAttributes.derive(SimpleTextAttributes.STYLE_BOLD, null, null, null) : operationAttributes);
                if (StringUtil.isNotEmpty(options)) {
                    SimpleTextAttributes regularAttributes = options.equals("FULL") ?
                            SimpleTextAttributes.ERROR_ATTRIBUTES :
                            SimpleTextAttributes.GRAYED_ATTRIBUTES;
                    append(" (" + options.toLowerCase() + ")", selected ? selectedCellAttributes : regularAttributes);
                }
                setBorder(null);
            } catch (ProcessCanceledException ignore) {}
        }
    };

    @Override
    public TreeTableCellRenderer createTableRenderer(TreeTableModel treeTableModel) {
        return new ExplainPlanTreeTableCellRenderer(this, getTree());
    }

    private final ColoredTableCellRenderer tableCellRenderer = new ColoredTableCellRenderer() {
        @Override
        protected void customizeCellRenderer(@NotNull JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            SimpleTextAttributes attributes = selected ?
                    SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES :
                    BasicTableTextAttributes.get().getPlainData(false, false);
            if (value instanceof DBObjectRef) {
                DBObjectRef objectRef = (DBObjectRef) value;
                append(objectRef.getPath(), attributes);
            } else if (value instanceof String){
                append((String) value, attributes);
            } else if (value instanceof BigDecimal) {
                BigDecimal bigDecimal = (BigDecimal) value;
                append(bigDecimal.toPlainString(), attributes);
                setTextAlign(SwingConstants.RIGHT);
            }

            setBorder(null);
            //setBorder(new CustomLineBorder(DBNTable.GRID_COLOR, 0, 0, 1, 1));
            ExplainPlanTreeTableModel tableModel = (ExplainPlanTreeTableModel) getTableModel();
            if (tableModel.isLargeValue(column)) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }
    };

    private void showCellValuePopup() {
        if (largeValuePopup != null) {
            largeValuePopup.cancel();
            largeValuePopup = null;
        }
        if (getSelectedColumnCount() == 1 && getSelectedRowCount() == 1) {
            int rowIndex = getSelectedRows()[0];
            int columnIndex = getSelectedColumns()[0];
            ExplainPlanTreeTableModel tableModel = (ExplainPlanTreeTableModel) getTableModel();
            Object value = getValueAt(rowIndex, columnIndex);
            if (tableModel.isLargeValue(columnIndex) && value instanceof String && StringUtil.isNotEmpty((String) value) && this.isShowing()) {
                Rectangle cellRect = getCellRect(rowIndex, columnIndex, true);

                TableColumn column = getColumnModel().getColumn(columnIndex);
                UserValueHolder<Object> userValueHolder = new UserValueHolderImpl<>(tableModel.getColumnName(columnIndex), DBObjectType.COLUMN, null, tableModel.getProject());
                userValueHolder.setUserValue(value);

                int preferredWidth = column.getWidth();
                LargeValuePreviewPopup viewer = new LargeValuePreviewPopup(getProject(), this, userValueHolder, preferredWidth);
                Point location = cellRect.getLocation();
                location.setLocation(location.getX() + 4, location.getY() + 20);

                largeValuePopup = viewer.show(this, location);
                largeValuePopup.addListener(
                        new JBPopupAdapter() {
                            @Override
                            public void onClosed(@NotNull LightweightWindowEvent event) {
                                largeValuePopup.cancel();
                                largeValuePopup = null;
                            }
                        }
                );

                Disposer.register(ExplainPlanTreeTable.this, largeValuePopup);
            }
        }
    }

    public void accommodateColumnsSize() {
        for (int columnIndex = 0; columnIndex < getColumnCount(); columnIndex++){
            accommodateColumnSize(columnIndex, 22);
        }
    }

    public void accommodateColumnSize(int colIndex, int span) {
        TableColumn column = getColumnModel().getColumn(colIndex);
        int columnIndex = column.getModelIndex();
        int preferredWidth = 0;

        // header
        JTableHeader tableHeader = getTableHeader();
        if (tableHeader != null) {
            Object headerValue = column.getHeaderValue();
            TableCellRenderer headerCellRenderer = column.getHeaderRenderer();
            if (headerCellRenderer == null) headerCellRenderer = tableHeader.getDefaultRenderer();
            Component headerComponent = headerCellRenderer.getTableCellRendererComponent(this, headerValue, false, false, 0, columnIndex);
            if (headerComponent.getPreferredSize().width > preferredWidth)
                preferredWidth = headerComponent.getPreferredSize().width;
        }

        // rows
        int maxColumnWidth = columnIndex == 0 ? MAX_TREE_COLUMN_WIDTH : MAX_COLUMN_WIDTH;
        for (int rowIndex =0; rowIndex < getModel().getRowCount(); rowIndex++) {
            if (preferredWidth > maxColumnWidth) {
                break;
            }
            Object value = getModel().getValueAt(rowIndex, columnIndex);
            TableCellRenderer renderer = getCellRenderer(rowIndex, columnIndex);

            if (renderer != null) {
                Component component = renderer.getTableCellRendererComponent(this, value, false, false, rowIndex, columnIndex);
                if (component.getPreferredSize().width > preferredWidth) {
                    preferredWidth = component.getPreferredSize().width;
                }
            }
        }

        if (preferredWidth > maxColumnWidth) {
            preferredWidth = maxColumnWidth;
        }

        if (preferredWidth < MIN_COLUMN_WIDTH) {
            preferredWidth = MIN_COLUMN_WIDTH;
        }

        preferredWidth = preferredWidth + span;

        if (column.getPreferredWidth() != preferredWidth)  {
            column.setPreferredWidth(preferredWidth);
        }

    }

    /********************************************************
     *                    Disposable                        *
     ********************************************************/
    @Getter
    private boolean disposed;

    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            nullify();
        }
    }
}
