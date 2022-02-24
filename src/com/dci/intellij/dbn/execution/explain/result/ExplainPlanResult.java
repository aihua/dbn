package com.dci.intellij.dbn.execution.explain.result;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.action.DataKeys;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionHandlerRef;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.ResultSets;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.execution.ExecutionResultBase;
import com.dci.intellij.dbn.execution.explain.result.ui.ExplainPlanResultForm;
import com.dci.intellij.dbn.language.common.DBLanguageDialect;
import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.psi.ExecutablePsiElement;
import com.dci.intellij.dbn.language.sql.SQLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dci.intellij.dbn.common.dispose.SafeDisposer.replace;

@Getter
@Setter
public class ExplainPlanResult extends ExecutionResultBase<ExplainPlanResultForm> {
    private String planId;
    private Date timestamp;
    private ExplainPlanEntry root;
    private final ConnectionHandlerRef connection;
    private final VirtualFile virtualFile;
    private final SchemaId currentSchema;
    private final String errorMessage;
    private final String statementText;
    private final String resultName;

    public ExplainPlanResult(ExecutablePsiElement executablePsiElement, ResultSet resultSet) throws SQLException {
        this(executablePsiElement, (String) null);
        // entries must be sorted by PARENT_ID NULLS FIRST, ID
        Map<Integer, ExplainPlanEntry> entries = new HashMap<>();
        ConnectionHandler connection = getConnection();
        List<String> explainColumnNames = ResultSets.getColumnNames(resultSet);

        while (resultSet.next()) {
            ExplainPlanEntry entry = new ExplainPlanEntry(connection, resultSet, explainColumnNames);
            Integer id = entry.getId();
            Integer parentId = entry.getParentId();
            entries.put(id, entry);
            if (parentId == null) {
                root = entry;
            } else {
                ExplainPlanEntry parentEntry = entries.get(parentId);
                parentEntry.addChild(entry);
                entry.setParent(parentEntry);
            }
        }
    }

    public ExplainPlanResult(ExecutablePsiElement executablePsiElement, String errorMessage) {
        DBLanguagePsiFile psiFile = executablePsiElement.getFile();
        ConnectionHandler connection = Failsafe.nn(psiFile.getConnection());
        this.connection = connection.ref();
        this.currentSchema = psiFile.getSchemaId();
        this.virtualFile = psiFile.getVirtualFile();
        this.resultName = Commons.nvl(executablePsiElement.createSubjectList(), "Explain Plan");
        this.errorMessage = errorMessage;
        this.statementText = executablePsiElement.getText();
    }

    @Override
    public ConnectionId getConnectionId() {
        return connection.getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return ConnectionHandlerRef.ensure(connection);
    }

    @Override
    public PsiFile createPreviewFile() {
        ConnectionHandler connection = getConnection();
        SchemaId currentSchema = getCurrentSchema();
        DBLanguageDialect languageDialect = connection.getLanguageDialect(SQLLanguage.INSTANCE);
        return DBLanguagePsiFile.createFromText(
                getProject(),
                "preview",
                languageDialect,
                statementText,
                connection,
                currentSchema);
    }

    @NotNull
    @Override
    public Project getProject() {
        return getConnection().getProject();
    }

    @Nullable
    @Override
    public ExplainPlanResultForm createForm() {
        return new ExplainPlanResultForm(this);
    }

    @Override
    @NotNull
    public String getName() {
        return resultName;
    }

    @Override
    public Icon getIcon() {
        return Icons.EXPLAIN_PLAN_RESULT;
    }

    public boolean isError() {
        return errorMessage != null;
    }

    /********************************************************
     *                    Data Provider                     *
     ********************************************************/
    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.EXPLAIN_PLAN_RESULT.is(dataId)) {
            return ExplainPlanResult.this;
        }
        return null;
    }

    /********************************************************
     *                    Disposable                   *
     *******************************************************  */
    @Override
    public void disposeInner() {
        root = replace(root, null, false);
        super.disposeInner();
    }
}
