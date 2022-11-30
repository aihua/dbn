package com.dci.intellij.dbn.common.ui.table;

import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.dispose.SafeDisposer;
import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.FontMetrics;
import com.dci.intellij.dbn.common.ui.component.DBNComponent;
import com.dci.intellij.dbn.common.ui.util.Mouse;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.data.grid.ui.table.basic.BasicTableHeaderRenderer;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.keyFMap.KeyFMap;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.dci.intellij.dbn.common.dispose.Failsafe.nd;
import static com.dci.intellij.dbn.common.dispose.SafeDisposer.replace;

public abstract class DBNTable<T extends DBNTableModel> extends JTable implements StatefulDisposable, UserDataHolder {
    private static final int MAX_COLUMN_WIDTH = 300;
    private static final int MIN_COLUMN_WIDTH = 10;

    private final WeakRef<DBNComponent> parentComponent;

    private int rowVerticalPadding;
    private double scrollDistance;
    private KeyFMap userData = KeyFMap.EMPTY_MAP;

    private Timer scrollTimer;
    private final Latent<JBScrollPane> scrollPane = Latent.weak(() -> UIUtil.getParentOfType(JBScrollPane.class, DBNTable.this));
    private final Latent<DBNTableGutter<?>> tableGutter = Latent.weak(() -> createTableGutter());
    private final FontMetrics metricsCache = new FontMetrics(this);

    public DBNTable(DBNComponent parent, T tableModel, boolean showHeader) {
        super(nd(tableModel));
        this.parentComponent = WeakRef.of(nd(parent));

        setGridColor(Colors.getTableGridColor());
        Font font = getFont();//UIUtil.getListFont();
        setFont(font);
        setBackground(Colors.getTextFieldBackground());
        setTransferHandler(DBNTableTransferHandler.INSTANCE);

        adjustRowHeight(1);

        JTableHeader tableHeader = getTableHeader();
        if (!showHeader) {
            tableHeader.setVisible(false);
            tableHeader.setPreferredSize(new Dimension(-1, 0));
        } else {
            tableHeader.setBackground(Colors.getPanelBackground());
            tableHeader.setBorder(new CustomLineBorder(Colors.getTableHeaderGridColor(), 0, 0, 1, 0));
            tableHeader.setDefaultRenderer(new BasicTableHeaderRenderer());
            tableHeader.addMouseMotionListener(Mouse.listener().onDrag(e -> {
                JBScrollPane scrollPane = getScrollPane();
                if (scrollPane != null) {
                    calculateScrollDistance();
                    if (scrollDistance != 0 && scrollTimer == null) {
                        scrollTimer = new Timer();
                        scrollTimer.schedule(new ScrollTask(), 100, 100);
                    }
                }
            }));

            tableHeader.addMouseListener(Mouse.listener().onRelease(e -> {
                if (scrollTimer != null) {
                    SafeDisposer.dispose(scrollTimer);
                    scrollTimer = null;
                }
            }));
        }

        setSelectionBackground(Colors.getTableSelectionBackground(true));
        setSelectionForeground(Colors.getTableSelectionForeground(true));

        SafeDisposer.register(parent, this);
        SafeDisposer.register(this, tableModel);
    }

    public JBScrollPane getScrollPane() {
        return scrollPane.get();
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        Container parent = getParent();
        if (parent instanceof JBScrollPane) {
            JBScrollPane scrollPane = (JBScrollPane) parent;
            scrollPane.getViewport().setBackground(bg);
        }
    }

    @Override
    public String getToolTipText(@NotNull MouseEvent e) {
        return null;
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        dataModel = replace(super.getModel(), dataModel, false);
        super.setModel(dataModel);
    }

    protected void initTableSorter() {
        setRowSorter(new DBNTableSorter(getModel()));
        JTableHeader tableHeader = getTableHeader();
        if (tableHeader != null) {
            tableHeader.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    protected void adjustRowHeight(int padding) {
        rowVerticalPadding = padding;
        adjustRowHeight();
    }

    protected void adjustRowHeight() {
        Font font = getFont();
        FontRenderContext fontRenderContext = getFontMetrics(font).getFontRenderContext();
        LineMetrics lineMetrics = font.getLineMetrics("ABCÄÜÖÂÇĞIİÖŞĀČḎĒËĠḤŌŠṢṬŪŽy", fontRenderContext);
        int fontHeight = Math.round(lineMetrics.getHeight());
        setRowHeight(fontHeight + (rowVerticalPadding * 2));
    }

    @Override
    @NotNull
    public T getModel() {
        return Failsafe.nn((T) super.getModel());
    }

    private void calculateScrollDistance() {
        JBScrollPane scrollPane = getScrollPane();
        if (scrollPane != null) {
            JViewport viewport = scrollPane.getViewport();
            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            if (pointerInfo != null) {
                double mouseLocation = pointerInfo.getLocation().getX();
                double viewportLocation = viewport.getLocationOnScreen().getX();

                Point viewPosition = viewport.getViewPosition();
                double contentLocation = viewport.getView().getLocationOnScreen().getX();

                if (contentLocation < viewportLocation && mouseLocation < viewportLocation + 20) {
                    scrollDistance = - Math.min(viewPosition.x, (viewportLocation - mouseLocation));
                } else {
                    int viewportWidth = viewport.getWidth();
                    int contentWidth = viewport.getView().getWidth();

                    if (contentLocation + contentWidth > viewportLocation + viewportWidth && mouseLocation > viewportLocation + viewportWidth - 20) {
                        scrollDistance = (mouseLocation - viewportLocation - viewportWidth);
                    } else {
                        scrollDistance = 0;
                    }
                }
            }
        }
    }

    @NotNull
    public final Project getProject() {
        return parentComponent.ensure().ensureProject();
    }

    @NotNull
    public DBNComponent getParentComponent() {
        return parentComponent.ensure();
    }

    protected Object getValueAtMouseLocation() {
        Point location = MouseInfo.getPointerInfo().getLocation();
        location.setLocation(location.getX() - getLocationOnScreen().getX(), location.getY() - getLocationOnScreen().getY());
        return getValueAtLocation(location);
    }

    private Object getValueAtLocation(Point point) {
        int columnIndex = columnAtPoint(point);
        int rowIndex = rowAtPoint(point);
        return columnIndex > -1 && rowIndex > -1 ? getModel().getValueAt(rowIndex, columnIndex) : null;
    }

    /*********************************************************
     *                    Cell metrics                       *
     *********************************************************/
    public void accommodateColumnsSize() {
        int buffer = getColumnWidthBuffer();
        for (int c = 0; c < getColumnCount(); c++){
            accommodateColumnSize(c, buffer);
        }
    }

    public int getColumnWidthBuffer() {
        return 22;
    }

    @Override
    public int convertColumnIndexToView(int modelColumnIndex) {
        return super.convertColumnIndexToView(modelColumnIndex);
    }

    @Override
    public int convertColumnIndexToModel(int viewColumnIndex) {
        // table is not scrolling correctly when columns are moved
/*
        if (getTableHeader().getDraggedColumn() != null && CommonUtil.isCalledThrough(BasicTableHeaderUI.MouseInputHandler.class)) {
            return getTableHeader().getDraggedColumn().getModelIndex();
        }
*/
        return super.convertColumnIndexToModel(viewColumnIndex);
    }

    public void accommodateColumnSize(int columnIndex, int span) {
        TableColumnModel columnModel = getColumnModel();
        if (columnIndex < columnModel.getColumnCount()) {

            T model = getModel();
            TableColumn column = columnModel.getColumn(columnIndex);

            int minWidth = getMinColumnWidth();
            int maxWidth = getMaxColumnWidth();
            int preferredWidth = 0;

            // header
            JTableHeader tableHeader = getTableHeader();
            if (tableHeader != null) {
                Object headerValue = column.getHeaderValue();
                TableCellRenderer renderer = column.getHeaderRenderer();
                if (renderer == null) renderer = tableHeader.getDefaultRenderer();
                Component headerComponent = renderer.getTableCellRendererComponent(this, headerValue, false, false, 0, columnIndex);
                int width = (int) headerComponent.getPreferredSize().getWidth();
                if (width > preferredWidth)
                    preferredWidth = width;
            }

            // rows
            String columnName = model.getColumnName(columnIndex);
            int rowCount = model.getRowCount();
            for (int r = 0; r < rowCount; r++) {
                if (preferredWidth >= maxWidth) break;

                int c = column.getModelIndex();
                Object value = model.getValueAt(r, c);
                if (value != null) {
                    String displayValue = model.getPresentableValue(value, c);
                    if (displayValue != null && displayValue.length() < 100) {
                        int cellWidth = metricsCache.getTextWidth(columnName, displayValue);
                        preferredWidth = Math.max(preferredWidth, cellWidth);
                    }
                }
            }

            preferredWidth = Math.min(preferredWidth, maxWidth);
            preferredWidth = Math.max(preferredWidth, minWidth);
            preferredWidth = preferredWidth + span;

            if (column.getPreferredWidth() != preferredWidth)  {
                column.setPreferredWidth(preferredWidth);
            }
        }
    }

    protected int getMinColumnWidth() {
        return MIN_COLUMN_WIDTH;
    }

    protected int getMaxColumnWidth() {
        return MAX_COLUMN_WIDTH;
    }

    public void selectCell(int rowIndex, int columnIndex) {
        if (rowIndex > -1 && columnIndex > -1 && rowIndex < getRowCount() && columnIndex < getColumnCount()) {
            Rectangle cellRect = getCellRect(rowIndex, columnIndex, true);
            if (!getVisibleRect().contains(cellRect)) {
                scrollRectToVisible(cellRect);
            }
            if (getSelectedRowCount() != 1 || getSelectedRow() != rowIndex) {
                setRowSelectionInterval(rowIndex, rowIndex);
            }

            if (getSelectedColumnCount() != 1 || getSelectedColumn() != columnIndex) {
                setColumnSelectionInterval(columnIndex, columnIndex);
            }
        }

    }

    public String getPresentableValueAt(int selectedRow, int selectedColumn) {
        Object value = getValueAt(selectedRow, selectedColumn);
        String presentableValue;
        try {
            presentableValue = getModel().getPresentableValue(value, selectedColumn);
        } catch (UnsupportedOperationException e) {
            presentableValue = value == null ? null : value.toString();
        }
        return presentableValue;
    }

    private class ScrollTask extends TimerTask {
        @Override
        public void run() {
            JBScrollPane scrollPane = getScrollPane();
            if (scrollPane != null && scrollDistance != 0) {
                Dispatch.run(() -> {
                    JViewport viewport = scrollPane.getViewport();
                    Point viewPosition = viewport.getViewPosition();
                    viewport.setViewPosition(new Point((int) (viewPosition.x + scrollDistance), viewPosition.y));
                    calculateScrollDistance();
                });
            }
        }
    }

    protected DBNTableGutter<?> createTableGutter() {
        return null; // do not instructions gutter by default
    }

    public final DBNTableGutter<?> getTableGutter() {
        return tableGutter.get();
    }

    public final void initTableGutter() {
        DBNTableGutter tableGutter = getTableGutter();
        if (tableGutter == null) return;

        JScrollPane scrollPane = UIUtil.getParentOfType(JScrollPane.class, this);
        if (scrollPane == null) return;

        scrollPane.setRowHeaderView(tableGutter);
    }

    protected void resetTableGutter() {
        tableGutter.reset();
        initTableGutter();
    }

    public void stopCellEditing() {
        if (isEditing()) {
            TableCellEditor cellEditor = getCellEditor();
            if (cellEditor != null) {
                cellEditor.stopCellEditing();
            }
        }
    }

    public Point getCellLocation(int row, int column) {
        Rectangle rectangle = getCellRect(row, column, true);
        Point location = getLocationOnScreen();
        return new Point(
                (int) (location.getX() + rectangle.getX()),
                (int) (location.getY() + rectangle.getY()));
    }

    public TableColumn getColumnByName(String columnName) {
        TableColumnModel columnModel = getColumnModel();
        int columnCount = columnModel.getColumnCount();
        for (int i=0; i < columnCount; i++) {
            TableColumn column = columnModel.getColumn(i);
            Object modelColumnIdentifier = column.getIdentifier();
            String modelColumnName = modelColumnIdentifier == null ? null : modelColumnIdentifier.toString();
            if (Strings.equalsIgnoreCase(columnName, modelColumnName)) {
                return column;
            }
        }
        return null;
    }

    @Override
    public void createDefaultColumnsFromModel() {
        TableModel tableModel = getModel();
        // Remove any current columns
        TableColumnModel columnModel = getColumnModel();
        Map<String, TableColumn> oldColumns = new HashMap<>();

        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            Object headerValue = column.getHeaderValue();
            if (headerValue instanceof String) {
                oldColumns.put(headerValue.toString(), column);
            }
        }
        boolean columnSelectionAllowed = columnModel.getColumnSelectionAllowed();

        columnModel = new DefaultTableColumnModel();
        columnModel.setColumnSelectionAllowed(columnSelectionAllowed);

        // Create new columns from the data model info
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            String columnName = tableModel.getColumnName(i);
            TableColumn oldColumn = oldColumns.get(columnName);

            TableColumn newColumn = new TableColumn(i);
            newColumn.setHeaderValue(columnName);
            if (oldColumn != null) {
                newColumn.setPreferredWidth(oldColumn.getPreferredWidth());
            }
            columnModel.addColumn(newColumn);
        }
        setColumnModel(columnModel);
    }
    /********************************************************
     *                    Disposable                        *
     ********************************************************/

    @Nullable
    @Override
    public <V> V getUserData(@NotNull Key<V> key) {
        return userData.get(key);
    }

    @Override
    public <V> void putUserData(@NotNull Key<V> key, @Nullable V value) {
        userData = value == null ?
                userData.minus(key) :
                userData.plus(key, value);
    }

    /********************************************************
     *                    Disposable                        *
     ********************************************************/
    @Getter
    private boolean disposed;

    public void dispose(){
        if (!disposed) {
            disposed = true;
            SafeDisposer.dispose(super.getModel(), false);
            listenerList = new EventListenerList();
            columnModel = new DefaultTableColumnModel();
            selectionModel = new DefaultListSelectionModel();
            nullify();
        }
    }
}
