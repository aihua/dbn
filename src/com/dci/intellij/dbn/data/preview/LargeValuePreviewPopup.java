package com.dci.intellij.dbn.data.preview;

import com.dci.intellij.dbn.common.Colors;
import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.Lookup;
import com.dci.intellij.dbn.common.dispose.SafeDisposer;
import com.dci.intellij.dbn.common.ui.Borders;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.ui.listener.PopupCloseListener;
import com.dci.intellij.dbn.common.ui.table.TableUtil;
import com.dci.intellij.dbn.common.util.ActionUtil;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.data.editor.ui.UserValueHolder;
import com.dci.intellij.dbn.data.grid.ui.table.basic.BasicTable;
import com.dci.intellij.dbn.data.value.LargeObjectValue;
import com.dci.intellij.dbn.editor.data.DatasetEditorManager;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.SQLException;

public class LargeValuePreviewPopup extends DBNFormImpl {
    public static final int INITIAL_MAX_SIZE = 4000;
    private JPanel mainPanel;
    private JTextArea valueTextArea;
    private JScrollPane valueScrollPane;
    private JPanel topActionsPanel;
    private JLabel infoLabel;
    private JPanel infoPanel;
    private JPanel leftActionsPanel;

    private transient JBPopup popup;
    private final WeakRef<JTable> table;
    private final UserValueHolder<?> userValueHolder;

    private boolean loadContentVisible;
    private String loadContentCaption;
    private String contentInfoText;

    private final boolean largeTextLayout;
    private boolean pinned;

    public LargeValuePreviewPopup(Project project, JTable table, UserValueHolder<?> userValueHolder, int preferredWidth) {
        super(null, project);
        this.table = WeakRef.of(table);
        this.userValueHolder = userValueHolder;

        loadContent(true);
        String value = valueTextArea.getText();
        int maxRowLength = StringUtil.textMaxRowLength(value);
        preferredWidth = Math.max(preferredWidth, Math.min(maxRowLength * 8, 600));
        largeTextLayout = preferredWidth > 500;

        valueScrollPane.setBorder(null);
        valueScrollPane.setPreferredSize(new Dimension(preferredWidth + 32, Math.max(60, preferredWidth / 4)));

        if (largeTextLayout) {
            boolean isBasicPreview = !(table instanceof BasicTable);
            if (isBasicPreview) {
                ActionToolbar actionToolbar = ActionUtil.createActionToolbar("", false, new WrapUnwrapContentAction());
                JComponent toolbarComponent = actionToolbar.getComponent();
                leftActionsPanel.add(toolbarComponent, BorderLayout.NORTH);
                topActionsPanel.setVisible(false);
            } else {
                ActionToolbar actionToolbar = ActionUtil.createActionToolbar("", true,
                    /*new PinUnpinPopupAction(),
                    new CloseAction(),
                    ActionUtil.SEPARATOR,*/
                        new WrapUnwrapContentAction(),
                        new LoadReloadAction());
                JComponent toolbarComponent = actionToolbar.getComponent();
                topActionsPanel.add(toolbarComponent, BorderLayout.WEST);
                leftActionsPanel.setVisible(false);
            }

            if (!isBasicPreview) {
                infoPanel.setVisible(true);
                infoPanel.setBorder(Borders.BOTTOM_LINE_BORDER);
            } else {
                infoLabel.setVisible(false);
            }

            DatasetEditorManager dataEditorManager = DatasetEditorManager.getInstance(userValueHolder.getProject());
            pinned = dataEditorManager.isValuePreviewPinned();
            boolean isWrapped = dataEditorManager.isValuePreviewTextWrapping();
            valueTextArea.setLineWrap(isWrapped);
        } else {
            valueTextArea.setLineWrap(true);
            infoPanel.setVisible(false);
            leftActionsPanel.setVisible(false);
        }

        valueTextArea.setBackground(Colors.LIGHT_BLUE);
        valueTextArea.addKeyListener(keyListener);
    }

    @Nullable
    public JTable getTable() {
        return WeakRef.get(table);
    }

    public void addPopupListener(JBPopupListener listener) {
    }

    private void loadContent(boolean initial) {
        String text = "";
        Object userValue = userValueHolder.getUserValue();
        if (userValue instanceof LargeObjectValue) {
            LargeObjectValue largeObjectValue = (LargeObjectValue) userValue;
            try {
                text = initial ?
                        largeObjectValue.read(INITIAL_MAX_SIZE) :
                        largeObjectValue.read();
                text = CommonUtil.nvl(text, "");

                long contentSize = largeObjectValue.size();
                if (initial && contentSize > INITIAL_MAX_SIZE) {
                    contentInfoText = getNumberOfLines(text) + " lines, " + INITIAL_MAX_SIZE + " characters (partially loaded)";
                    loadContentVisible = true;
                    loadContentCaption = "Load entire content";
                } else {
                    contentInfoText = getNumberOfLines(text) + " lines, " + text.length() + " characters";
                    loadContentVisible = false;
                }
            } catch (SQLException e) {
                contentInfoText = "Could not load " + largeObjectValue.getDisplayValue() + " content. Cause: " + e.getMessage();
                loadContentCaption = "Reload content";
            }
        } else {
            text = userValue == null ? "" : userValue.toString();
            contentInfoText = getNumberOfLines(text) + " lines, " + text.length() + " characters";
            loadContentVisible = false;
        }
        int caretPosition = valueTextArea.getText().length();
        valueTextArea.setText(text);
        valueTextArea.setCaretPosition(caretPosition);
        if (popup != null && largeTextLayout) {
            infoLabel.setText(contentInfoText);
            //popup.setAdText(contentInfoText, SwingUtilities.LEFT);
        }
    }

    private static int getNumberOfLines(String text) {
        return StringUtil.countNewLines(text) + 1;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            super.keyTyped(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            JTable table = getTable();
            // only if fired for table cells. Enable table navigation.
            if (table != null) {
                int selectedRow = table.getSelectedRow();
                int selectedColumn = table.getSelectedColumn();

                int row = selectedRow;
                int column = selectedColumn;
                if (e.getKeyCode() == 38) { // UP
                    if (selectedRow > 0) row = selectedRow - 1;
                } else if (e.getKeyCode() == 40) { // DOWN
                    if (selectedRow < table.getRowCount() - 1) row = selectedRow + 1;
                } else if (e.getKeyCode() == 37) { // LEFT
                    if (selectedColumn > 0) column = selectedColumn - 1;
                } else if (e.getKeyCode() == 39) { // RIGHT
                    if (selectedColumn < table.getColumnCount() - 1) column = selectedColumn + 1;
                }

                if (row != selectedRow || column != selectedColumn) {
                    TableUtil.selectCell(table, row, column);
                }
            }
        }
    };

    public JBPopup show(Component component) {
        JBPopup popup = createPopup();
        popup.showInScreenCoordinates(component,
                new Point(
                        (int) (component.getLocationOnScreen().getX() + component.getWidth() + 8),
                        (int) component.getLocationOnScreen().getY()));
        return popup;
    }

    public JBPopup show(Component component, Point point) {
        JBPopup popup = createPopup();
        point.setLocation(
                point.getX() + component.getLocationOnScreen().getX(),
                point.getY() + component.getLocationOnScreen().getY());

        popup.showInScreenCoordinates(component, point);
        return popup;
    }

    private JBPopup createPopup() {
        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(mainPanel, valueTextArea);
        popupBuilder.setMovable(true);
        popupBuilder.setResizable(true);
        popupBuilder.setRequestFocus(true);
        popupBuilder.setDimensionServiceKey(userValueHolder.getProject(), "LargeValuePreview." + userValueHolder.getName(), false);
/*
        popupBuilder.setCancelOnMouseOutCallback(new MouseChecker() {
            @Override
            public boolean check(MouseEvent event) {
                return false;
            }
        });
*/

        popupBuilder.setCancelCallback(() -> !pinned);

        if (largeTextLayout) {
            infoLabel.setText(contentInfoText);
            //popupBuilder.setAdText(contentInfoText);
            //popupBuilder.setTitle("Large value preview");
        }

        popup = popupBuilder.createPopup();
        popup.addListener(PopupCloseListener.create(this));
        return popup;
    }

    @Override
    public void disposeInner() {
        Object userValue = userValueHolder.getUserValue();
        if (userValue instanceof LargeObjectValue) {
            LargeObjectValue largeObjectValue = (LargeObjectValue) userValue;
            largeObjectValue.release();
        }
        SafeDisposer.dispose(popup);
        super.disposeInner();
    }


    /**
     * ******************************************************
     * Actions                         *
     * *******************************************************
     */
    public class WrapUnwrapContentAction extends ToggleAction {

        WrapUnwrapContentAction() {
            super("Wrap/Unwrap", "", Icons.ACTION_WRAP_TEXT);
        }


        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            DatasetEditorManager dataEditorManager = getDataEditorManager(e);
            return dataEditorManager != null && dataEditorManager.isValuePreviewTextWrapping();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            DatasetEditorManager editorManager = getDataEditorManager(e);
            if (editorManager != null) {
                editorManager.setValuePreviewTextWrapping(state);
                valueTextArea.setLineWrap(state);
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            DatasetEditorManager dataEditorManager = getDataEditorManager(e);
            if (dataEditorManager != null) {
                boolean isWrapped = dataEditorManager.isValuePreviewTextWrapping();
                e.getPresentation().setText(isWrapped ? "Unwrap Content" : "Wrap Content");
            }
        }
    }

    public class PinUnpinPopupAction extends ToggleAction {

        public PinUnpinPopupAction() {
            super("Pin/Unpin", "", Icons.ACTION_PIN);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return pinned;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            DatasetEditorManager editorManager = getDataEditorManager(e);
            if (editorManager != null) {
                editorManager.setValuePreviewPinned(state);
                pinned = state;
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setText(pinned ? "Unpin" : "Pin");

        }
    }

    private class LoadReloadAction extends AnAction {
        private LoadReloadAction() {
            super("Load / Reload Content", null, Icons.ACTION_RERUN);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            loadContent(false);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(loadContentVisible);
            presentation.setText(loadContentCaption);
        }
    }

    private class CloseAction extends AnAction {
        private CloseAction() {
            super("Close", null, Icons.ACTION_CLOSE);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            pinned = false;
            popup.cancel();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
        }
    }

    @Nullable
    private static DatasetEditorManager getDataEditorManager(AnActionEvent e) {
        Project project = Lookup.getProject(e);
        return project == null ? null : DatasetEditorManager.getInstance(project);
    }

}
