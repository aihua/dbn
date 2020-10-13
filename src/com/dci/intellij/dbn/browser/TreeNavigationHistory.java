package com.dci.intellij.dbn.browser;

import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.browser.options.DatabaseBrowserSettings;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TreeNavigationHistory implements Disposable{
    private final List<BrowserTreeNode> history = new ArrayList<BrowserTreeNode>();
    private int offset;

    public void add(BrowserTreeNode treeNode) {
        if (history.size() > 0 && treeNode == history.get(offset)) {
            return;
        }
        while (history.size() > offset + 1) {
            history.remove(offset + 1);
        }

        DatabaseBrowserSettings browserSettings = DatabaseBrowserSettings.getInstance(treeNode.getProject());

        int historySize = browserSettings.getGeneralSettings().getNavigationHistorySize().value();
        while (history.size() > historySize) {
            history.remove(0);
        }
        history.add(treeNode);
        offset = history.size() -1;
    }

    public void clear() {
        history.clear();
    }

    public boolean hasNext() {
        return offset < history.size()-1;
    }

    public boolean hasPrevious() {
        return offset > 0;
    }

    public @Nullable BrowserTreeNode next() {
        if (offset < history.size() -1) {
            offset = offset + 1;
            BrowserTreeNode browserTreeNode = history.get(offset);
            if (browserTreeNode.isDisposed()) {
                history.remove(browserTreeNode);
                return next();
            }
            return browserTreeNode;
        }
        return null;
    }

    public @Nullable BrowserTreeNode previous() {
        if (offset > 0) {
            offset = offset-1;
            BrowserTreeNode browserTreeNode = history.get(offset);
            if (browserTreeNode.isDisposed()) {
                history.remove(browserTreeNode);
                return previous();
            }
            return browserTreeNode;
        }
        return null;
    }

    @Override
    public void dispose() {
        history.clear();
    }
}
