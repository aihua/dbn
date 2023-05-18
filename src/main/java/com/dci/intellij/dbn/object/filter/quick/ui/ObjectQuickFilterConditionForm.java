package com.dci.intellij.dbn.object.filter.quick.ui;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.listener.ComboBoxSelectionKeyListener;
import com.dci.intellij.dbn.common.ui.misc.DBNComboBox;
import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.object.filter.ConditionOperator;
import com.dci.intellij.dbn.object.filter.quick.ObjectQuickFilter;
import com.dci.intellij.dbn.object.filter.quick.ObjectQuickFilterCondition;
import com.dci.intellij.dbn.object.filter.quick.ObjectQuickFilterManager;
import com.dci.intellij.dbn.object.filter.quick.action.DeleteQuickFilterConditionAction;
import com.dci.intellij.dbn.object.filter.quick.action.EnableDisableQuickFilterConditionAction;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.dci.intellij.dbn.common.ui.util.TextFields.onTextChange;

public class ObjectQuickFilterConditionForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JLabel objectNameLabel;
    private JTextField patternTextField;
    private DBNComboBox<ConditionOperator> operatorComboBox;

    private final ObjectQuickFilterCondition condition;

    ObjectQuickFilterConditionForm(@NotNull ObjectQuickFilterForm parent, @NotNull final ObjectQuickFilterCondition condition) {
        super(parent);
        this.condition = condition;
        ObjectQuickFilter<?> filter = condition.getFilter();

        DBObjectType objectType = filter.getObjectType();
        objectNameLabel.setIcon(objectType.getIcon());
        objectNameLabel.setText(objectType.getName().toUpperCase() + " NAME");

        operatorComboBox.setValues(ConditionOperator.values());
        patternTextField.setText(condition.getPattern());
        operatorComboBox.setSelectedValue(condition.getOperator());
        operatorComboBox.addListener((oldValue, newValue) -> {
            Project project = ensureProject();
            ObjectQuickFilterManager quickFilterManager = ObjectQuickFilterManager.getInstance(project);
            quickFilterManager.setLastUsedOperator(newValue);
            condition.setOperator(newValue);
        });

        patternTextField.setToolTipText("<html>press <b>Up/Down</b> keys to change the operator</html>");
        patternTextField.addKeyListener(ComboBoxSelectionKeyListener.create(operatorComboBox, false));
        onTextChange(patternTextField, e -> condition.setPattern(patternTextField.getText().trim()));

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel,
                "DBNavigator.DataEditor.SimpleFilter.Condition", true,
                new EnableDisableQuickFilterConditionAction(this),
                new DeleteQuickFilterConditionAction(this));
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
    }

    @NotNull
    public ObjectQuickFilterForm getParentForm() {
        return ensureParentComponent();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return patternTextField;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @NotNull
    protected ObjectQuickFilterCondition getCondition() {
        return Failsafe.nn(condition);
    }

    public void remove() {
        getParentForm().removeConditionPanel(condition);
    }

    public boolean isActive() {
        return getCondition().isActive();
    }

    public void setActive(boolean active) {
        getCondition().setActive(active);
    }
}
