package com.dci.intellij.dbn.execution.method;

import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.util.Cloneable;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.dci.intellij.dbn.execution.ExecutionOption;
import com.dci.intellij.dbn.execution.ExecutionOptions;
import com.dci.intellij.dbn.execution.ExecutionTarget;
import com.dci.intellij.dbn.execution.LocalExecutionInput;
import com.dci.intellij.dbn.execution.method.result.MethodExecutionResult;
import com.dci.intellij.dbn.object.DBArgument;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.DBTypeAttribute;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.stringAttribute;

@Getter
@Setter
public class MethodExecutionInput extends LocalExecutionInput implements Comparable<MethodExecutionInput>, Cloneable<MethodExecutionInput> {
    private DBObjectRef<DBMethod> method;

    private transient MethodExecutionResult executionResult;
    private final List<ArgumentValue> argumentValues = new ArrayList<>();
    private Map<String, MethodExecutionArgumentValue> argumentValueHistory = new HashMap<>();

    public MethodExecutionInput(Project project) {
        super(project, ExecutionTarget.METHOD);
        method = new DBObjectRef<>();

        ExecutionOptions options = getOptions();
        options.set(ExecutionOption.COMMIT_AFTER_EXECUTION, true);
        //setSessionId(SessionId.POOL);
    }

    public MethodExecutionInput(Project project, DBObjectRef<DBMethod> method) {
        super(project, ExecutionTarget.METHOD);
        this.method = method;
        SchemaId methodSchema = method.getSchemaId();

        if (methodSchema != null) {
            setTargetSchemaId(methodSchema);
        }


        if (DatabaseFeature.DATABASE_LOGGING.isSupported(method)) {
            ConnectionHandler connection = Failsafe.nn(method.getConnection());
            getOptions().set(ExecutionOption.ENABLE_LOGGING, connection.isLoggingEnabled());
        }
    }

    public MethodExecutionContext initExecution(DBDebuggerType debuggerType) {
        MethodExecutionResult executionResult = new MethodExecutionResult(this, debuggerType);
        executionResult.setPrevious(this.executionResult);
        this.executionResult = executionResult;
        return initExecutionContext();
    }

    @Override
    protected MethodExecutionContext createExecutionContext() {
        return new MethodExecutionContext(this);
    }

    @Nullable
    @Override
    public ConnectionHandler getConnection() {
        DBMethod method = getMethod();
        return method == null ? this.method == null ? null : this.method.getConnection() : method.getConnection();
    }

    @Override
    public ConnectionId getConnectionId() {
        return method.getConnectionId();
    }

    @Override
    public boolean hasExecutionVariables() {
        return false;
    }

    @Override
    public boolean isSchemaSelectionAllowed() {
        return DatabaseFeature.AUTHID_METHOD_EXECUTION.isSupported(getConnection());
    }

    @Override
    public boolean isSessionSelectionAllowed() {
        return true;
    }

    @Override
    public boolean isDatabaseLogProducer() {
        return true;
    }

    @Nullable
    public DBMethod getMethod() {
        return DBObjectRef.get(method);
    }

    public DBObjectRef<DBMethod> getMethodRef() {
        return method;
    }

    public boolean isObsolete() {
        ConnectionHandler connection = method.getConnection();
        return connection == null/* || getMethod() == null*/;
    }

    public boolean isInactive() {
        ConnectionHandler connection = getConnection();
        return connection != null && !connection.getSettings().isActive();
    }

    public void setInputValue(@NotNull DBArgument argument, DBTypeAttribute typeAttribute, String value) {
        ArgumentValue argumentValue = getArgumentValue(argument, typeAttribute);
        argumentValue.setValue(value);
    }

    public void setInputValue(@NotNull DBArgument argument, String value) {
        ArgumentValue argumentValue = getArgumentValue(argument);
        argumentValue.setValue(value);
    }

    public String getInputValue(@NotNull DBArgument argument) {
        ArgumentValue argumentValue = getArgumentValue(argument);
        return (String) argumentValue.getValue();
    }

    public List<String> getInputValueHistory(@NotNull DBArgument argument, @Nullable DBTypeAttribute typeAttribute) {
        ArgumentValue argumentValue =
                typeAttribute == null ?
                        getArgumentValue(argument) :
                        getArgumentValue(argument, typeAttribute);

        ArgumentValueHolder valueStore = argumentValue.getValueHolder();
        if (valueStore instanceof MethodExecutionArgumentValue) {
            MethodExecutionArgumentValue executionVariable = (MethodExecutionArgumentValue) valueStore;
            return executionVariable.getValueHistory();
        }
        return Collections.emptyList();
    }

    public String getInputValue(DBArgument argument, DBTypeAttribute typeAttribute) {
        ArgumentValue argumentValue = getArgumentValue(argument, typeAttribute);
        return (String) argumentValue.getValue();
    }

    public List<ArgumentValue> getArgumentValues() {
        return argumentValues;
    }

    private ArgumentValue getArgumentValue(@NotNull DBArgument argument) {
        for (ArgumentValue argumentValue : argumentValues) {
            if (Commons.match(argument, argumentValue.getArgument())) {
                return argumentValue;
            }
        }
        ArgumentValue argumentValue = new ArgumentValue(argument, null);
        argumentValue.setValueHolder(getExecutionVariable(argumentValue.getName()));
        argumentValues.add(argumentValue);
        return argumentValue;
    }

    private ArgumentValue getArgumentValue(DBArgument argument, DBTypeAttribute attribute) {
        for (ArgumentValue argumentValue : argumentValues) {
            if (Commons.match(argumentValue.getArgument(), argument) &&
                    Commons.match(argumentValue.getAttribute(), attribute)) {
                return argumentValue;
            }
        }

        ArgumentValue argumentValue = new ArgumentValue(argument, attribute, null);
        argumentValue.setValueHolder(getExecutionVariable(argumentValue.getName()));
        argumentValues.add(argumentValue);
        return argumentValue;
    }

    private MethodExecutionArgumentValue getExecutionVariable(String name) {
        for (MethodExecutionArgumentValue executionVariable : argumentValueHistory.values()) {
            if (Strings.equalsIgnoreCase(executionVariable.getName(), name)) {
                return executionVariable;
            }
        }
        MethodExecutionArgumentValue executionVariable = new MethodExecutionArgumentValue(name);
        argumentValueHistory.put(executionVariable.getName(), executionVariable);
        return executionVariable;
    }

    public Map<String, MethodExecutionArgumentValue> getArgumentValueHistory() {
        return argumentValueHistory;
    }

    @Nullable
    public MethodExecutionResult getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(MethodExecutionResult executionResult) {
        this.executionResult = executionResult;
    }

    /*********************************************************
     *                 PersistentConfiguration               *
     *********************************************************/
    @Override
    public void readConfiguration(Element element) {
        super.readConfiguration(element);
        method.readState(element);
        setTargetSchemaId(SchemaId.get(stringAttribute(element, "execution-schema")));
        Element argumentsElement = element.getChild("argument-actions");
        if (argumentsElement != null) {
            for (Element valueElement : argumentsElement.getChildren()) {
                MethodExecutionArgumentValue argumentValue = new MethodExecutionArgumentValue(valueElement);
                argumentValueHistory.put(argumentValue.getName(), argumentValue);
            }
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        super.writeConfiguration(element);
        method.writeState(element);
        element.setAttribute("execution-schema", getTargetSchemaId() == null ? "" : getTargetSchemaId().id());

        Element argumentsElement = new Element("argument-actions");
        element.addContent(argumentsElement);

        for (MethodExecutionArgumentValue executionVariable : argumentValueHistory.values()) {
            Element argumentElement = new Element("argument");
            executionVariable.writeState(argumentElement);
            argumentsElement.addContent(argumentElement);
        }
    }

    @Override
    public int compareTo(@NotNull MethodExecutionInput executionInput) {
        DBObjectRef<DBMethod> localMethod = method;
        DBObjectRef<DBMethod> remoteMethod = executionInput.method;
        return localMethod.compareTo(remoteMethod);
    }

    @Override
    public MethodExecutionInput clone() {
        MethodExecutionInput clone = new MethodExecutionInput(getProject());
        clone.method = method;
        clone.setTargetSchemaId(getTargetSchemaId());
        clone.setOptions(ExecutionOptions.clone(getOptions()));
        clone.argumentValueHistory = new HashMap<>();
        for (MethodExecutionArgumentValue executionVariable : argumentValueHistory.values()) {
            clone.argumentValueHistory.put(
                    executionVariable.getName(),
                    executionVariable.clone());
        }
        return clone;
    }

}
