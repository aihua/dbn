package com.dci.intellij.dbn.common.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.property.PropertyHolder;
import com.dci.intellij.dbn.common.util.ActionUtil;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ValueSelector<T extends Presentable> extends JPanel{
    private final Set<ValueSelectorListener<T>> listeners = new HashSet<>();
    private final PropertyHolder<ValueSelectorOption> options = PropertyHolder.create(ValueSelectorOption.class);

    private final JLabel label;
    private final JPanel innerPanel;
    private boolean isEnabled = true;
    private ListPopup popup;

    private static final Border focusBorder = new CompoundBorder(new RoundedLineBorder(new JBColor(Gray._190, Gray._55), 3, 1), JBUI.Borders.empty(2, 4));
    private static final Border defaultBorder = JBUI.Borders.empty(3, 5);

    private List<T> values;
    private PresentableFactory<T> valueFactory;


    public ValueSelector(@Nullable String text, @Nullable T preselectedValue, ValueSelectorOption... options) {
        this(null, text, null, preselectedValue, options);
    }

    public ValueSelector(@Nullable Icon icon, @Nullable String text, @Nullable T preselectedValue, ValueSelectorOption... options) {
        this(icon, text, null, preselectedValue, options);
    }

    public ValueSelector(@Nullable Icon icon, @Nullable String text, @Nullable List<T> values, @Nullable T preselectedValue, ValueSelectorOption... options) {
        super(new BorderLayout());
        setOptions(options);
        this.values = values;

        label = new JLabel(CommonUtil.nvl(text, ""), cropIcon(icon), SwingConstants.LEFT);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(mouseListener);

        setBorder(defaultBorder);

        innerPanel = new JPanel(new BorderLayout());
        innerPanel.add(label, BorderLayout.WEST);
        innerPanel.addMouseListener(mouseListener);
        innerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(innerPanel, BorderLayout.CENTER);

        setMinimumSize(new Dimension(0, 30));
    }

    @Override
    public void setBorder(Border border) {
        super.setBorder(border);
    }

    public void setOptions(ValueSelectorOption ... options) {
        for (ValueSelectorOption option : options) {
            this.options.set(option, true);
        }

    }

    public void setValueFactory(PresentableFactory<T> valueFactory) {
        this.valueFactory = valueFactory;
    }

    public void addListener(ValueSelectorListener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(ValueSelectorListener<T> listener) {
        listeners.remove(listener);
    }

    private static Icon cropIcon(Icon icon) {
        return icon == null ? null : IconUtil.cropIcon(icon, 16, 16);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        label.setCursor(isEnabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR): Cursor.getDefaultCursor());
        innerPanel.setCursor(isEnabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());

        innerPanel.setBackground(UIUtil.getPanelBackground());
        innerPanel.setFocusable(isEnabled);
        label.setForeground(isEnabled ? UIUtil.getTextFieldForeground() : UIUtil.getLabelDisabledForeground());
    }

   @Override
   public void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
    private MouseListener mouseListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            if (popup == null) {
                innerPanel.setBorder(focusBorder);
                innerPanel.setBackground(new JBColor(Gray._210, Gray._75));

                GUIUtil.repaint(ValueSelector.this);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (popup == null) {
                innerPanel.setBorder(defaultBorder);
                innerPanel.setBackground(UIUtil.getPanelBackground());

                GUIUtil.repaint(ValueSelector.this);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (getValues().size() == 0) {
                selectValue(null);
            } else {
                if (isEnabled && popup == null) {
                    innerPanel.requestFocus();
                    showPopup();
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            super.mouseMoved(e);
        }
    };

    private void showPopup() {
        innerPanel.setCursor(Cursor.getDefaultCursor());
        label.setCursor(Cursor.getDefaultCursor());
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        for (T value : getValues()) {
            actionGroup.add(new SelectValueAction(value));
        }
        if (valueFactory != null) {
            actionGroup.add(ActionUtil.SEPARATOR);
            actionGroup.add(new AddValueAction());
        }
        popup = JBPopupFactory.getInstance().createActionGroupPopup(
                null,
                actionGroup,
                DataManager.getInstance().getDataContext(this),
                false,
                false,
                false,
                () -> {
                    popup = null;

                    innerPanel.setBorder(defaultBorder);
                    innerPanel.setBackground(UIUtil.getPanelBackground());
                    innerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    innerPanel.requestFocus();
                    GUIUtil.repaint(ValueSelector.this);
                },
                10,
                preselect -> {
/*
                    if (anAction instanceof ValueSelector.SelectValueAction) {
                        SelectValueAction action = (SelectValueAction) anAction;
                        return action.value.equals(selectedValue);
                    }
*/
                    return false;
                });
        GUIUtil.showUnderneathOf(popup, this, 3, 200);
    }

    public void clearValues() {
        selectValue(null);
        values.clear();
    }

    public String getOptionDisplayName(T value) {
        return getName(value);
    }

    public class SelectValueAction extends DumbAwareAction {
        private T value;

        SelectValueAction(T value) {
            super(getOptionDisplayName(value), null, options.is(ValueSelectorOption.HIDE_ICON) ? null : value.getIcon());
            this.value = value;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            selectValue(value);
            innerPanel.requestFocus();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setVisible(isVisible(value));
            e.getPresentation().setText(getOptionDisplayName(value), false);
        }
    }

    private class AddValueAction extends DumbAwareAction {
        AddValueAction() {
            super(valueFactory.getActionName(), null, Icons.ACTION_ADD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            valueFactory.create(inputValue -> {
                if (inputValue != null) {
                    addValue(inputValue);
                    selectValue(inputValue);
                }
            });
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setVisible(valueFactory != null);
        }
    }

    @NotNull
    private String getName(T value) {
        if (value != null) {
            String description = value.getDescription();
            String name = value.getName();
            return options.is(ValueSelectorOption.HIDE_DESCRIPTION) || StringUtil.isEmpty(description) ? name : name + " (" + description + ")";
        } else {
            return "";
        }
    }


    public boolean isVisible(T value) {
        return true;
    }

    public void setSelectedValue(@Nullable T value) {
        selectValue(value);
    }

    public final List<T> getValues() {
        if (values == null) {
            values = loadValues();
        }
        return values;
    }

    protected List<T> loadValues() {
        return new ArrayList<>();
    }

    public void setValues(List<T> values) {
        this.values = values;
    }

    private void addValue(T value) {
        this.values.add(value);
    }

    public void addValues(Collection<T> value) {
        this.values.addAll(value);
    }


    public void resetValues() {
        this.values = null;
    }

    private void selectValue(T value) {
        for (ValueSelectorListener<T> listener : listeners) {
            listener.selectionChanged(null, value);
        }
    }
}
