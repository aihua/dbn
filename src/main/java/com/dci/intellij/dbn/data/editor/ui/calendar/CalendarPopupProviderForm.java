package com.dci.intellij.dbn.data.editor.ui.calendar;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.DataKeys;
import com.dci.intellij.dbn.common.action.DataProviders;
import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.ui.util.Borders;
import com.dci.intellij.dbn.common.ui.util.Mouse;
import com.dci.intellij.dbn.common.ui.util.UserInterface;
import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.data.editor.ui.TextFieldPopupProviderForm;
import com.dci.intellij.dbn.data.editor.ui.TextFieldPopupType;
import com.dci.intellij.dbn.data.editor.ui.TextFieldWithPopup;
import com.dci.intellij.dbn.data.editor.ui.UserValueHolder;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import static com.dci.intellij.dbn.common.dispose.Failsafe.nd;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;

public class CalendarPopupProviderForm extends TextFieldPopupProviderForm implements TableModelListener {
    private static final TableCellRenderer CELL_RENDERER = new CalendarTableCellRenderer();
    private static final TableCellRenderer HEADER_CELL_RENDERER = new CalendarTableHeaderCellRenderer();
    private final TableModel CALENDER_HEADER_TABLE_MODEL = new CalendarHeaderTableModel();
    private JPanel mainPanel;
    private JTable daysTable;
    private JTable weeksTable;
    private JLabel monthYearLabel;
    private JPanel calendarPanel;
    private JPanel actionsLeftPanel;
    private JPanel actionsRightPanel;
    private JPanel timePanel;
    private JTextField timeTextField;
    private JLabel timeLabel;
    private JPanel actionsPanelBottom;
    private JPanel headerSeparatorPanel;

    public CalendarPopupProviderForm(TextFieldWithPopup<?> textField, boolean autoPopup) {
        super(textField, autoPopup, true);
        calendarPanel.setBackground(weeksTable.getBackground());
        daysTable.addKeyListener(this);
        timeTextField.addKeyListener(this);

        weeksTable.setDefaultRenderer(Object.class, HEADER_CELL_RENDERER);
        weeksTable.setFocusable(false);
        weeksTable.setShowGrid(false);
        calendarPanel.setBorder(Borders.COMPONENT_OUTLINE_BORDER);
        headerSeparatorPanel.setBorder(Borders.BOTTOM_LINE_BORDER);

        daysTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        daysTable.setDefaultRenderer(Object.class, CELL_RENDERER);
        daysTable.getTableHeader().setDefaultRenderer(HEADER_CELL_RENDERER);
        daysTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        daysTable.setShowGrid(false);
        daysTable.addMouseListener(Mouse.listener().onClick(e -> {
            if (e.getButton() == MouseEvent.BUTTON1) {
                selectDate();
            }
        }));

        /*tDays.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                final Point point = e.getPoint();
                int rowIndex = tDays.rowAtPoint(point);
                int columnIndex = tDays.columnAtPoint(point);
                tDays.setRowSelectionInterval(rowIndex, rowIndex);
                tDays.setColumnSelectionInterval(columnIndex, columnIndex);
            }
        });*/

        ActionToolbar actionToolbarLeft = Actions.createActionToolbar(actionsLeftPanel,
                "DBNavigator.ActionGroup.Calendar.LeftControls",
                "DBNavigator.Place.DataEditor.CalendarPopup",
                true);

        ActionToolbar actionToolbarRight = Actions.createActionToolbar(actionsLeftPanel,
                "DBNavigator.ActionGroup.Calendar.RightControls",
                "DBNavigator.Place.DataEditor.CalendarPopup",
                true);

        ActionToolbar actionToolbarBottom = Actions.createActionToolbar(actionsLeftPanel,
                "DBNavigator.ActionGroup.Calendar.BottomControls",
                "DBNavigator.Place.DataEditor.CalendarPopup",
                true);

        Arrays.asList(actionToolbarLeft, actionToolbarRight, actionToolbarBottom).forEach(tb -> tb.getActions().forEach(a -> registerAction(a)));

        actionsLeftPanel.add(actionToolbarLeft.getComponent(), BorderLayout.WEST);
        actionsRightPanel.add(actionToolbarRight.getComponent(), BorderLayout.EAST);
        actionsPanelBottom.add(actionToolbarBottom.getComponent(), BorderLayout.EAST);

        DataProviders.register(actionsLeftPanel, this);
        DataProviders.register(actionsRightPanel, this);
        DataProviders.register(actionsPanelBottom, this);

        updateComponentColors();
        Colors.subscribe(this, () -> updateComponentColors());
    }

    private void updateComponentColors() {
        nd(this);
        Color panelBackground = Colors.getPanelBackground();
        Color labelForeground = Colors.getLabelForeground();
        Color tableBackground = Colors.getTableBackground();

        UserInterface.changePanelBackground(mainPanel, panelBackground);
        timeLabel.setForeground(labelForeground);
        monthYearLabel.setForeground(labelForeground);
        daysTable.setBackground(tableBackground);
        weeksTable.setBackground(tableBackground);
        calendarPanel.setBackground(tableBackground);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public Formatter getFormatter() {
        return Formatter.getInstance(ensureProject());
    }

    private Date getDateForPopup() {
        UserValueHolder<?> userValueHolder = getEditorComponent().getUserValueHolder();
        if (userValueHolder == null) {
            String dateString = getEditorComponent().getTextField().getText();
            try {
                return getFormatter().parseDateTime(dateString);
            } catch (ParseException e) {
                conditionallyLog(e);
                return new Date();
            }
        } else {
            Object userValue = userValueHolder.getUserValue();
            return userValue instanceof Date ? (Date) userValue : new Date();
        }
    }

    @Override
    public JBPopup createPopup() {
        Date date = getDateForPopup();
        CalendarTableModel tableModel = new CalendarTableModel(date);
        tableModel.addTableModelListener(this);

        daysTable.setModel(tableModel);
        weeksTable.setModel(CALENDER_HEADER_TABLE_MODEL);

        int rowIndex = tableModel.getInputDateRowIndex();
        int columnIndex = tableModel.getInputDateColumnIndex();
        daysTable.setRowSelectionInterval(rowIndex, rowIndex);
        daysTable.setColumnSelectionInterval(columnIndex, columnIndex);

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(mainPanel, daysTable);
        popupBuilder.setRequestFocus(true);

        monthYearLabel.setText(tableModel.getCurrentMonthName() + " " + tableModel.getCurrentYear());

        timeTextField.setText(getFormatter().formatTime(date));
        timeLabel.setText("Time (" + getFormatter().getTimeFormatPattern() + ")");

        return popupBuilder.createPopup();
    }

    @Override
    public void handleKeyPressedEvent(KeyEvent e) {}

    @Override
    public void handleKeyReleasedEvent(KeyEvent e) {}

    @Override
    public void handleFocusLostEvent(FocusEvent e) {}


    @Override
    public String getKeyShortcutName() {
        return IdeActions.ACTION_SHOW_INTENTION_ACTIONS;
    }

    @Override
    public String getDescription() {
        return "Calendar";
    }

    @Override
    public TextFieldPopupType getPopupType() {
        return TextFieldPopupType.CALENDAR;
    }

    @Nullable
    @Override
    public Icon getButtonIcon() {
        return Icons.DATA_EDITOR_CALENDAR;
    }

    private void selectDate() {
        CalendarTableModel model = (CalendarTableModel) daysTable.getModel();
        Date date = model.getTimestamp(
                daysTable.getSelectedRow(),
                daysTable.getSelectedColumn(),
                timeTextField.getText(),
                getFormatter());
        TextFieldWithPopup editorComponent = getEditorComponent();
        editorComponent.setText(getFormatter().formatDateTime(date));
        hidePopup();
        getTextField().requestFocus();
    }

    /******************************************************
     *                   KeyListener                      *
     ******************************************************/
    @Override
    public void keyPressed(KeyEvent e) {
        super.keyPressed(e);
        if (!e.isConsumed()) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER ) {
                selectDate();
            }
            if (e.getKeyCode() == KeyEvent.VK_TAB  && e.getSource() == daysTable) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(daysTable);
                e.consume();
            }
        }
    }

    /******************************************************
     *                TableModelListener                  *
     ******************************************************/
    @Override
    public void tableChanged(TableModelEvent e) {
        CalendarTableModel model = getCalendarTableModel();
        monthYearLabel.setText(model.getCurrentMonthName() + " " + model.getCurrentYear());
    }

    CalendarTableModel getCalendarTableModel() {
        return (CalendarTableModel) daysTable.getModel();
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.CALENDAR_POPUP_PROVIDER_FORM.is(dataId)) {
            return this;
        }
        return super.getData(dataId);
    }

    public void setTimeText(String timeString) {
        timeTextField.setText(timeString);
    }


}
