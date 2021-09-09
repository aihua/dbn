package com.dci.intellij.dbn.editor.data.filter;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.dispose.Failsafe;
import com.dci.intellij.dbn.common.options.setting.SettingsSupport;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.data.sorting.SortingState;
import com.dci.intellij.dbn.editor.data.filter.ui.DatasetBasicFilterForm;
import com.dci.intellij.dbn.object.DBDataset;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DatasetBasicFilter extends DatasetFilterImpl {
    private final List<DatasetBasicFilterCondition> conditions = new ArrayList<>();
    private ConditionJoinType joinType = ConditionJoinType.AND;


    DatasetBasicFilter(DatasetFilterGroup parent, String name) {
        super(parent, name, DatasetFilterType.BASIC);
    }

    @Override
    public void generateName() {
        if (!isCustomNamed()) {
            boolean addSeparator = false;
            StringBuilder buffer = new StringBuilder();
            for (DatasetBasicFilterCondition condition : conditions) {
                if (condition.isActive() && condition.getValue().trim().length() > 0) {
                    if (addSeparator) buffer.append(joinType == ConditionJoinType.AND ? " & " : " | ");
                    addSeparator = true;
                    buffer.append(condition.getValue());
                    if (buffer.length() > 40) {
                        buffer.setLength(40);
                        buffer.append("...");
                        break;
                    }
                }
            }

            String name =  buffer.length() > 0 ? buffer.toString() : getFilterGroup().createFilterName("Filter");
            setName(name);
        }
    }

    void addCondition(String columnName, Object value, ConditionOperator operator) {
        DatasetBasicFilterCondition condition = new DatasetBasicFilterCondition(this, columnName, value, operator, true);
        addCondition(condition);
    }

    public void addCondition(DatasetBasicFilterCondition condition) {
        conditions.add(condition);
    }

    public boolean containsConditionForColumn(String columnName) {
        for (DatasetBasicFilterCondition condition : conditions) {
            if (condition.getColumnName().equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getVolatileName() {
        ConfigurationEditorForm configurationEditorForm = getSettingsEditor();
        if (configurationEditorForm != null) {
            DatasetBasicFilterForm basicFilterForm = (DatasetBasicFilterForm) configurationEditorForm;
            return basicFilterForm.getFilterName();
        }
        return super.getDisplayName();
    }

    @Override
    public boolean isIgnored() {
        return false;
    }

    @Override
    public Icon getIcon() {
        return  isTemporary() ? (
                    getError() == null ?
                        Icons.DATASET_FILTER_BASIC_TEMP : 
                        Icons.DATASET_FILTER_BASIC_TEMP_ERR) :
                    getError() == null ?
                        Icons.DATASET_FILTER_BASIC :
                        Icons.DATASET_FILTER_BASIC_ERR;
    }

    @Override
    public String createSelectStatement(DBDataset dataset, SortingState sortingState) {
        setError(null);
        StringBuilder buffer = new StringBuilder();
        DatasetFilterUtil.createSimpleSelectStatement(dataset, buffer);
        boolean initialized = false;
        for (DatasetBasicFilterCondition condition : conditions) {
            if (condition.isActive()) {
                if (!initialized) {
                    buffer.append(" where ");
                    initialized = true;
                } else {
                    switch (joinType) {
                        case AND: buffer.append(" and "); break;
                        case OR: buffer.append(" or "); break;
                    }
                }
                condition.appendConditionString(buffer, dataset);
            }
        }

        DatasetFilterUtil.addOrderByClause(dataset, buffer, sortingState);
        return buffer.toString();
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
   @Override
   @NotNull
   public ConfigurationEditorForm createConfigurationEditor() {
       DBDataset dataset = Failsafe.nn(lookupDataset());
       return new DatasetBasicFilterForm(dataset, this);
   }

   @Override
   public void readConfiguration(Element element) {
       super.readConfiguration(element);
       joinType = SettingsSupport.getEnumAttribute(element, "join-type", ConditionJoinType.class);
       for (Element child : element.getChildren()) {
           DatasetBasicFilterCondition condition = new DatasetBasicFilterCondition(this);
           condition.readConfiguration(child);
           conditions.add(condition);
       }
   }

    @Override
    public void writeConfiguration(Element element) {
        super.writeConfiguration(element);
        element.setAttribute("type", "basic");
        element.setAttribute("join-type", joinType.name());
        for (DatasetBasicFilterCondition condition: conditions) {
            Element conditionElement = new Element("condition");
            element.addContent(conditionElement);
            condition.writeConfiguration(conditionElement);
        }
    }
}
