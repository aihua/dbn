package com.dci.intellij.dbn.code.common.completion.options.filter;

import com.dci.intellij.dbn.code.common.completion.options.filter.ui.CheckedTreeNodeProvider;
import com.dci.intellij.dbn.code.common.completion.options.filter.ui.CodeCompletionFilterTreeNode;
import com.dci.intellij.dbn.common.options.PersistentConfiguration;
import com.intellij.ui.CheckedTreeNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CodeCompletionFilterOptionBundle implements CheckedTreeNodeProvider, PersistentConfiguration {
    @EqualsAndHashCode.Exclude
    private final CodeCompletionFilterSettings filterSettings;

    private final List<CodeCompletionFilterOption> options = new ArrayList<>();
    private final String name;

    public CodeCompletionFilterOptionBundle(String name, CodeCompletionFilterSettings filterSettings) {
        this.name = name;
        this.filterSettings = filterSettings;
    }

    @Override
    public void readConfiguration(Element element) {
        for (Element child: element.getChildren()) {
            CodeCompletionFilterOption option = new CodeCompletionFilterOption(filterSettings);
            if (Objects.equals(child.getName(), "filter-element")){
                option.readConfiguration(child);
                CodeCompletionFilterOption local = findOption(option);
                if (local == null) {
                    options.add(option);
                } else {
                    local.readConfiguration(child);
                }
            }
        }
    }

    private CodeCompletionFilterOption findOption(CodeCompletionFilterOption option) {
        for (CodeCompletionFilterOption o : options) {
            if (o.getObjectType() == option.getObjectType() && o.getTokenTypeCategory() == option.getTokenTypeCategory()) {
                return o;
            }
        }
        return null;
    }

    @Override
    public void writeConfiguration(Element element){
        for (CodeCompletionFilterOption option : options) {
            Element child = new Element("filter-element");
            option.writeConfiguration(child);
            element.addContent(child);
        }
    }

    @Override
    public CheckedTreeNode createCheckedTreeNode() {
        CodeCompletionFilterTreeNode node = new CodeCompletionFilterTreeNode(this, false);
        //node.setChecked(true);
        for (CodeCompletionFilterOption option : options) {
            //if (!option.isSelected()) node.setChecked(false);
            node.add(option.createCheckedTreeNode());
        }
        node.updateCheckedStatusFromChildren();
        return node;
    }
}
