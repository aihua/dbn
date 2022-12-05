package com.dci.intellij.dbn.editor.session;

import com.dci.intellij.dbn.common.action.DataKeys;
import com.dci.intellij.dbn.common.dispose.Checks;
import com.dci.intellij.dbn.common.dispose.DisposableUserDataHolderBase;
import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.event.ProjectEvents;
import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.dci.intellij.dbn.common.ui.util.UserInterface;
import com.dci.intellij.dbn.connection.ConnectionAction;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.context.DatabaseContextBase;
import com.dci.intellij.dbn.connection.operation.options.OperationSettings;
import com.dci.intellij.dbn.editor.session.model.SessionBrowserModel;
import com.dci.intellij.dbn.editor.session.model.SessionBrowserModelRow;
import com.dci.intellij.dbn.editor.session.options.SessionBrowserSettings;
import com.dci.intellij.dbn.editor.session.ui.SessionBrowserDetailsForm;
import com.dci.intellij.dbn.editor.session.ui.SessionBrowserForm;
import com.dci.intellij.dbn.editor.session.ui.table.SessionBrowserTable;
import com.dci.intellij.dbn.language.common.WeakRef;
import com.dci.intellij.dbn.vfs.file.DBSessionBrowserVirtualFile;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.Timer;
import java.util.*;

public class SessionBrowser extends DisposableUserDataHolderBase implements FileEditor, DatabaseContextBase, DataProvider {
    private final WeakRef<DBSessionBrowserVirtualFile> databaseFile;

    private SessionBrowserForm browserForm;
    private boolean preventLoading = false;
    private boolean loading;
    private Timer refreshTimer;
    private FileEditorState cachedState;

    public SessionBrowser(DBSessionBrowserVirtualFile databaseFile) {
        this.databaseFile = WeakRef.of(databaseFile);
        this.browserForm = new SessionBrowserForm(this);

        Disposer.register(this, browserForm);

        loadSessions(true);
    }

    @NotNull
    public SessionBrowserTable getBrowserTable() {
        return getBrowserForm().getBrowserTable();
    }

    @NotNull
    public SessionBrowserForm getBrowserForm() {
        return Failsafe.nn(browserForm);
    }

    public void showSearchHeader() {
        getBrowserForm().showSearchHeader();
    }

    @Nullable
    public SessionBrowserModel getTableModel() {
        return getBrowserTable().getModel();
    }

    public SessionBrowserSettings getSettings() {
        return OperationSettings.getInstance(getProject()).getSessionBrowserSettings();
    }

    public boolean isPreventLoading(boolean force) {
        if (force) return false;
        SessionBrowserTable editorTable = getBrowserTable();
        return preventLoading || editorTable.getSelectedRowCount() > 1;
    }

    public void loadSessions(boolean force) {
        if (!canLoad(force)) return;

        Project project = getProject();
        ConnectionAction.invoke("loading the sessions", false, this,
                action -> Background.run(project, () -> {
                    if (!canLoad(force)) return;

                    DBSessionBrowserVirtualFile databaseFile = getDatabaseFile();
                    try {
                        setLoading(true);
                        SessionBrowserManager sessionBrowserManager = SessionBrowserManager.getInstance(project);
                        SessionBrowserModel model = sessionBrowserManager.loadSessions(databaseFile);
                        replaceModel(model);
                    } finally {
                        ProjectEvents.notify(project,
                                SessionBrowserLoadListener.TOPIC,
                                (listener) -> listener.sessionsLoaded(databaseFile));
                        setLoading(false);
                    }
                }),
                action -> {
                    setLoading(false);
                    setRefreshInterval(0);
                },
                null);
    }

    private boolean canLoad(boolean force) {
        return !loading && !isPreventLoading(force);
    }

    private void replaceModel(SessionBrowserModel newModel) {
        if (newModel != null) {
            Dispatch.run(() -> {
                SessionBrowserTable editorTable = getBrowserTable();
                SessionBrowserModel oldModel = editorTable.getModel();
                SessionBrowserState state = oldModel.getState();
                newModel.setState(state);
                editorTable.setModel(newModel);
                refreshTable();
                Disposer.dispose(oldModel);
            });
        }
    }

    public void clearFilter() {
        SessionBrowserTable editorTable = getBrowserTable();
        SessionBrowserFilter filter = editorTable.getModel().getFilter();
        if (filter != null) {
            filter.clear();
            refreshTable();
        }
    }

    public void refreshTable() {
        SessionBrowserTable editorTable = getBrowserTable();
        UserInterface.repaint(editorTable);
        editorTable.accommodateColumnsSize();
        //editorTable.restoreSelection();
    }

    void refreshLoadTimestamp() {
        if (Checks.isValid(browserForm)) {
            browserForm.refreshLoadTimestamp();
        }
    }

    public void disconnectSelectedSessions() {
        interruptSessions(SessionInterruptionType.DISCONNECT);
    }

    public void killSelectedSessions() {
        interruptSessions(SessionInterruptionType.TERMINATE);
    }

    public void interruptSession(SessionIdentifier identifier, SessionInterruptionType type) {
        SessionBrowserManager sessionBrowserManager = SessionBrowserManager.getInstance(getProject());
        sessionBrowserManager.interruptSessions(this, Collections.singletonList(identifier), type);
        loadSessions(true);
    }

    private void interruptSessions(SessionInterruptionType type) {
        List<SessionIdentifier> sessionIds = new ArrayList<>();

        SessionBrowserTable editorTable = getBrowserTable();
        SessionBrowserModel model = editorTable.getModel();
        for (int selectedRow : editorTable.getSelectedRows()) {
            SessionBrowserModelRow row = model.getRowAtIndex(selectedRow);
            if (row != null) {
                sessionIds.add(row.getSessionIdentifier());
            }
        }

        SessionBrowserManager sessionBrowserManager = SessionBrowserManager.getInstance(getProject());
        sessionBrowserManager.interruptSessions(this, sessionIds, type);
        loadSessions(true);
    }


    @NotNull
    public DBSessionBrowserVirtualFile getDatabaseFile() {
        return databaseFile.ensure();
    }

    @NotNull
    public Project getProject() {
        return getDatabaseFile().getProject();
    }

    @Override
    @NotNull
    public JComponent getComponent() {
        return isDisposed() ? new JPanel() : browserForm.getComponent();
    }

    @Override
    @Nullable
    public JComponent getPreferredFocusedComponent() {
        return isDisposed() ? null : getBrowserTable();
    }

    @Override
    @NonNls
    @NotNull
    public String getName() {
        return "Data";
    }

    @Override
    @NotNull
    public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        if (!isDisposed()) {
            SessionBrowserTable editorTable = getBrowserTable();
            SessionBrowserModel model = editorTable.getModel();
            cachedState = model.getState().clone();
        }
        return cachedState;
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
        if (fileEditorState instanceof SessionBrowserState) {
            SessionBrowserTable editorTable = getBrowserTable();
            SessionBrowserModel model = editorTable.getModel();
            SessionBrowserState sessionBrowserState = (SessionBrowserState) fileEditorState;
            model.setState(sessionBrowserState);
            refreshTable();
            startRefreshTimer((sessionBrowserState).getRefreshInterval());
        }
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {

    }

    @Override
    public void deselectNotify() {

    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    @Nullable
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Override
    @Nullable
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    @Nullable
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }

    public void setPreventLoading(boolean preventLoading) {
        this.preventLoading = preventLoading;
    }

    public boolean isLoading() {
        return loading;
    }

    protected void setLoading(boolean loading) {
        if (this.loading != loading) {
            this.loading = loading;

            Dispatch.run(() -> {
                if (browserForm != null) {
                    if (SessionBrowser.this.loading)
                        browserForm.showLoadingHint(); else
                        browserForm.hideLoadingHint();
                }

                SessionBrowserTable editorTable = getBrowserTable();
                editorTable.setLoading(SessionBrowser.this.loading);
                UserInterface.repaint(editorTable);
            });
        }

    }

    public int getRowCount() {
        return getBrowserTable().getRowCount();
    }

    public void setRefreshInterval(int refreshInterval) {
        SessionBrowserModel tableModel = getTableModel();
        if (tableModel != null) {
            SessionBrowserState state = tableModel.getState();
            if (state.getRefreshInterval() != refreshInterval) {
                state.setRefreshInterval(refreshInterval);
                stopRefreshTimer();
                startRefreshTimer(refreshInterval);
            }
        }
    }

    private void startRefreshTimer(int refreshInterval) {
        if (refreshTimer == null && refreshInterval > 0) {
            refreshTimer = new Timer("DBN - Session Browser (refresh timer)");
            int period = refreshInterval * 1000;
            refreshTimer.schedule(new RefreshTask(), period, period);
        }
    }

    private void stopRefreshTimer() {
        Disposer.dispose(refreshTimer);
        refreshTimer = null;
    }

    String getModelError() {
        SessionBrowserModel tableModel = getTableModel();
        return tableModel == null ? null : tableModel.getLoadError();
    }

    public int getRefreshInterval() {
        SessionBrowserModel tableModel = getTableModel();
        return tableModel == null ? 0 : tableModel.getState().getRefreshInterval();
    }

    @Nullable
    public Object getSelectedSessionId() {
        SessionBrowserTable editorTable = getBrowserTable();
        if (editorTable.getSelectedRowCount() == 1) {
            int rowIndex = editorTable.getSelectedRow();
            SessionBrowserModelRow rowAtIndex = editorTable.getModel().getRowAtIndex(rowIndex);
            return rowAtIndex == null ? null : rowAtIndex.getSessionId();
        }
        return null;
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return getDatabaseFile();
    }

    private class RefreshTask extends TimerTask {
        @Override
        public void run() {
            loadSessions(false);
        }
    }

    public void updateDetails() {
        if (browserForm != null) {
            SessionBrowserTable editorTable = browserForm.getBrowserTable();
            SessionBrowserDetailsForm detailsForm = browserForm.getDetailsForm();
            if (editorTable.getSelectedRowCount() == 1) {
                SessionBrowserModelRow selectedRow = editorTable.getModel().getRowAtIndex(editorTable.getSelectedRow());
                detailsForm.update(selectedRow);
            } else {
                detailsForm.update(null);
            }
        }
    }


    @NotNull
    public ConnectionId getConnectionId() {
        return getDatabaseFile().getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return getDatabaseFile().getConnection();
    }

    /*******************************************************
     *                   Data Provider                     *
     *******************************************************/

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.SESSION_BROWSER.is(dataId)) {
            return SessionBrowser.this;
        }
        return null;
    }


    @Override
    public void dispose() {
        stopRefreshTimer();
        browserForm = null;
        super.dispose();
    }
}

