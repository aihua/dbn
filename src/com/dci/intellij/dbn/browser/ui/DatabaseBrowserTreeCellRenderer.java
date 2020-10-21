package com.dci.intellij.dbn.browser.ui;

import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.browser.model.LoadInProgressTreeNode;
import com.dci.intellij.dbn.browser.options.DatabaseBrowserSettings;
import com.dci.intellij.dbn.common.ui.tree.TreeUtil;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.ConnectionBundle;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.grid.options.DataGridSettings;
import com.dci.intellij.dbn.object.DBColumn;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.DBUser;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.list.DBObjectList;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class DatabaseBrowserTreeCellRenderer implements TreeCellRenderer {
    private DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
    private DatabaseBrowserSettings browserSettings;

    public DatabaseBrowserTreeCellRenderer(@NotNull Project project) {
        browserSettings = DatabaseBrowserSettings.getInstance(project);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        return cellRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }

    private class DefaultTreeCellRenderer extends ColoredTreeCellRenderer {
        @Override
        public Font getFont() {
            Font font = super.getFont();
            return font == null ? UIUtil.getTreeFont() : font;
        }

        @Override
        public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            try{
                if (value instanceof LoadInProgressTreeNode) {
                    LoadInProgressTreeNode loadInProgressTreeNode = (LoadInProgressTreeNode) value;
                    setIcon(loadInProgressTreeNode.getIcon(0));
                    append("Loading...", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
                    return;
                }

                BrowserTreeNode treeNode = (BrowserTreeNode) value;
                setIcon(treeNode.getIcon(0));

                boolean isDirty = false;
                String displayName;
                if (treeNode instanceof ConnectionBundle) {
                    displayName = "PROJECT";
                } else {
                    displayName = treeNode.getPresentableText();
                }

                if (treeNode instanceof DBObjectList) {
                    DBObjectList objectsList = (DBObjectList) treeNode;
                    boolean isEmpty = objectsList.getChildCount() == 0;
                    isDirty = /*objectsList.isDirty() ||*/ objectsList.isLoading() || (!objectsList.isLoaded() && !hasConnectivity(objectsList));
                    SimpleTextAttributes textAttributes =
                            isDirty ? SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES :
                                    isEmpty ? SimpleTextAttributes.REGULAR_ATTRIBUTES :
                                            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

                    append(CommonUtil.nvl(displayName, ""), textAttributes);

                    // todo display load error
                    /*
                                SimpleTextAttributes descriptionTA = objectsList.getErrorMessage() == null ?
                                        SimpleTextAttributes.GRAY_ATTRIBUTES : SimpleTextAttributes.ERROR_ATTRIBUTES;
                                append(" " + displayDetails, descriptionTA);

                                if (objectsList.getErrorMessage() != null) {
                                    String msg = "Could not load " + displayName + ". Cause: " + objectsList.getErrorMessage();
                                    setToolTipText(msg);
                                }  else {
                                    setToolTipText(null);
                                }
                    */
                } else {
                    boolean showBold = false;
                    boolean showGrey = false;
                    boolean isDisposed = false;
                    if (treeNode instanceof DBObject) {
                        DBObject object = (DBObject) treeNode;
                        if (object instanceof DBSchema) {
                            DBSchema schema = (DBSchema) object;
                            showBold = schema.isUserSchema();
                            showGrey = schema.isEmptySchema();
                        } else if (object instanceof DBUser) {
                            DBUser user = (DBUser) object;
                            showBold = user.isSessionUser();
                        }

                        isDisposed = object.isDisposed();
                    }

                    if (!showGrey && treeNode instanceof DBColumn) {
                        DBColumn column = (DBColumn) treeNode;
                        DataGridSettings dataGridSettings = DataGridSettings.getInstance(treeNode.getProject());
                        showGrey = dataGridSettings.getTrackingColumnSettings().isTrackingColumn(column.getName());
                    }

                    SimpleTextAttributes textAttributes =
                            isDisposed ? SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES :
                                    showBold ? (showGrey ? SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES) :
                                            (showGrey ? SimpleTextAttributes.GRAYED_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);

                    if (displayName == null) displayName = "displayName null!!";

                    append(displayName, textAttributes);

                    TreeUtil.applySpeedSearchHighlighting(tree, this, true, selected);
                }
                String displayDetails = treeNode.getPresentableTextDetails();
                if (!StringUtil.isEmptyOrSpaces(displayDetails)) {
                    append(" " + displayDetails, isDirty ? SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES);
                }


                if (browserSettings.getGeneralSettings().getShowObjectDetails().value()) {
                    String conditionalDetails = treeNode.getPresentableTextConditionalDetails();
                    if (!StringUtil.isEmptyOrSpaces(conditionalDetails)) {
                        append(" - " + conditionalDetails, SimpleTextAttributes.GRAY_ATTRIBUTES);
                    }

                }
            } catch (ProcessCanceledException ignore) {}
        }

        private boolean hasConnectivity(@NotNull DBObjectList objectsList) {
            ConnectionHandler connectionHandler = objectsList.getConnectionHandler();
            return objectsList.getConnectionHandler().canConnect() && connectionHandler.isValid();
        }
    }
}
