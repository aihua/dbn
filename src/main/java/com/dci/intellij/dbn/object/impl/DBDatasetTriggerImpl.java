package com.dci.intellij.dbn.object.impl;

import com.dci.intellij.dbn.browser.ui.HtmlToolTipBuilder;
import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.connection.PooledConnection;
import com.dci.intellij.dbn.connection.SchemaId;
import com.dci.intellij.dbn.database.DatabaseDDLInterface;
import com.dci.intellij.dbn.database.common.metadata.def.DBTriggerMetadata;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.object.DBDataset;
import com.dci.intellij.dbn.object.DBDatasetTrigger;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.status.DBObjectStatus;
import com.dci.intellij.dbn.object.common.status.DBObjectStatusHolder;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.dci.intellij.dbn.object.type.DBTriggerEvent;
import com.dci.intellij.dbn.object.type.DBTriggerType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;

public class DBDatasetTriggerImpl extends DBTriggerImpl implements DBDatasetTrigger {
    DBDatasetTriggerImpl(DBDataset dataset, DBTriggerMetadata metadata) throws SQLException {
        super(dataset, metadata);
    }

    @Override
    public DBDataset getDataset() {
        return (DBDataset) getParentObject();
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.DATASET_TRIGGER;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        DBObjectStatusHolder objectStatus = getStatus();
        if (objectStatus.is(DBObjectStatus.VALID)) {
            if (objectStatus.is(DBObjectStatus.ENABLED)) {
                if (objectStatus.is(DBObjectStatus.DEBUG)) {
                    return Icons.DBO_TRIGGER_DEBUG;
                } else {
                    return Icons.DBO_TRIGGER;
                }
            } else {
                if (objectStatus.is(DBObjectStatus.DEBUG)) {
                    return Icons.DBO_TRIGGER_DISABLED_DEBUG;
                } else {
                    return Icons.DBO_TRIGGER_DISABLED;
                }
            }
        } else {
            if (objectStatus.is(DBObjectStatus.ENABLED)) {
                return Icons.DBO_TRIGGER_ERR;
            } else {
                return Icons.DBO_TRIGGER_ERR_DISABLED;
            }

        }
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        DBTriggerType triggerType = getTriggerType();
        DBTriggerEvent[] triggeringEvents = getTriggerEvents();
        ttb.append(true, getObjectType().getName(), true);
        StringBuilder triggerDesc = new StringBuilder();
        triggerDesc.append(" - ");
        triggerDesc.append(triggerType.getName().toLowerCase());
        triggerDesc.append(" ") ;

        for (DBTriggerEvent triggeringEvent : triggeringEvents) {
            if (triggeringEvent != triggeringEvents[0]) triggerDesc.append(" or ");
            triggerDesc.append(triggeringEvent.getName());
        }
        triggerDesc.append(" on ");
        triggerDesc.append(getDataset().getName());

        ttb.append(false, triggerDesc.toString(), false);

        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    /*********************************************************
     *                         Loaders                       *
     *********************************************************/

    @Override
    public void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException {
        DBSchema schema = getSchema();
        PooledConnection.run(false, getConnection(), SchemaId.from(schema), connection -> {
            DatabaseDDLInterface ddlInterface = getConnection().getInterfaceProvider().getDdlInterface();
            DBDataset dataset = getDataset();
            ddlInterface.updateTrigger(
                    dataset.getSchema().getName(),
                    dataset.getName(),
                    getName(),
                    oldCode,
                    newCode,
                    connection);
        });
    }
}
