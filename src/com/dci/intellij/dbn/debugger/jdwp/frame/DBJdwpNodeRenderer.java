package com.dci.intellij.dbn.debugger.jdwp.frame;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.debugger.jdwp.process.DBJdwpDebugProcess;
import com.intellij.debugger.DebuggerContext;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.ui.impl.watch.ArgumentValueDescriptorImpl;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl;
import com.intellij.debugger.ui.impl.watch.FieldDescriptorImpl;
import com.intellij.debugger.ui.impl.watch.LocalVariableDescriptorImpl;
import com.intellij.debugger.ui.impl.watch.NodeManagerImpl;
import com.intellij.debugger.ui.tree.DebuggerTreeNode;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.ValueDescriptor;
import com.intellij.debugger.ui.tree.render.ChildrenBuilder;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.debugger.ui.tree.render.NodeRendererImpl;
import com.intellij.psi.PsiExpression;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.tools.jdi.ObjectReferenceImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DBJdwpNodeRenderer extends NodeRendererImpl {
    public DBJdwpNodeRenderer() {
        super("DB Value");
        myProperties.setEnabled(true);
    }

    @Override
    public void buildChildren(Value value, ChildrenBuilder builder, EvaluationContext evaluationContext) {
        if (value instanceof ObjectReferenceImpl) {
            List<DebuggerTreeNode> nodes = new ArrayList<DebuggerTreeNode>();
            ObjectReferenceImpl objectReference = (ObjectReferenceImpl) value;

            List<Field> fields = getFields(value);
            for (Field field : fields) {
                NodeManagerImpl nodeManager = (NodeManagerImpl) builder.getNodeManager();
                FieldDescriptorImpl fieldDescriptor = nodeManager.getFieldDescriptor(null, objectReference, field);
                nodes.add(new DebuggerTreeNodeImpl(null, fieldDescriptor));
                System.out.printf("");
            }
            builder.setChildren(nodes);
        }
    }

    @NotNull
    private List<Field> getFields(Value value) {
        Type type = value.type();
        if (type instanceof ReferenceType) {
            ReferenceType referenceType = (ReferenceType) type;
            return referenceType.fields();
        }
        return Collections.emptyList();

    }

    @Override
    public PsiExpression getChildValueExpression(DebuggerTreeNode node, DebuggerContext context) throws EvaluateException {
        return null;
    }

    @Override
    public boolean isExpandable(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {
        List<Field> fields = getFields(value);
        for (Field field : fields) {
            if (field.name().equals("_type")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String calcLabel(ValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) throws EvaluateException {
        ObjectReference value = (ObjectReference) descriptor.getValue();
        List<Field> fields = ((ReferenceType) value.type()).fields();
        String stringValue = "";
        String typeIdentifier = value.type().name();
        DBJdwpDebugProcess debugProcess = evaluationContext.getDebugProcess().getUserData(DBJdwpDebugProcess.KEY);
        String typeName = debugProcess == null ? null : debugProcess.getDebuggerInterface().getJdwpTypeName(typeIdentifier);

        String typeLength = null;
        for (Field field : fields) {
            if (field.name().equals("_value")) {
                Value fieldValue = value.getValue(field);
                stringValue = fieldValue == null ? " null" : fieldValue.toString();

            }/* else if (field.name().equals("_maxLength")) {
                                Value fieldValue = value.getValue(field);
                                if  (fieldValue != null) {
                                    typeLength = fieldValue.toString();
                                }
                            }*/
            else if (field.name().equals("_type")) {
                Value fieldValue = value.getValue(field);
                if  (fieldValue != null) {
                    typeName = fieldValue.toString();
                }
                stringValue = "";
            }
        }

        return stringValue;
    }

    @Override
    public String getUniqueId() {
        return "DBJdwpNodeRendererId";
    }

    @Override
    public boolean isApplicable(Type type) {
        return type != null && type.name().startsWith("$Oracle");
    }

    @Override
    public Icon calcValueIcon(ValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) throws EvaluateException {
        boolean isArgument = false;
        if (descriptor instanceof LocalVariableDescriptorImpl) {
            LocalVariableDescriptorImpl localVariableDescriptor = (LocalVariableDescriptorImpl) descriptor;

            try {
                if (localVariableDescriptor.getLocalVariable().getType().name().endsWith("REFCURSOR")) {
                    return Icons.DBO_CURSOR;
                }

                isArgument = localVariableDescriptor.getLocalVariable().getVariable().isArgument();
            }
            catch (EvaluateException ignored) {}
            catch (ClassNotLoadedException ignore) {}
        }
        else if (descriptor instanceof ArgumentValueDescriptorImpl) {
            isArgument = ((ArgumentValueDescriptorImpl)descriptor).isParameter();
        }
        if (isArgument) {
            return Icons.DBO_ARGUMENT;
        }

        return Icons.DBO_VARIABLE;
    }

    @Override
    public String getName() {
        return super.getName().toLowerCase();
    }

    @Nullable
    @Override
    public String getIdLabel(Value value, DebugProcess process) {
        String label = super.getIdLabel(value, process);
        if (label != null && !label.toLowerCase().startsWith("deprecated")) {
            label = adjustIdLabel(label);
        }

        return label;
    }

    @Nullable
    @Override
    public String calcIdLabel(ValueDescriptor descriptor, DebugProcess process, DescriptorLabelListener labelListener) {
        String label = super.calcIdLabel(descriptor, process, labelListener);
        if (label != null) {
            label = adjustIdLabel(label);
        }
        return label;
    }

    @NotNull
    private static String adjustIdLabel(@NotNull String label) {
        int index = label.indexOf("@");
        if (index > -1) {
            label = label.substring(0, index);
        }
        label = label.toLowerCase();
        return label;
    }
}
