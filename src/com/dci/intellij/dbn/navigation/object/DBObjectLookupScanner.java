package com.dci.intellij.dbn.navigation.object;

import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.routine.AsyncTaskExecutor;
import com.dci.intellij.dbn.common.thread.ThreadPool;
import com.dci.intellij.dbn.common.util.CollectionUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.dci.intellij.dbn.connection.VirtualConnectionHandler;
import com.dci.intellij.dbn.object.DBSchema;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.common.list.DBObjectList;
import com.dci.intellij.dbn.object.common.list.DBObjectListContainer;
import com.dci.intellij.dbn.object.common.list.DBObjectListVisitor;
import com.dci.intellij.dbn.object.type.DBObjectType;

import java.util.List;
import java.util.concurrent.TimeUnit;

class DBObjectLookupScanner extends StatefulDisposable.Base implements DBObjectListVisitor {
    private final DBObjectLookupModel model;
    private final boolean forceLoad;
    private final AsyncTaskExecutor asyncScanner = new AsyncTaskExecutor(
            ThreadPool.objectLookupExecutor(), 3, TimeUnit.SECONDS);

    DBObjectLookupScanner(DBObjectLookupModel model, boolean forceLoad) {
        this.model = model;
        this.forceLoad = forceLoad;
    }

    @Override
    public void visit(DBObjectList<DBObject> objectList) {
        if (isScannable(objectList)) {
            DBObjectType objectType = objectList.getObjectType();
            asyncScanner.submit(() -> {
                boolean lookupEnabled = model.isObjectLookupEnabled(objectType);
                for (DBObject object : objectList.getObjects()) {
                    checkDisposed();

                    if (lookupEnabled) {
                        model.getData().consume(object);
                    }

                    DBObjectListContainer childObjects = object.getChildObjects();
                    if (childObjects != null) childObjects.visitLists(this, false);
                }
            });
        }
    }

    public void scan() {
        ConnectionHandler selectedConnection = model.getSelectedConnection();
        DBSchema selectedSchema = model.getSelectedSchema();

        if (selectedConnection == null || selectedConnection instanceof VirtualConnectionHandler) {
            ConnectionManager connectionManager = ConnectionManager.getInstance(model.getProject());
            List<ConnectionHandler> connectionHandlers = connectionManager.getConnectionHandlers();
            CollectionUtil.forEach(
                    connectionHandlers,
                    (connectionHandler -> {
                        model.checkCancelled();

                        DBObjectListContainer objectListContainer = connectionHandler.getObjectBundle().getObjectListContainer();
                        objectListContainer.visitLists(this, false);
                    }));
        } else {
            DBObjectListContainer objectListContainer =
                    selectedSchema == null ?
                            selectedConnection.getObjectBundle().getObjectListContainer() :
                            selectedSchema.getChildObjects();
            if (objectListContainer != null) {
                objectListContainer.visitLists(this, false);
            }
        }
        asyncScanner.awaitCompletion();
    }

    private boolean isScannable(DBObjectList<DBObject> objectList) {
        if (objectList != null && !objectList.isInternal()) {
            if (model.isListLookupEnabled(objectList.getObjectType())) {
                if (objectList.isLoaded() || objectList.canLoadFast() || forceLoad) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void checkDisposed() {
        super.checkDisposed();
        model.checkCancelled();
    }

    @Override
    protected void disposeInner() {
        nullify();
    }
}
