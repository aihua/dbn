package com.dci.intellij.dbn.editor.session.action;

import com.dci.intellij.dbn.common.action.DataKeys;
import com.dci.intellij.dbn.common.action.Lookups;
import com.dci.intellij.dbn.common.ui.misc.DBNComboBoxAction;
import com.dci.intellij.dbn.common.util.Context;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.editor.session.SessionBrowser;
import com.dci.intellij.dbn.editor.session.SessionBrowserFilter;
import com.dci.intellij.dbn.editor.session.SessionBrowserFilterType;
import com.dci.intellij.dbn.editor.session.model.SessionBrowserModel;
import com.dci.intellij.dbn.editor.session.options.SessionBrowserSettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public abstract class AbstractFilterComboBoxAction extends DBNComboBoxAction implements DumbAware {
    private final SessionBrowserFilterType filterType;

    public AbstractFilterComboBoxAction(SessionBrowserFilterType filterType) {
        this.filterType = filterType;
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent component) {
        SessionBrowser sessionBrowser = getSessionBrowser(component);
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new SelectFilterValueAction(null));
        actionGroup.addSeparator();
        if (sessionBrowser != null) {
            SessionBrowserModel model = sessionBrowser.getTableModel();
            if (model != null) {
                SessionBrowserFilter filter = model.getFilter();
                String selectedFilterValue = filter == null ? null : filter.getFilterValue(filterType);
                List<String> filterValues = model.getDistinctValues(filterType, selectedFilterValue);
                for (String filterValue : filterValues) {
                    SelectFilterValueAction action = new SelectFilterValueAction(filterValue);
                    actionGroup.add(action);
                }
            }
        }
        return actionGroup;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        String text = filterType.getName();
        Icon icon = null;//Icons.DATASET_FILTER_EMPTY;

        SessionBrowser sessionBrowser = getSessionBrowser(e);
        if (sessionBrowser != null) {
            SessionBrowserModel model = sessionBrowser.getTableModel();
            if (model != null) {
                SessionBrowserFilter modelFilter = model.getFilter();
                if (modelFilter != null) {
                    String filterValue = modelFilter.getFilterValue(filterType);
                    if (Strings.isNotEmpty(filterValue)) {
                        text = filterValue;
                        icon = filterType.getIcon();
                    }
                }
            }
        }

        presentation.setText(text, false);
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

    private class SelectFilterValueAction extends AnAction {
        private final String filterValue;

        public SelectFilterValueAction(String filterValue) {
            super(filterValue == null ? "No Filter" : filterValue, null, filterValue == null ? null : filterType.getIcon());
            this.filterValue = filterValue;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SessionBrowser sessionBrowser = getSessionBrowser(e);
            if (sessionBrowser != null) {
                SessionBrowserModel model = sessionBrowser.getTableModel();
                if (model !=  null) {
                    SessionBrowserFilter modelFilter = model.getFilter();
                    if (modelFilter != null) {
                        modelFilter.setFilterValue(filterType, filterValue);
                        SessionBrowserSettings sessionBrowserSettings = sessionBrowser.getSettings();
                        if (sessionBrowserSettings.isReloadOnFilterChange()) {
                            sessionBrowser.loadSessions(false);
                        } else {
                            sessionBrowser.refreshTable();
                        }
                    }
                }
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            if (filterValue != null) {
                e.getPresentation().setText(filterValue, false);
            }
        }
    }
 }