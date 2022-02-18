package com.dci.intellij.dbn.common.ui.tab;

import com.dci.intellij.dbn.common.dispose.SafeDisposer;
import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.ui.DBNForm;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public class TabbedPane extends JBEditorTabs implements StatefulDisposable {
    private boolean disposed;

    public TabbedPane(@NotNull DBNForm form) {
        super(form.getProject(), ActionManager.getInstance(), IdeFocusManager.findInstance(), form);
        Disposer.register(form, this);
    }

    public void select(JComponent component, boolean requestFocus) {
        TabInfo tabInfo = findInfo(component);
        if (tabInfo != null) {
            select(tabInfo, requestFocus);
        }
    }

    @NotNull
    @Override
    public TabInfo addTab(TabInfo info, int index) {
        checkDisposed();
        return super.addTab(info, index);
    }

    @Override
    public TabInfo addTabSilently(TabInfo info, int index) {
        checkDisposed();
        return super.addTabSilently(info, index);
    }

    @NotNull
    @Override
    public TabInfo addTab(TabInfo info) {
        checkDisposed();
        return super.addTab(info);
    }

    @NotNull
    @Override
    public ActionCallback removeTab(TabInfo tabInfo) {
        return removeTab(tabInfo, true);
    }

    public ActionCallback removeTab(TabInfo tabInfo, boolean disposeComponent) {
        Object object = tabInfo.getObject();
        ActionCallback actionCallback = super.removeTab(tabInfo);
        if (disposeComponent) {
            SafeDisposer.dispose(object, true);
            tabInfo.setObject(null);
        }
        return actionCallback;
    }

    @Override
    public void dispose() {
        if (!disposed && !super.isDisposed()) {
            disposed = true;
            for (TabInfo tabInfo : myInfo2Label.keySet()) {
                Object object = tabInfo.getObject();
                SafeDisposer.dispose(object, true);
            }
            nullify();
        }
    }
}
