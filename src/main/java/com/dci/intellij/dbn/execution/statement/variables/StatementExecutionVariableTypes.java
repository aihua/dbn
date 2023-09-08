package com.dci.intellij.dbn.execution.statement.variables;

import com.dci.intellij.dbn.common.state.PersistentStateElement;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.data.type.GenericDataType;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dci.intellij.dbn.common.options.setting.Settings.*;
import static com.dci.intellij.dbn.execution.statement.variables.VariableNames.adjust;

public class StatementExecutionVariableTypes implements PersistentStateElement {
    private final Map<ConnectionId, Map<String, GenericDataType>> variableTypes = new ConcurrentHashMap<>();

    @Nullable
    public GenericDataType getVariableDataType(ConnectionId connectionId, String variableName) {
        val variableTypes = this.variableTypes.get(connectionId);
        if (variableTypes == null) return null;

        variableName = adjust(variableName);
        return variableTypes.get(variableName);
    }

    public void setVariableDataType(ConnectionId connectionId, String variableName, GenericDataType dataType) {
        val variableTypes = this.variableTypes.computeIfAbsent(connectionId, id -> new ConcurrentHashMap<>());

        variableName = adjust(variableName);
        variableTypes.put(variableName, dataType);
    }

    @Override
    public void readState(Element element) {
        Element root = element.getChild("execution-variable-types");
        if (root == null) return;

        for (Element child : root.getChildren()) {
            ConnectionId connectionId = ConnectionId.get(stringAttribute(child, "connection-id"));
            String variableName = adjust(stringAttribute(child, "name"));
            GenericDataType variableType = enumAttribute(child, "data-type", GenericDataType.LITERAL);
            Map<String, GenericDataType> parameters = variableTypes.computeIfAbsent(connectionId, id -> new ConcurrentHashMap<>());
            parameters.put(variableName, variableType);
        }
    }

    @Override
    public void writeState(Element element) {
        Element root = new Element("execution-variable-types");
        element.addContent(root);
        for (val entry : variableTypes.entrySet()) {
            ConnectionId connectionId = entry.getKey();
            Map<String, GenericDataType> parameters = entry.getValue();
            for (val paramEntry : parameters.entrySet()) {
                Element child = new Element("variable");
                String parameterName = paramEntry.getKey();
                GenericDataType parameterType = paramEntry.getValue();

                setStringAttribute(child, "connection-id", connectionId.id());
                setStringAttribute(child, "name", parameterName);
                setEnumAttribute(child, "data-type", parameterType);
                root.addContent(child);
            }
        }
    }

}
