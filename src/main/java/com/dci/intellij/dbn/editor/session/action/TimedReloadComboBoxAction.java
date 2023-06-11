package com.dci.intellij.dbn.editor.session.action;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.DataKeys;
import com.dci.intellij.dbn.common.action.Lookups;
import com.dci.intellij.dbn.common.ui.misc.DBNComboBoxAction;
import com.dci.intellij.dbn.common.util.Context;
import com.dci.intellij.dbn.editor.session.SessionBrowser;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TimedReloadComboBoxAction extends DBNComboBoxAction implements DumbAware {

    public TimedReloadComboBoxAction() {
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent component) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new SelectRefreshTimeAction(0));
        actionGroup.addSeparator();
        actionGroup.add(new SelectRefreshTimeAction(5));
        actionGroup.add(new SelectRefreshTimeAction(10));
        actionGroup.add(new SelectRefreshTimeAction(20));
        actionGroup.add(new SelectRefreshTimeAction(30));
        actionGroup.add(new SelectRefreshTimeAction(60));
        return actionGroup;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Icon icon = Icons.ACTION_TIMED_REFRESH_OFF;
        String text = "No Refresh";


        SessionBrowser sessionBrowser = getSessionBrowser(e);
        if (sessionBrowser != null) {
            int refreshInterval = sessionBrowser.getRefreshInterval();
            if (refreshInterval > 0) {
                text = refreshInterval + " seconds";
                if (sessionBrowser.isPreventLoading(false)) {
                    icon = Icons.ACTION_TIMED_REFRESH_INTERRUPTED;
                } else {
                    icon = Icons.ACTION_TIMED_REFRESH;
                }

            }
        }

        presentation.setText(text);
        presentation.setIcon(icon);
    }

    @Nullable
    public static SessionBrowser getSessionBrowser(JComponent component) {
        DataContext dataContext = Context.getDataContext(component);
        SessionBrowser sessionBrowser = DataKeys.SESSION_BROWSER.getData(dataContext);
        if (sessionBrowser == null) {
            FileEditor fileEditor = Lookups.getFileEditor(dataContext);
            if (fileEditor instanceof SessionBrowser) {
                sessionBrowser = (SessionBrowser) fileEditor;
            }
        }
        return sessionBrowser;
    }

    @Nullable
    public static SessionBrowser getSessionBrowser(AnActionEvent e) {
        SessionBrowser sessionBrowser = e.getData((DataKeys.SESSION_BROWSER));
        if (sessionBrowser == null) {
            FileEditor fileEditor = Lookups.getFileEditor(e);
            if (fileEditor instanceof SessionBrowser) {
                sessionBrowser = (SessionBrowser) fileEditor;
            }
        }
        return sessionBrowser;
    }

    private static class SelectRefreshTimeAction extends AnAction {
        private final int seconds;

        SelectRefreshTimeAction(int seconds) {
            super(seconds == 0 ? "No Refresh" : seconds + " seconds", null, seconds == 0 ? null : Icons.COMMON_TIMER);
            this.seconds = seconds;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SessionBrowser sessionBrowser = getSessionBrowser(e);
            if (sessionBrowser != null) {
                sessionBrowser.setRefreshInterval(seconds);
            }
        }
    }
 }