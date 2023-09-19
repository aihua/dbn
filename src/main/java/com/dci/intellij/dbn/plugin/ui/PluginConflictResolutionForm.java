package com.dci.intellij.dbn.plugin.ui;

import com.dci.intellij.dbn.common.text.TextContent;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.form.DBNHintForm;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.plugin.PluginConflictResolution;
import com.intellij.util.Alarm;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

import static com.dci.intellij.dbn.common.text.TextContent.html;

public class PluginConflictResolutionForm extends DBNFormBase {
    private JPanel mainPanel;
    private JRadioButton disablePluginRadioButton;
    private JRadioButton continueLimitedRadioButton;
    private JRadioButton continueFeaturedRadioButton;
    private JRadioButton decideLaterRadioButton;
    private JLabel selectOptionLabel;
    private JPanel hintPanel;
    private final Alarm selectOptionAlarm;

    @SneakyThrows
    public PluginConflictResolutionForm(@NotNull PluginConflictResolutionDialog dialog) {
        super(dialog);
        String content = Commons.readInputStream(getClass().getResourceAsStream("plugin_conflict_resolution.html"));
        TextContent hintText = html(content);
        DBNHintForm disclaimerForm = new DBNHintForm(this, hintText, null, true);
        disclaimerForm.setHighlighted(true);
        hintPanel.add(disclaimerForm.getComponent());

        selectOptionLabel.setVisible(false);
        selectOptionAlarm = new Alarm(this);

        ActionListener selectionListener = e -> {
            PluginConflictResolution resolution = getChosenResolution();
            if (resolution == null) return;

            selectOptionLabel.setVisible(false);
            switch (resolution) {
                case DISABLE_PLUGIN: dialog.renameAction("Disable DBN and restart"); break;
                case CONTINUE_FEATURED: dialog.renameAction("Continue with full DBN support"); break;
                case CONTINUE_LIMITED: dialog.renameAction("Continue with limited DBN support"); break;
                case DECIDE_LATER: dialog.renameAction("Continue"); break;
                default:
            }
        };
        disablePluginRadioButton.addActionListener(selectionListener);
        continueFeaturedRadioButton.addActionListener(selectionListener);
        continueLimitedRadioButton.addActionListener(selectionListener);
        decideLaterRadioButton.addActionListener(selectionListener);
    }



    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    protected void showErrorMessage() {
        selectOptionLabel.setVisible(true);
        Dispatch.alarmRequest(selectOptionAlarm, (int) TimeUnit.SECONDS.toMillis(3), true, () -> selectOptionLabel.setVisible(false));
    }

    @Nullable
    protected PluginConflictResolution getChosenResolution() {
        if (disablePluginRadioButton.isSelected()) return PluginConflictResolution.DISABLE_PLUGIN;
        if (continueLimitedRadioButton.isSelected()) return PluginConflictResolution.CONTINUE_LIMITED;
        if (continueFeaturedRadioButton.isSelected()) return PluginConflictResolution.CONTINUE_FEATURED;
        if (decideLaterRadioButton.isSelected()) return PluginConflictResolution.DECIDE_LATER;
        return null;
    }
}
