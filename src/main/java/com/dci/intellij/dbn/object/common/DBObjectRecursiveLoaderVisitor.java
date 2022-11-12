package com.dci.intellij.dbn.object.common;

import com.dci.intellij.dbn.common.dispose.StatefulDisposable;
import com.dci.intellij.dbn.common.load.ProgressMonitor;
import com.dci.intellij.dbn.object.common.list.DBObjectList;
import com.dci.intellij.dbn.object.common.list.DBObjectListContainer;
import com.dci.intellij.dbn.object.common.list.DBObjectListVisitor;

import java.util.List;

import static com.dci.intellij.dbn.common.util.Unsafe.cast;

public class DBObjectRecursiveLoaderVisitor extends StatefulDisposable.Base implements DBObjectListVisitor{
    public static final DBObjectRecursiveLoaderVisitor INSTANCE = new DBObjectRecursiveLoaderVisitor();

    private DBObjectRecursiveLoaderVisitor() {
    }

    @Override
    public void visit(DBObjectList<?> objectList) {
        if (!objectList.isSubContent()) {
            List<DBObject> objects = cast(objectList.getObjects());
            for (DBObject object : objects) {
                ProgressMonitor.checkCancelled();
                checkDisposed();

                DBObjectListContainer childObjects = object.getChildObjects();
                if (childObjects != null) {
                    childObjects.visitObjects(this, false);
                }
            }
        }
    }

    @Override
    protected void disposeInner() {
    }
}