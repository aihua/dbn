package com.dci.intellij.dbn.language.psql.structure;

import com.dci.intellij.dbn.common.util.Naming;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.ide.util.treeView.smartTree.Group;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PSQLStructureViewModelGroup implements Group {
    private static TextAttributesKey TEXT_ATTRIBUTES_KEY =
            TextAttributesKey.createTextAttributesKey(
                    "PSQLStructureViewModelGroup",
                    new TextAttributes(JBColor.BLACK, null, null, null, Font.BOLD));

    private DBObjectType objectType;
    private List<TreeElement> children = new ArrayList<TreeElement>();


    PSQLStructureViewModelGroup(DBObjectType objectType) {
        this.objectType = objectType;
    }

    public void addChild(TreeElement treeElement) {
        children.add(treeElement);
    }

    @Override
    @NotNull
    public ItemPresentation getPresentation() {
        return itemPresentation;
    }

    @Override
    @NotNull
    public Collection<TreeElement> getChildren() {
        return children;
    }


    private ItemPresentation itemPresentation = new ItemPresentation(){
        @Override
        public String getPresentableText() {
            return Naming.capitalize(objectType.getListName());
        }

        @Override
        public String getLocationString() {
            return null;
        }

        @Override
        public Icon getIcon(boolean open) {
            return null;//objectType.getListIcon();
        }

        public TextAttributesKey getTextAttributesKey() {
            return TEXT_ATTRIBUTES_KEY;
        }
    };
}