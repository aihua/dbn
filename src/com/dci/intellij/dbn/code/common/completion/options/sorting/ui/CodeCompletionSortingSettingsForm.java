package com.dci.intellij.dbn.code.common.completion.options.sorting.ui;

import com.dci.intellij.dbn.code.common.completion.options.sorting.CodeCompletionSortingItem;
import com.dci.intellij.dbn.code.common.completion.options.sorting.CodeCompletionSortingSettings;
import com.dci.intellij.dbn.code.common.completion.options.sorting.action.MoveDownAction;
import com.dci.intellij.dbn.code.common.completion.options.sorting.action.MoveUpAction;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.common.util.ActionUtil;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

import static com.dci.intellij.dbn.common.ui.GUIUtil.updateBorderTitleForeground;

public class CodeCompletionSortingSettingsForm extends ConfigurationEditorForm<CodeCompletionSortingSettings> {
    private JPanel mainPanel;
    private JList sortingItemsList;
    private JCheckBox enableCheckBox;
    private JPanel actionPanel;

    public CodeCompletionSortingSettingsForm(CodeCompletionSortingSettings settings) {
        super(settings);
        resetFormChanges();
        sortingItemsList.setCellRenderer(LIST_CELL_RENDERER);
        sortingItemsList.setFont(UIUtil.getLabelFont());
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("", true,
                new MoveUpAction(sortingItemsList, settings),
                new MoveDownAction(sortingItemsList, settings));
        actionPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
        registerComponent(mainPanel);
        updateBorderTitleForeground(mainPanel);
    }


    @Override
    protected ActionListener createActionListener() {
         return e -> {
             getConfiguration().setModified(true);
             sortingItemsList.setEnabled(enableCheckBox.isSelected());
             sortingItemsList.setBackground(
                     enableCheckBox.isSelected() ?
                             UIUtil.getTextFieldBackground() :
                             UIUtil.getComboBoxDisabledBackground());
             sortingItemsList.clearSelection();
         };
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        List<CodeCompletionSortingItem> sortingItems = getConfiguration().getSortingItems();
        sortingItems.clear();
        ListModel model = sortingItemsList.getModel();
        for (int i=0; i<model.getSize(); i++) {
            sortingItems.add((CodeCompletionSortingItem) model.getElementAt(i));
        }
        getConfiguration().setEnabled(enableCheckBox.isSelected());
    }

    @Override
    public void resetFormChanges() {
        DefaultListModel model = new DefaultListModel();
        for (CodeCompletionSortingItem sortingItem : getConfiguration().getSortingItems()) {
            model.addElement(sortingItem);
        }
        sortingItemsList.setModel(model);
        enableCheckBox.setSelected(getConfiguration().isEnabled());
        sortingItemsList.setEnabled(getConfiguration().isEnabled());
        sortingItemsList.setBackground(
                enableCheckBox.isSelected() ?
                        UIUtil.getTextFieldBackground() :
                        UIUtil.getComboBoxDisabledBackground());
    }

    public static ListCellRenderer LIST_CELL_RENDERER = new ColoredListCellRenderer() {
        @Override
        protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
            CodeCompletionSortingItem sortingItem = (CodeCompletionSortingItem) value;
            DBObjectType objectType = sortingItem.getObjectType();
            if (objectType == null) {
                append(sortingItem.getTokenTypeName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            } else {
                append(objectType.getName().toUpperCase(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                setIcon(objectType.getIcon());
            }

        }
    };
}
