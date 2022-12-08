package com.dci.intellij.dbn.editor.session.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.ToggleAction;
import com.dci.intellij.dbn.common.color.Colors;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.exception.OutdatedContentException;
import com.dci.intellij.dbn.common.ref.WeakRef;
import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.common.ui.component.DBNComponent;
import com.dci.intellij.dbn.common.ui.form.DBNFormBase;
import com.dci.intellij.dbn.common.ui.util.Borders;
import com.dci.intellij.dbn.common.util.Actions;
import com.dci.intellij.dbn.common.util.Documents;
import com.dci.intellij.dbn.common.util.Editors;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.editor.session.SessionBrowser;
import com.dci.intellij.dbn.editor.session.SessionBrowserManager;
import com.dci.intellij.dbn.editor.session.model.SessionBrowserModelRow;
import com.dci.intellij.dbn.editor.session.ui.table.SessionBrowserTable;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.PsiFileRef;
import com.dci.intellij.dbn.language.sql.SQLLanguage;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.vfs.DatabaseFileViewProvider;
import com.dci.intellij.dbn.vfs.file.DBSessionStatementVirtualFile;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

public class SessionBrowserCurrentSqlPanel extends DBNFormBase {
    private JPanel actionsPanel;
    private JPanel viewerPanel;
    private JPanel mainPanel;

    private final WeakRef<SessionBrowser> sessionBrowser;
    private PsiFileRef<DBLanguagePsiFile> psiFile;
    private DBSessionStatementVirtualFile virtualFile;
    private Document document;
    private EditorEx viewer;
    private Object selectedSessionId;

    private final AtomicReference<Thread> refreshHandle = new AtomicReference<>();


    SessionBrowserCurrentSqlPanel(DBNComponent parent, SessionBrowser sessionBrowser) {
        super(parent);
        this.sessionBrowser = WeakRef.of(sessionBrowser);
        createStatementViewer();

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, "", true, new RefreshAction(), new WrapUnwrapContentAction());
        actionsPanel.add(actionToolbar.getComponent(),BorderLayout.WEST);

    }

    @NotNull
    public SessionBrowser getSessionBrowser() {
        return sessionBrowser.ensure();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    private void setPreviewText(String text) {
        Documents.setText(document, text);
    }

    private void setSchemaId(SchemaId schemaId) {
        getVirtualFile().setSchemaId(schemaId);
    }

    @NotNull
    public DBSessionStatementVirtualFile getVirtualFile() {
        return Failsafe.nn(virtualFile);
    }

    void loadCurrentStatement() {
        SessionBrowser sessionBrowser = getSessionBrowser();
        SessionBrowserTable editorTable = sessionBrowser.getBrowserTable();
        if (editorTable.getSelectedRowCount() == 1) {
            SessionBrowserModelRow selectedRow = editorTable.getModel().getRowAtIndex(editorTable.getSelectedRow());
            if (selectedRow != null) {
                setPreviewText("-- Loading...");
                selectedSessionId = selectedRow.getSessionId();

                Object sessionId = selectedSessionId;
                String schemaName = selectedRow.getSchema();
                Project project = sessionBrowser.getProject();

                Background.run(project, refreshHandle, () -> {
                    ConnectionHandler connection = getConnection();
                    DBSchema schema = null;
                    if (Strings.isNotEmpty(schemaName)) {
                        schema = connection.getObjectBundle().getSchema(schemaName);
                    }

                    checkCancelled(sessionId);
                    SessionBrowserManager sessionBrowserManager = SessionBrowserManager.getInstance(project);
                    String sql = sessionBrowserManager.loadSessionCurrentSql(connection, sessionId);
                    sql = sql.trim().replaceAll("\r\n", "\n").replaceAll("\r", "\n");

                    checkCancelled(sessionId);
                    setSchemaId(SchemaId.from(schema));
                    setPreviewText(sql);
                });
            } else {
                setPreviewText("");
            }
        } else {
            setPreviewText("");
        }
    }

    private void checkCancelled(Object sessionId) {
        if (selectedSessionId == null || !selectedSessionId.equals(sessionId)) {
            throw new OutdatedContentException(this);
        }
    }

    @NotNull
    private ConnectionHandler getConnection() {
        return getSessionBrowser().getConnection();
    }

    public DBLanguagePsiFile getPsiFile() {
        return psiFile.get();
    }

    private void createStatementViewer() {
        SessionBrowser sessionBrowser = getSessionBrowser();
        Project project = sessionBrowser.getProject();
        ConnectionHandler connection = getConnection();
        virtualFile = new DBSessionStatementVirtualFile(sessionBrowser, "");
        DatabaseFileViewProvider viewProvider = new DatabaseFileViewProvider(project, virtualFile, true);
        DBLanguagePsiFile psiFile = (DBLanguagePsiFile) virtualFile.initializePsiFile(viewProvider, SQLLanguage.INSTANCE);
        this.psiFile = PsiFileRef.of(psiFile);
        document = Documents.getDocument(psiFile);


        viewer = (EditorEx) EditorFactory.getInstance().createViewer(document, project);
        viewer.setEmbeddedIntoDialogWrapper(true);
        Editors.setEditorReadonly(this.viewer, true);
        Editors.initEditorHighlighter(viewer, SQLLanguage.INSTANCE, connection);
        //statementViewer.setBackgroundColor(colorsScheme.getColor(ColorKey.find("CARET_ROW_COLOR")));

        JScrollPane viewerScrollPane = viewer.getScrollPane();
        viewerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        viewerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        //viewerScrollPane.setBorder(null);
        viewerScrollPane.setViewportBorder(Borders.lineBorder(Colors.getReadonlyEditorBackground(), 4));

        EditorSettings settings = viewer.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);
        settings.setUseSoftWraps(true);
        viewer.getComponent().setFocusable(false);

        viewerPanel.add(viewer.getComponent(), BorderLayout.CENTER);
    }


    public class WrapUnwrapContentAction extends ToggleAction {
        WrapUnwrapContentAction() {
            super("Wrap/Unwrap", "", Icons.ACTION_WRAP_TEXT);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return viewer != null && viewer.getSettings().isUseSoftWraps();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            viewer.getSettings().setUseSoftWraps(state);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            boolean isWrapped = viewer != null && viewer.getSettings().isUseSoftWraps();
            e.getPresentation().setText(isWrapped ? "Unwrap Content" : "Wrap Content");

        }
    }

    public class RefreshAction extends AnAction {
        RefreshAction() {
            super("Reload", "", Icons.ACTION_REFRESH);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            loadCurrentStatement();
        }
    }

    public EditorEx getViewer() {
        return viewer;
    }


    @Override
    protected void disposeInner() {
        Editors.releaseEditor(viewer);
        virtualFile = null;
        super.disposeInner();
    }

}
