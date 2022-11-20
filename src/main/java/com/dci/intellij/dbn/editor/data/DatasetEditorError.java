package com.dci.intellij.dbn.editor.data;

import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.database.DatabaseObjectIdentifier;
import com.dci.intellij.dbn.database.interfaces.DatabaseMessageParserInterface;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode
public class DatasetEditorError {
    private final DBObjectRef<?> messageObject;
    private final String message;
    private boolean dirty;
    private boolean notified;

    @EqualsAndHashCode.Exclude
    private final Set<ChangeListener> changeListeners = new HashSet<>();

    public DatasetEditorError(ConnectionHandler connection, Exception exception) {
        this.message = exception.getMessage();
        DBObject messageObject = resolveMessageObject(connection, exception);
        this.messageObject = DBObjectRef.of(messageObject);
    }

    public DBObject getMessageObject() {
        return DBObjectRef.get(messageObject);
    }

    @Nullable
    private static DBObject resolveMessageObject(ConnectionHandler connection, Exception exception) {
        DBObject messageObject = null;
        if (exception instanceof SQLException) {
            DatabaseMessageParserInterface messageParserInterface = connection.getMessageParserInterface();
            DatabaseObjectIdentifier objectIdentifier = messageParserInterface.identifyObject((SQLException) exception);
            if (objectIdentifier != null) {
                messageObject = connection.getObjectBundle().getObject(objectIdentifier);
            }
        }
        return messageObject;
    }

    public DatasetEditorError(String message, DBObject messageObject) {
        this.message = message;
        this.messageObject = DBObjectRef.of(messageObject);
    }

    public void addChangeListener(ChangeListener changeListener) {
        changeListeners.add(changeListener);
    }

    public void removeChangeListener(ChangeListener changeListener) {
        changeListeners.remove(changeListener);
    }

    public void markDirty() {
        dirty = true;
        ChangeEvent changeEvent = new ChangeEvent(this);
        for (ChangeListener changeListener: changeListeners) {
            changeListener.stateChanged(changeEvent);
        }
    }
}
