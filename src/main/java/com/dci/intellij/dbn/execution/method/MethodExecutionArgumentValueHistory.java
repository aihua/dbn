package com.dci.intellij.dbn.execution.method;

import com.dci.intellij.dbn.common.state.PersistentStateElement;
import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.config.ConnectionConfigListener;
import lombok.val;
import org.jdom.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.connectionIdAttribute;
import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.stringAttribute;

public class MethodExecutionArgumentValueHistory implements PersistentStateElement, ConnectionConfigListener {
    private final Map<ConnectionId, Map<String, MethodExecutionArgumentValue>> argumentValues = new ConcurrentHashMap<>();

    public MethodExecutionArgumentValue getArgumentValue(ConnectionId connectionId, String name, boolean create) {
        Map<String, MethodExecutionArgumentValue> argumentValues = this.argumentValues.get(connectionId);

        if (argumentValues != null) {
            for (String argumentName : argumentValues.keySet()) {
                if (Strings.equalsIgnoreCase(argumentName, name)) {
                    return argumentValues.get(argumentName);
                }
            }
        }

        if (create) {
            if (argumentValues == null) {
                argumentValues = new HashMap<>();
                this.argumentValues.put(connectionId, argumentValues);
            }

            MethodExecutionArgumentValue argumentValue = new MethodExecutionArgumentValue(name);
            argumentValues.put(name, argumentValue);
            return argumentValue;

        }
        return null;
    }

    public void cacheVariable(ConnectionId connectionId, String name, String value) {
        if (Strings.isNotEmpty(value)) {
            MethodExecutionArgumentValue argumentValue = getArgumentValue(connectionId, name, true);
            argumentValue.setValue(value);
        }
    }

    public void connectionRemoved(ConnectionId connectionId) {
        argumentValues.remove(connectionId);
    }

    /*********************************************
     *            PersistentStateElement         *
     *********************************************/
    @Override
    public void readState(Element element) {
        Element argumentValuesElement = element.getChild("argument-values-cache");
        if (argumentValuesElement != null) {
            this.argumentValues.clear();
            for (Element argumentValueElement : argumentValuesElement.getChildren()) {
                ConnectionId connectionId = connectionIdAttribute(argumentValueElement, "connection-id");
                for (Element argumentElement : argumentValueElement.getChildren()) {
                    String name = stringAttribute(argumentElement, "name");
                    MethodExecutionArgumentValue argumentValue = getArgumentValue(connectionId, name, true);
                    argumentValue.readState(argumentElement);
                }
            }
        }
    }

    @Override
    public void writeState(Element element) {
        Element argumentValuesElement = new Element("argument-values-cache");
        element.addContent(argumentValuesElement);

        for (val entry : argumentValues.entrySet()) {
            ConnectionId connectionId = entry.getKey();
            Element connectionElement = new Element("connection");
            connectionElement.setAttribute("connection-id", connectionId.id());
            argumentValuesElement.addContent(connectionElement);

            for (val argumentEntry : entry.getValue().entrySet()) {
                MethodExecutionArgumentValue argumentValue = argumentEntry.getValue();
                if (argumentValue.getValueHistory().size() > 0) {
                    Element argumentElement = new Element("argument");
                    connectionElement.addContent(argumentElement);
                    argumentValue.writeState(argumentElement);
                }

            }
        }
    }
}
