package com.dci.intellij.dbn.execution.method.history.ui;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.util.Commons;
import com.dci.intellij.dbn.object.type.DBObjectType;
import lombok.Getter;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.List;

public class MethodExecutionHistoryTreeNode extends DefaultMutableTreeNode {
    public enum Type {
        ROOT,
        CONNECTION,
        SCHEMA,
        PACKAGE,
        TYPE,
        PROCEDURE,
        FUNCTION,
        UNKNOWN
    }
    private final @Getter String name;
    private final @Getter Type type;

    public MethodExecutionHistoryTreeNode(MethodExecutionHistoryTreeNode parent, Type type, String name) {
        this.name = name;
        this.type = type;
        if (parent != null) {
            parent.add(this);
        }
    }
    public Icon getIcon() {
        return
            type == Type.CONNECTION? Icons.CONNECTION_CONNECTED :
            type == Type.SCHEMA ? Icons.DBO_SCHEMA :
            type == Type.PACKAGE ? Icons.DBO_PACKAGE :
            type == Type.TYPE ? Icons.DBO_TYPE :
            type == Type.PROCEDURE ? Icons.DBO_PROCEDURE :
            type == Type.FUNCTION ? Icons.DBO_FUNCTION : null;
    }

    public static Type getNodeType(DBObjectType objectType) {
        return
            objectType == DBObjectType.SCHEMA ? Type.SCHEMA :
            objectType == DBObjectType.PACKAGE ? Type.PACKAGE :
            objectType == DBObjectType.TYPE ? Type.TYPE :
            objectType == DBObjectType.PROCEDURE ||
                    objectType == DBObjectType.PACKAGE_PROCEDURE ||
                    objectType == DBObjectType.TYPE_PROCEDURE ? Type.PROCEDURE :
            objectType == DBObjectType.FUNCTION ||
                    objectType == DBObjectType.PACKAGE_FUNCTION ||
                    objectType == DBObjectType.TYPE_FUNCTION ? Type.FUNCTION : Type.UNKNOWN;
    }

    public List<TreeNode> getChildren() {
        return Commons.nvl(children, () -> Collections.emptyList());
    }

    @Override
    public boolean getAllowsChildren() {
        return
            type != Type.PROCEDURE &&
            type != Type.FUNCTION;
    }

    public boolean isValid() {
        return true;
    }
}
