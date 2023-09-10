package com.dci.intellij.dbn.common.ui.tab;

import com.dci.intellij.dbn.common.dispose.Disposer;
import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.ui.form.DBNForm;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;

public class TabbedPane extends JBEditorTabs implements StatefulDisposable {
    private boolean disposed;
    private final Map<TabInfo, String> tabInfos = ContainerUtil.createConcurrentWeakMap();

    public TabbedPane(@NotNull DBNForm form) {
        super(form.ensureProject(), ActionManager.getInstance(), IdeFocusManager.findInstance(), form);
        setTabDraggingEnabled(true);
        Disposer.register(form, () -> customDispose());
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
        acknowledgeTab(info);
        return super.addTab(info, index);
    }

    @Override
    public TabInfo addTabSilently(TabInfo info, int index) {
        acknowledgeTab(info);
        return super.addTabSilently(info, index);
    }

    @NotNull
    @Override
    public TabInfo addTab(TabInfo info) {
        acknowledgeTab(info);
        return super.addTab(info);
    }

    private void acknowledgeTab(TabInfo info) {
        checkDisposed();
        tabInfos.put(info, info.getText());
    }

    @NotNull
    @Override
    public ActionCallback removeTab(TabInfo tabInfo) {
        return removeTab(tabInfo, true);
    }

    public ActionCallback removeTab(TabInfo info, boolean disposeComponent) {
        tabInfos.remove(info);
        Object object = info.getObject();
        ActionCallback actionCallback = super.removeTab(info);
        if (disposeComponent) {
            Disposer.dispose(object);
            info.setObject(null);
        }
        return actionCallback;
    }

    public void selectTab(String tabName) {
        if (tabName == null) return;

        for (TabInfo tabInfo : getTabs()) {
            if (Objects.equals(tabInfo.getText(), tabName)) {
                select(tabInfo, false);
                return;
            }
        }
    }

    public String getSelectedTabName() {
        TabInfo selectedInfo = getSelectedInfo();
        if (selectedInfo == null) return null;

        return selectedInfo.getText();
    }

    private void customDispose() {
        if (disposed && !super.isDisposed()) return;

        disposed = true;
        for (TabInfo tabInfo : tabInfos.keySet()) {
            Object object = tabInfo.getObject();
            tabInfo.setObject(null);
            Disposer.dispose(object);
        }
        nullify();
    }
}
