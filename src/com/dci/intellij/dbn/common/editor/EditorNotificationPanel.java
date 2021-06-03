package com.dci.intellij.dbn.common.editor;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.message.MessageType;
import com.dci.intellij.dbn.common.util.Context;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.ui.PlatformColors;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.util.UUID;

public class EditorNotificationPanel extends JPanel{
    protected final JLabel label = new JLabel();
    protected final JPanel linksPanel;

    public EditorNotificationPanel(MessageType messageType) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(1, 15, 1, 15));

        setPreferredSize(new Dimension(-1, 24));

        add(label, BorderLayout.CENTER);
        Icon icon = null;
        Color background;

        switch (messageType) {
            case INFO: {
                icon = Icons.COMMON_INFO;
                background = HintUtil.getInformationColor();
                break;
            }
            case WARNING:{
                icon = Icons.COMMON_WARNING;
                background = HintUtil.getInformationColor();
                break;
            }
            case ERROR:{
                //icon = AllIcons.General.Error;
                background = HintUtil.getErrorColor();
                break;
            }
            default:{
                //icon = AllIcons.General.Information;
                background = HintUtil.getInformationColor();
                break;
            }
        }

        label.setIcon(icon);
        setBackground(background);

        linksPanel = new JPanel(new FlowLayout());
        linksPanel.setBackground(background);
        add(linksPanel, BorderLayout.EAST);
    }

    public void setText(@NotNull String text) {
        label.setText(text);
    }

    public void setIcon(@NotNull Icon icon) {
        label.setIcon(icon);
    }

    public HyperlinkLabel createActionLabel(final String text, @NonNls final String actionId) {
        return createActionLabel(text, new Runnable() {
            @Override
            public void run() {
                executeAction(actionId);
            }
        });
    }

    protected HyperlinkLabel createActionLabel(final String text, final Runnable action) {
        HyperlinkLabel label = new HyperlinkLabel(text, PlatformColors.BLUE, getBackground(), PlatformColors.BLUE);
        label.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(HyperlinkEvent e) {
                action.run();
            }
        });
        linksPanel.add(label);
        return label;
    }

    private void executeAction(final String actionId) {
        AnAction action = ActionManager.getInstance().getAction(actionId);
        AnActionEvent event = new AnActionEvent(null, Context.getDataContext(this), UUID.randomUUID().toString(),
                action.getTemplatePresentation(), ActionManager.getInstance(),
                0);
        action.beforeActionPerformedUpdate(event);
        action.update(event);

        if (event.getPresentation().isEnabled() && event.getPresentation().isVisible()) {
            action.actionPerformed(event);
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, 0);
    }
}
