package com.dci.intellij.dbn.execution.method.ui;

import com.dci.intellij.dbn.common.dispose.Disposed;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.options.setting.Settings;
import com.dci.intellij.dbn.common.project.ProjectRef;
import com.dci.intellij.dbn.common.state.PersistentStateElement;
import com.dci.intellij.dbn.common.util.CollectionUtil;
import com.dci.intellij.dbn.common.util.Unsafe;
import com.dci.intellij.dbn.connection.ConnectionId;
import com.dci.intellij.dbn.connection.config.ConnectionConfigListener;
import com.dci.intellij.dbn.execution.method.MethodExecutionInput;
import com.dci.intellij.dbn.object.DBFunction;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.DBProcedure;
import com.dci.intellij.dbn.object.DBProgram;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.dci.intellij.dbn.common.dispose.Disposer.replace;

@Getter
@Setter
public class MethodExecutionHistory implements PersistentStateElement, ConnectionConfigListener, Disposable{
    private final ProjectRef project;
    private boolean groupEntries = true;
    private DBObjectRef<DBMethod> selection;

    private List<MethodExecutionInput> executionInputs = CollectionUtil.createConcurrentList();

    public MethodExecutionHistory(Project project) {
        this.project = ProjectRef.of(project);
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    public List<MethodExecutionInput> getExecutionInputs() {
        return executionInputs;
    }

    public void setExecutionInputs(List<MethodExecutionInput> executionInputs) {
        this.executionInputs.clear();
        this.executionInputs.addAll(executionInputs);
    }

    public void cleanupHistory(List<ConnectionId> connectionIds) {
        executionInputs.removeIf(executionInput -> connectionIds.contains(executionInput.getConnectionId()));
    }

    public void connectionRemoved(ConnectionId connectionId) {
        executionInputs.removeIf(executionInput -> connectionId.equals(executionInput.getConnectionId()));
        if (selection != null && Objects.equals(selection.getConnectionId(), connectionId)) {
            selection = null;
        }
    }

    @Nullable
    public List<DBMethod> getRecentlyExecutedMethods(@NotNull DBProgram program) {
        List<DBMethod> recentObjects = new ArrayList<>();
        List<DBProcedure> procedures = program.getProcedures();
        List<DBFunction> functions = program.getFunctions();
        for (DBProcedure procedure : procedures) {
            MethodExecutionInput executionInput = getExecutionInput(procedure, false);
            if (executionInput != null) {
                recentObjects.add(procedure);
            }
        }
        for (DBFunction function : functions) {
            MethodExecutionInput executionInput = getExecutionInput(function, false);
            if (executionInput != null) {
                recentObjects.add(function);
            }
        }
        return recentObjects.isEmpty() ? null : recentObjects;
    }

    @NotNull
    public MethodExecutionInput getExecutionInput(@NotNull DBMethod method) {
        MethodExecutionInput executionInput = getExecutionInput(method, true);
        return Failsafe.nn(executionInput);
    }

    @Nullable
    public MethodExecutionInput getExecutionInput(@NotNull DBMethod method, boolean create) {
        return getExecutionInput(method.ref(), create);
    }

    public MethodExecutionInput getExecutionInput(DBObjectRef method, boolean create) {
        for (MethodExecutionInput executionInput : executionInputs) {
            if (executionInput.getMethodRef().equals(method)) {
                return executionInput;
            }
        }

        if (create) {
            return createExecutionInput(method);
        }

        return null;
    }

    @NotNull
    private MethodExecutionInput createExecutionInput(@NotNull DBObjectRef<DBMethod> method) {
        MethodExecutionInput executionInput = getExecutionInput(method, false);
        if (executionInput == null) {
            synchronized (this) {
                executionInput = getExecutionInput(method, false);
                if (executionInput == null) {
                    executionInput = new MethodExecutionInput(getProject(), method);
                    executionInputs.add(executionInput);
                    Collections.sort(executionInputs);
                    selection = method;
                    return executionInput;
                }
            }
        }
        return executionInput;
    }

    @Nullable
    public MethodExecutionInput getLastSelection() {
        if (selection != null) {
            for (MethodExecutionInput executionInput : executionInputs) {
                if (executionInput.getMethodRef().equals(selection)) {
                    return executionInput;
                }
            }
        }
        return null;
    }


    /*****************************************
     *         PersistentStateElement        *
     *****************************************/
    @Override
    public void readState(Element element) {
        executionInputs.clear();
        Element historyElement = element.getChild("execution-history");
        if (historyElement != null) {
            groupEntries = Settings.getBoolean(historyElement, "group-entries", groupEntries);

            Element executionInputsElement = historyElement.getChild("execution-inputs");
            for (Element child : executionInputsElement.getChildren()) {
                Unsafe.warned(() -> {
                    MethodExecutionInput executionInput = new MethodExecutionInput(getProject());
                    executionInput.readConfiguration(child);
                    if (getExecutionInput(executionInput.getMethodRef(), false) == null) {
                        executionInputs.add(executionInput);
                    }
                });
            }
            Collections.sort(executionInputs);

            Element selectionElement = historyElement.getChild("selection");
            if (selectionElement != null) {
                selection = new DBObjectRef<>();
                selection.readState(selectionElement);
            }
        }
    }

    @Override
    public void writeState(Element element) {
        Element historyElement = new Element("execution-history");
        element.addContent(historyElement);

        Settings.setBoolean(historyElement, "group-entries", groupEntries);

        Element configsElement = new Element("execution-inputs");
        historyElement.addContent(configsElement);
        for (MethodExecutionInput executionInput : this.executionInputs) {
            if (!executionInput.isObsolete()) {
                Element configElement = new Element("execution-input");
                executionInput.writeConfiguration(configElement);
                configsElement.addContent(configElement);
            }
        }

        if (selection != null) {
            Element selectionElement = new Element("selection");
            historyElement.addContent(selectionElement);
            selection.writeState(selectionElement);
        }
    }

    @Override
    public void dispose() {
        executionInputs = replace(executionInputs, Disposed.list());
    }


}
