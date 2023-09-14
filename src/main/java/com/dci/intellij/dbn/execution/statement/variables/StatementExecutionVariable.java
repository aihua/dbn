package com.dci.intellij.dbn.execution.statement.variables;

import com.dci.intellij.dbn.common.list.MostRecentStack;
import com.dci.intellij.dbn.common.state.PersistentStateElement;
import com.dci.intellij.dbn.data.type.GenericDataType;
import com.dci.intellij.dbn.language.common.psi.ExecVariablePsiElement;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.StringTokenizer;

import static com.dci.intellij.dbn.common.options.setting.Settings.*;
import static com.dci.intellij.dbn.execution.statement.variables.VariableNames.adjust;

@Getter
@Setter
public class StatementExecutionVariable extends VariableValueProvider implements Comparable<StatementExecutionVariable>, PersistentStateElement {
    private int offset;
    private String name;
    private GenericDataType dataType;
    private MostRecentStack<String> valueHistory = new MostRecentStack<>();
    private VariableValueProvider previewValueProvider;

    public StatementExecutionVariable() {}

    public StatementExecutionVariable(StatementExecutionVariable source) {
        this.dataType = source.dataType;
        this.name = source.name;
        this.valueHistory = new MostRecentStack<>(source.getValueHistory());
    }

    public StatementExecutionVariable(ExecVariablePsiElement variablePsiElement) {
        this.name = adjust(variablePsiElement.getText());
        this.offset = variablePsiElement.getTextOffset();
    }

    @Override
    public String getValue() {
        return valueHistory.get();
    }

    public void setValue(String value) {
        valueHistory.stack(value);
    }

    public Iterable<String> getValueHistory() {
        return valueHistory;
    }

    @NotNull
    public VariableValueProvider getPreviewValueProvider() {
        return previewValueProvider == null ? this : previewValueProvider;
    }

    public boolean isProvided() {
        return valueHistory.get() != null;
    }

    @Override
    public int compareTo(@NotNull StatementExecutionVariable o) {
        return name.compareTo(o.name);
    }

    @Override
    public void readState(Element element) {
        name = adjust(stringAttribute(element, "name"));
        dataType = enumAttribute(element, "data-type", GenericDataType.class);
        // TODO cleanup - attribute rename backward compatibility;
        if (dataType == null) enumAttribute(element, "dataType", GenericDataType.class);

        for (Element child : element.getChildren()) {
            valueHistory.add(child.getText());
        }

        // TODO cleanup - attribute values backward compatibility;
        String variableValues = element.getAttributeValue("values");
        if (variableValues != null) {
            StringTokenizer valuesTokenizer = new StringTokenizer(variableValues, ",");
            while (valuesTokenizer.hasMoreTokens()) {
                String value = valuesTokenizer.nextToken().trim();
                if (Strings.isEmpty(value)) continue;
                valueHistory.add(value);
            }
        }
    }

    @Override
    public void writeState(Element element) {
        element.setAttribute("name", name);
        element.setAttribute("data-type", dataType.name());
        for (String value : valueHistory) {
            if (Strings.isEmpty(value)) continue;

            Element valueElement = newElement(element, "value");
            valueElement.addContent(value);
        }
    }

    public void populate(StatementExecutionVariable variable) {
        setValue(variable.getValue());
    }
}
