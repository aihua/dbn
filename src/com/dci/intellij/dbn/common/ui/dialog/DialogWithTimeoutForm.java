package com.dci.intellij.dbn.common.ui.dialog;

import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.Borders;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.util.TimeUtil;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class DialogWithTimeoutForm extends DBNFormImpl {
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JLabel timeLeftLabel;

    public DialogWithTimeoutForm(DBNDialog<?> parent, int secondsLeft) {
        super(parent);
        contentPanel.setBorder(Borders.BOTTOM_LINE_BORDER);
        updateTimeLeft(secondsLeft);
    }

    public void setContentComponent(JComponent contentComponent) {
        contentPanel.add(contentComponent, BorderLayout.CENTER);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public void updateTimeLeft(int secondsLeft) {
        Dispatch.run(() -> {
            int minutes = 0;
            int seconds = secondsLeft;
            if (secondsLeft > 60) {
                minutes = TimeUtil.getMinutes(secondsLeft);
                seconds = secondsLeft - TimeUtil.getSeconds(minutes);
            }

            if (minutes == 0) {
                timeLeftLabel.setText(seconds + " seconds");
                timeLeftLabel.setForeground(JBColor.RED);
            } else {
                timeLeftLabel.setText(minutes +":" + (seconds < 10 ? "0" :"") + seconds + " minutes");
            }
        });
    }
}
