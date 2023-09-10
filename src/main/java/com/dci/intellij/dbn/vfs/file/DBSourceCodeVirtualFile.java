package com.dci.intellij.dbn.vfs.file;

import com.dci.intellij.dbn.common.thread.Background;
import com.dci.intellij.dbn.common.thread.ThreadMonitor;
import com.dci.intellij.dbn.common.thread.Write;
import com.dci.intellij.dbn.common.util.ChangeTimestamp;
import com.dci.intellij.dbn.common.util.Documents;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.session.DatabaseSession;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.code.SourceCodeManager;
import com.dci.intellij.dbn.editor.code.content.GuardedBlockMarkers;
import com.dci.intellij.dbn.editor.code.content.GuardedBlockType;
import com.dci.intellij.dbn.editor.code.content.SourceCodeContent;
import com.dci.intellij.dbn.editor.code.content.SourceCodeOffsets;
import com.dci.intellij.dbn.language.common.DBLanguageDialect;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.psi.PsiUtil;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.common.status.DBObjectStatus;
import com.dci.intellij.dbn.vfs.DBParseableVirtualFile;
import com.dci.intellij.dbn.vfs.DatabaseFileViewProvider;
import com.intellij.lang.Language;
import com.intellij.notebook.editor.BackedVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.sql.Timestamp;

import static com.dci.intellij.dbn.common.util.GuardedBlocks.createGuardedBlocks;
import static com.dci.intellij.dbn.common.util.GuardedBlocks.removeGuardedBlocks;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dci.intellij.dbn.vfs.file.status.DBFileStatus.*;

@Slf4j
@Getter
public class DBSourceCodeVirtualFile extends DBContentVirtualFile implements DBParseableVirtualFile, DocumentListener, BackedVirtualFile {

    private SourceCodeContent originalContent = new SourceCodeContent();
    private SourceCodeContent localContent = new SourceCodeContent();
    private SourceCodeContent databaseContent = null;

    private ChangeTimestamp databaseTimestamp = new ChangeTimestamp();

    private String sourceLoadError;

    public DBSourceCodeVirtualFile(final DBEditableObjectVirtualFile databaseFile, DBContentType contentType) {
        super(databaseFile, contentType);
        setCharset(databaseFile.getConnection().getSettings().getDetailSettings().getCharset());
    }

    @NotNull
    public SourceCodeOffsets getOffsets() {
        return localContent.getOffsets();
    }

    @Override
    public PsiFile initializePsiFile(DatabaseFileViewProvider fileViewProvider, Language language) {
        ConnectionHandler connection = this.getConnection();
        String parseRootId = getParseRootId();
        if (parseRootId != null) {
            DBLanguageDialect languageDialect = connection.resolveLanguageDialect(language);
            if (languageDialect != null) {
                fileViewProvider.getVirtualFile().putUserData(PARSE_ROOT_ID_KEY, getParseRootId());
                DBLanguagePsiFile file = fileViewProvider.initializePsiFile(languageDialect);
                file.setUnderlyingObject(getObject());
                return file;
            }
        }
        return null;
    }

    @Override
    public DatabaseSession getSession() {
        return this.getConnection().getSessionBundle().getPoolSession();
    }

    public boolean isLoaded() {
        return localContent.isLoaded();
    }

    public String getParseRootId() {
        return getObject().getCodeParseRootId(contentType);
    }

    @Nullable
    public DBLanguagePsiFile getPsiFile() {
        Project project = getProject();
        return PsiUtil.getPsiFile(project, this);
    }

    public void refreshContentState() {
        if (isNot(REFRESHING) && isLoaded()) {
            try {
                set(REFRESHING, true);
                DBSchemaObject object = getObject();

                if (is(LATEST) || is(MERGED)) {
                    boolean checkSources = true;
                    Project project = object.getProject();
                    SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);

                    ChangeTimestamp latestTimestamp = new ChangeTimestamp();
                    if (isChangeMonitoringSupported()) {
                        latestTimestamp = sourceCodeManager.loadChangeTimestamp(object, contentType);
                        checkSources = databaseTimestamp.isOlderThan(latestTimestamp);
                        databaseTimestamp = latestTimestamp;
                    }

                    databaseTimestamp = latestTimestamp;

                    if (checkSources) {
                        SourceCodeContent latestContent = sourceCodeManager.loadSourceFromDatabase(object, contentType);

                        if (is(LATEST) && !latestContent.matches(originalContent, true)) {
                            set(OUTDATED, true);
                            databaseContent = latestContent;
                        }

                        if (is(MERGED) && !latestContent.matches(databaseContent, true)) {
                            set(OUTDATED, true);
                            databaseContent = latestContent;
                        }
                    }
                }

            } catch (SQLException e) {
                conditionallyLog(e);
                log.warn("Error refreshing source content state", e);
            } finally {
                set(REFRESHING, false);
            }
        }
    }

    public boolean isChangedInDatabase(boolean reload) {
        if (isNot(REFRESHING) && isLoaded()) {
            if (reload || databaseTimestamp.isDirty()) {
                if (ThreadMonitor.isTimeSensitiveThread()) {
                    Background.run(getProject(), () -> refreshContentState());
                } else {
                    refreshContentState();
                }

            }
            return isNot(REFRESHING) && (is(OUTDATED) || is(MERGED));
        }
        return false;
    }

    public boolean isMergeRequired() {
        return is(MODIFIED) && is(OUTDATED);
    }

    public void markAsMerged() {
        set(MERGED, true);
    }

    @NotNull
    public Timestamp getDatabaseChangeTimestamp() {
        return databaseTimestamp.value();
    }

    @NotNull
    public CharSequence getOriginalContent() {
        return originalContent.getText();
    }

    @NotNull
    public CharSequence getContent() {
        return localContent.getText();
    }

    public void loadSourceFromDatabase() throws SQLException {
        DBSchemaObject object = getObject();
        Project project = object.getProject();
        SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
        SourceCodeContent newContent = sourceCodeManager.loadSourceFromDatabase(object, contentType);
        databaseTimestamp = sourceCodeManager.loadChangeTimestamp(object, contentType);

        updateFileContent(newContent, null);
        originalContent.setText(newContent.getText());
        object.getStatus().set(contentType, DBObjectStatus.PRESENT, newContent.length() > 0);

        databaseContent = null;
        sourceLoadError = null;
        set(LATEST, true);
        setModified(false);
    }

    public void saveSourceToDatabase() throws SQLException {
        DBSchemaObject object = getObject();
        Project project = object.getProject();

        String oldContent = getOriginalContent().toString();
        String newContent = getContent().toString();
        object.executeUpdateDDL(contentType, oldContent, newContent);

        SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
        databaseTimestamp = sourceCodeManager.loadChangeTimestamp(object, contentType);
        originalContent.setText(newContent);

        databaseContent = null;
        sourceLoadError = null;
        set(LATEST, true);
        setModified(false);
    }

    public void revertLocalChanges() {
        updateFileContent(null, originalContent.getText());
        databaseContent = null;
        sourceLoadError = null;
        set(LATEST, true);
        setModified(false);
    }

    private void updateFileContent(@Nullable SourceCodeContent newContent, @Nullable CharSequence newText) {
        if (newContent != null) {
            localContent = newContent;
        } else {
            localContent.setText(newText);
        }
        Document document = Documents.getDocument(DBSourceCodeVirtualFile.this);
        if (document == null) return;

        Documents.setText(document, localContent.getText());

        SourceCodeOffsets offsets = localContent.getOffsets();
        GuardedBlockMarkers guardedBlocks = offsets.getGuardedBlocks();
        if (guardedBlocks.isEmpty()) return;

        Write.run(getProject(), () -> {
            removeGuardedBlocks(document, GuardedBlockType.READONLY_DOCUMENT_SECTION);
            createGuardedBlocks(document, GuardedBlockType.READONLY_DOCUMENT_SECTION, guardedBlocks, null);
        });
    }

    private boolean isChangeMonitoringSupported() {
        return DatabaseFeature.OBJECT_CHANGE_MONITORING.isSupported(getObject());
    }

    @Override
    @NotNull
    public byte[] contentsToByteArray() {
        return localContent.getText().toString().getBytes(getCharset());
    }

    @Override
    public long getLength() {
        return localContent.length();
    }

    public void setSourceLoadError(String sourceLoadError) {
        this.sourceLoadError = sourceLoadError;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, T value) {
        if (key == FileDocumentManagerImpl.HARD_REF_TO_DOCUMENT_KEY && contentType.isOneOf(DBContentType.CODE, DBContentType.CODE_BODY) ) {
            getMainDatabaseFile().putUserData(FileDocumentManagerImpl.HARD_REF_TO_DOCUMENT_KEY, (Document) value);
        }
        super.putUserData(key, value);
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {

    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        CharSequence oldContent = originalContent.getText();
        CharSequence newContent = event.getDocument().getCharsSequence();

        setModified(!Strings.equals(oldContent, newContent));
        localContent.setText(newContent);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        originalContent = new SourceCodeContent();
        localContent = new SourceCodeContent();
    }

    @NotNull
    @Override
    public VirtualFile getOriginFile() {
        // DBN-536 TODO check why below was needed
        return this;//getMainDatabaseFile();
    }
}
