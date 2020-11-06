package com.dci.intellij.dbn.execution.statement.variables;

import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.type.GenericDataType;
import com.dci.intellij.dbn.database.DatabaseMetadataInterface;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.element.util.IdentifierCategory;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.dci.intellij.dbn.language.common.psi.ExecVariablePsiElement;
import com.dci.intellij.dbn.language.common.psi.IdentifierPsiElement;
import com.dci.intellij.dbn.language.common.psi.lookup.ObjectLookupAdapter;
import com.dci.intellij.dbn.object.DBColumn;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.type.DBObjectType;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatementExecutionVariablesBundle extends StatefulDisposable.Base implements StatefulDisposable {
    public static final Comparator<StatementExecutionVariable> NAME_COMPARATOR = (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName());
    public static final Comparator<StatementExecutionVariable> NAME_LENGTH_COMPARATOR = (o1, o2) -> o2.getName().length() - o1.getName().length();
    public static final Comparator<StatementExecutionVariable> OFFSET_COMPARATOR = (o1, o2) -> o1.getOffset() - o2.getOffset();

    private Map<StatementExecutionVariable, String> errorMap;
    private Set<StatementExecutionVariable> variables = new THashSet<>();

    public StatementExecutionVariablesBundle(Set<ExecVariablePsiElement> variablePsiElements) {
        initialize(variablePsiElements);
    }

    public void initialize(Set<ExecVariablePsiElement> variablePsiElements) {
        Set<StatementExecutionVariable> newVariables = new THashSet<StatementExecutionVariable>();
        for (ExecVariablePsiElement variablePsiElement : variablePsiElements) {
            StatementExecutionVariable variable = getVariable(variablePsiElement.getText());
            if (variable == null) {
                variable = new StatementExecutionVariable(variablePsiElement);
            } else {
                variable.setOffset(variablePsiElement.getTextOffset());
            }

            if (variable.getDataType() == null) {
                DBDataType dataType = lookupDataType(variablePsiElement);
                if (dataType != null && dataType.isNative()) {
                    variable.setDataType(dataType.getGenericDataType());
                } else {
                    variable.setDataType(GenericDataType.LITERAL);
                }
            }
            newVariables.add(variable);
        }
        variables = newVariables;
    }

    public boolean isProvided() {
        for (StatementExecutionVariable variable : variables) {
            if (!variable.isProvided()) {
                return false;
            }
        }
        return true;
    }

    public void populate(Map<String, StatementExecutionVariable> variableCache, boolean force) {
        for (StatementExecutionVariable variable : variables) {
            if (!variable.isProvided() || force) {
                StatementExecutionVariable cacheVariable = variableCache.get(variable.getName().toUpperCase());
                if (cacheVariable != null) {
                    variable.populate(cacheVariable);
                }
            }
        }
    }

    public boolean hasErrors() {
        return errorMap != null && errorMap.size() > 0;
    }

    private static DBDataType lookupDataType(ExecVariablePsiElement variablePsiElement) {
        BasePsiElement conditionPsiElement = variablePsiElement.findEnclosingPsiElement(ElementTypeAttribute.CONDITION);

        if (conditionPsiElement != null) {
            ObjectLookupAdapter lookupAdapter = new ObjectLookupAdapter(variablePsiElement, IdentifierCategory.REFERENCE, DBObjectType.COLUMN);
            BasePsiElement basePsiElement = lookupAdapter.findInScope(conditionPsiElement);
            if (basePsiElement instanceof IdentifierPsiElement) {
                IdentifierPsiElement columnPsiElement = (IdentifierPsiElement) basePsiElement;
                DBObject object = columnPsiElement.getUnderlyingObject();
                if (object instanceof DBColumn) {
                    DBColumn column = (DBColumn) object;
                    return column.getDataType();
                }
            }
        }
        return null;
    }

    @Nullable
    public StatementExecutionVariable getVariable(String name) {
        for (StatementExecutionVariable variable : variables) {
            if (StringUtil.equalsIgnoreCase(variable.getName(), name)) {
                return variable;
            }
        }
        return null;
    }

    public Set<StatementExecutionVariable> getVariables() {
        return variables;
    }

    public String prepareStatementText(@NotNull ConnectionHandler connectionHandler, String statementText, boolean forPreview) {
        errorMap = null;
        List<StatementExecutionVariable> variables = new ArrayList<StatementExecutionVariable>(this.variables);
        variables.sort(NAME_LENGTH_COMPARATOR);
        Formatter formatter = Formatter.getInstance(connectionHandler.getProject());
        for (StatementExecutionVariable variable : variables) {
            VariableValueProvider previewValueProvider = variable.getPreviewValueProvider();
            boolean useNullValue = forPreview ? previewValueProvider.useNull() : variable.useNull();

            if (useNullValue) {
                statementText = StringUtil.replaceIgnoreCase(statementText, variable.getName(), "NULL");
            } else {
                String value = forPreview ? previewValueProvider.getValue() : variable.getValue();
                if (!StringUtil.isEmpty(value)) {
                    GenericDataType genericDataType = forPreview ? previewValueProvider.getDataType() : variable.getDataType();
                    if (genericDataType == GenericDataType.LITERAL) {
                        value = StringUtil.replace(value, "'", "''");
                        value = '\'' + value + '\'';
                    } else {
                        if (genericDataType == GenericDataType.DATE_TIME){
                            DatabaseMetadataInterface metadataInterface = connectionHandler.getInterfaceProvider().getMetadataInterface();
                            try {
                                Date date = formatter.parseDateTime(value);
                                value = metadataInterface.createDateString(date);
                            } catch (ParseException e) {
                                try {
                                    Date date = formatter.parseDate(value);
                                    value = metadataInterface.createDateString(date);
                                } catch (ParseException e1) {
                                    addError(variable, "Invalid date");
                                }
                            }
                        } else if (genericDataType == GenericDataType.NUMERIC){
                            try {
                                formatter.parseNumber(value);
                            } catch (ParseException e) {
                                addError(variable, "Invalid number");
                            }

                        } else {
                            throw new IllegalArgumentException("Data type " + genericDataType.getName() + " not supported with execution variables.");
                        }
                    }

                    statementText = StringUtil.replaceIgnoreCase(statementText, variable.getName(), value);
                }
            }
        }
        return statementText;
    }

    private void addError(StatementExecutionVariable variable, String value) {
        if (errorMap == null) {
            errorMap = new THashMap<StatementExecutionVariable, String>();
        }
        errorMap.put(variable, value);
    }

    public String getError(StatementExecutionVariable variable) {
        return errorMap == null ? null : errorMap.get(variable);
    }

    @Override
    protected void disposeInner() {
        nullify();
    }
}
