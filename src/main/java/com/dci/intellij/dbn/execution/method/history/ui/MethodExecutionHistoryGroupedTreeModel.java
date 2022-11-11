package com.dci.intellij.dbn.execution.method.history.ui;

import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.execution.method.MethodExecutionInput;
import com.dci.intellij.dbn.object.DBMethod;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.object.type.DBObjectType;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodExecutionHistoryGroupedTreeModel extends MethodExecutionHistoryTreeModel {

    MethodExecutionHistoryGroupedTreeModel(List<MethodExecutionInput> executionInputs, boolean debug) {
        super(executionInputs);
        for (MethodExecutionInput executionInput : executionInputs) {
            if (!executionInput.isObsolete() &&
                    !executionInput.isInactive() &&
                    (!debug || DatabaseFeature.DEBUGGING.isSupported(executionInput.getConnection()))) {
                RootTreeNode root = getRoot();

                if (root != null) {
                    ConnectionTreeNode connectionNode = root.getConnectionNode(executionInput);
                    SchemaTreeNode schemaNode = connectionNode.getSchemaNode(executionInput);

                    DBObjectRef<DBMethod> methodRef = executionInput.getMethodRef();
                    DBObjectRef<?> parentRef = methodRef.getParentRef(DBObjectType.PROGRAM);
                    if (parentRef != null) {
                        ProgramTreeNode programNode = schemaNode.getProgramNode(executionInput);
                        programNode.getMethodNode(executionInput);
                    } else {
                        schemaNode.getMethodNode(executionInput);
                    }
                }
            }
        }
    }

    @Override
    protected String getMethodName(MethodExecutionInput executionInput) {
        return executionInput.getMethodRef().getObjectName();
    }

    @Override
    public TreePath getTreePath(MethodExecutionInput executionInput) {
        List<MethodExecutionHistoryTreeNode> path = new ArrayList<>();
        RootTreeNode root = getRoot();
        if (root != null) {
            ConnectionTreeNode connectionTreeNode = root.getConnectionNode(executionInput);
            SchemaTreeNode schemaTreeNode = connectionTreeNode.getSchemaNode(executionInput);

            path.add(root);
            path.add(connectionTreeNode);
            path.add(schemaTreeNode);
            if (executionInput.getMethodRef().getParentObject(DBObjectType.PROGRAM) != null) {
                ProgramTreeNode programTreeNode = schemaTreeNode.getProgramNode(executionInput);
                path.add(programTreeNode);
                MethodTreeNode methodTreeNode = programTreeNode.getMethodNode(executionInput);
                path.add(methodTreeNode);
            } else {
                MethodTreeNode methodTreeNode = schemaTreeNode.getMethodNode(executionInput);
                path.add(methodTreeNode);
            }
        }

        return new TreePath(path.toArray());
    }

    @Override
    public List<MethodExecutionInput> getExecutionInputs() {
        RootTreeNode root = getRoot();
        if (root != null) {
            List<TreeNode> children = root.getChildren();
            if (children != null && !children.isEmpty()) {
                List<MethodExecutionInput> executionInputs = new ArrayList<>();
                for (TreeNode connectionTreeNode : children) {
                    ConnectionTreeNode connectionNode = (ConnectionTreeNode) connectionTreeNode;
                    for (TreeNode schemaTreeNode : connectionNode.getChildren()) {
                        SchemaTreeNode schemaNode = (SchemaTreeNode) schemaTreeNode;
                        for (TreeNode node : schemaNode.getChildren()) {
                            if (node instanceof ProgramTreeNode) {
                                ProgramTreeNode programNode = (ProgramTreeNode) node;
                                for (TreeNode methodTreeNode : programNode.getChildren()) {
                                    MethodTreeNode methodNode = (MethodTreeNode) methodTreeNode;
                                    MethodExecutionInput executionInput =
                                            getExecutionInput(connectionNode, schemaNode, programNode, methodNode);

                                    if (executionInput != null) {
                                        executionInputs.add(executionInput);
                                    }
                                }

                            } else {
                                MethodTreeNode methodNode = (MethodTreeNode) node;
                                MethodExecutionInput executionInput =
                                        getExecutionInput(connectionNode, schemaNode, null, methodNode);

                                if (executionInput != null) {
                                    executionInputs.add(executionInput);
                                }
                            }
                        }
                    }
                }
                return executionInputs;
            }
        }
        return Collections.emptyList();
    }

    private MethodExecutionInput getExecutionInput(
            ConnectionTreeNode connectionNode,
            SchemaTreeNode schemaNode,
            ProgramTreeNode programNode,
            MethodTreeNode methodNode) {
        for (MethodExecutionInput executionInput : executionInputs) {
            DBObjectRef<DBMethod> methodRef = executionInput.getMethodRef();
            ConnectionHandler connection = executionInput.getConnection();
            if (connection != null && connection.getConnectionId().equals(connectionNode.getConnectionId()) &&
                Strings.equalsIgnoreCase(methodRef.getSchemaName(), schemaNode.getName()) &&
                Strings.equalsIgnoreCase(methodRef.getObjectName(), methodNode.getName()) &&
                methodRef.getOverload() == methodNode.getOverload() ) {

                DBObjectRef<?> programRef = methodRef.getParentRef(DBObjectType.PROGRAM);
                if (programNode == null && programRef == null) {
                    return executionInput;
                } else if (programNode != null && programRef != null){
                    String programName = programNode.getName();
                    String inputProgramName = programRef.getObjectName();
                    if (Strings.equalsIgnoreCase(programName, inputProgramName)) {
                        return executionInput;
                    }
                }
            }
        }
        return null;
    }



}
