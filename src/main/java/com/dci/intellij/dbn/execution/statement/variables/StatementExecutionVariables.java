package com.dci.intellij.dbn.execution.statement.variables;

import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.state.PersistentStateElement;
import com.dci.intellij.dbn.common.util.Files;
import com.dci.intellij.dbn.common.util.Strings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.dci.intellij.dbn.execution.statement.variables.VariableNames.adjust;

public class StatementExecutionVariables implements PersistentStateElement {
    private final ProjectRef project;
    private final Map<String, Set<StatementExecutionVariable>> variables = new HashMap<>();

    public StatementExecutionVariables(Project project) {
        this.project = ProjectRef.of(project);
    }

    public Project getProject() {
        return project.ensure();
    }

    public Set<StatementExecutionVariable> getVariables(@Nullable VirtualFile virtualFile) {
        if (virtualFile != null) {
            String fileUrl = virtualFile.getUrl();
            return variables.computeIfAbsent(fileUrl, u -> new HashSet<>());
        }
        return Collections.emptySet();
    }

    public void cacheVariable(@Nullable VirtualFile virtualFile, StatementExecutionVariable executionVariable) {
        if (virtualFile == null) return;

        Set<StatementExecutionVariable> variables = getVariables(virtualFile);
        for (StatementExecutionVariable variable : variables) {
            if (Objects.equals(variable.getName(), executionVariable.getName())) {
                variable.setValue(executionVariable.getValue());
                return;
            }
        }
        variables.add(new StatementExecutionVariable(executionVariable));
    }

    @Nullable
    public StatementExecutionVariable getVariable(@Nullable VirtualFile virtualFile, String name) {
        if (virtualFile == null) return null;

        name = adjust(name);
        Set<StatementExecutionVariable> variables = getVariables(virtualFile);
        for (StatementExecutionVariable variable : variables) {
            if (Strings.equals(variable.getName(), name)) {
                return variable;
            }
        }
        return null;
    }

    /*********************************************
     *            PersistentStateElement         *
     *********************************************/

    @Override
    public void readState(Element element) {
        Element variablesElement = element.getChild("execution-variables");
        if (variablesElement == null) return;

        this.variables.clear();
        for (Element fileElement : variablesElement.getChildren()) {
            String fileUrl = fileElement.getAttributeValue("file-url");
            if ( Strings.isEmpty(fileUrl)) {
                // TODO backward compatibility. Do cleanup
                fileUrl = fileElement.getAttributeValue("path");
            }

            Set<StatementExecutionVariable> fileVariables = new HashSet<>();
            this.variables.put(fileUrl, fileVariables);

            for (Element child : fileElement.getChildren()) {
                StatementExecutionVariable executionVariable = new StatementExecutionVariable();
                executionVariable.readState(child);
                fileVariables.add(executionVariable);
            }
        }
    }

    @Override
    public void writeState(Element element) {
        Element variablesElement = new Element("execution-variables");
        element.addContent(variablesElement);

        for (val entry : variables.entrySet()) {
            String fileUrl = entry.getKey();
            if (Files.isValidFileUrl(fileUrl, getProject())) {
                Element fileElement = new Element("file");
                fileElement.setAttribute("file-url", fileUrl);
                for (StatementExecutionVariable executionVariable : entry.getValue()) {
                    Element variableElement = new Element("variable");
                    executionVariable.writeState(variableElement);
                    fileElement.addContent(variableElement);
                }
                variablesElement.addContent(fileElement);
            }
        }
    }
}
