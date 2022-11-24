package com.dci.intellij.dbn.common.content.dependency;

import com.dci.intellij.dbn.common.content.DynamicContent;
import org.jetbrains.annotations.NotNull;

public class DualContentDependencyAdapter extends BasicDependencyAdapter implements ContentDependencyAdapter {
    private ContentDependency first;
    private ContentDependency second;

    private DualContentDependencyAdapter(DynamicContent firstContent, DynamicContent secondContent) {
        first = dependency(firstContent);

        if (firstContent == secondContent) {
            // dual dependencies may rely on same content
            second = first;
        } else {
            second = dependency(secondContent);
        }
    }

    public static DualContentDependencyAdapter create(DynamicContent firstContent, DynamicContent secondContent) {
        return new DualContentDependencyAdapter(firstContent, secondContent);
    }

    @Override
    public boolean canLoad() {
        if (!content(first).isLoaded()) return false;
        if (!content(second).isLoaded()) return false;

        return true;
    }

    @Override
    public boolean canLoadInBackground() {
        if (content(first).isLoadingInBackground()) return false;
        if (content(second).isLoadingInBackground()) return false;

        return true;
    }

    @Override
    public boolean isDependencyDirty() {
        if (content(first).isDirty()) return true;
        if (content(second).isDirty()) return true;

        return false;
    }

    @Override
    public void refreshSources() {
        content(first).refresh();
        content(second).refresh();
        super.refreshSources();
    }

    @Override
    public boolean canLoadFast() {
        return
            content(first).isLoaded() &&
            content(second).isLoaded() /*&&
            !ThreadMonitor.isDispatchThread()*/;
    }

    @Override
    public void beforeLoad(boolean force) {
        if (!force) return;

        content(first).refresh();
        content(second).refresh();
    }

    @Override
    public void afterLoad() {
        first.updateSignature();
        second.updateSignature();
    }

    private static DynamicContent content(ContentDependency dependency) {
        return dependency.getSourceContent();
    }

    @NotNull
    private static ContentDependency dependency(DynamicContent content) {
        if (content == null) return VoidContentDependency.INSTANCE;
        return new BasicContentDependency(content);
    }

    @Override
    public void dispose() {
        first = VoidContentDependency.INSTANCE;
        second = VoidContentDependency.INSTANCE;
        super.dispose();
    }
}
