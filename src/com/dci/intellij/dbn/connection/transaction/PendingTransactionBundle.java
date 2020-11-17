package com.dci.intellij.dbn.connection.transaction;


import com.dci.intellij.dbn.common.util.CollectionUtil;
import com.dci.intellij.dbn.connection.SessionId;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PendingTransactionBundle {
    private final List<PendingTransaction> entries = new ArrayList<>();

    public void notifyChange(VirtualFile file, DBNConnection connection){

        PendingTransaction pendingTransaction = getPendingTransaction(file, connection.getSessionId());
        if (pendingTransaction == null) {
            pendingTransaction = new PendingTransaction(connection, file);
            entries.add(pendingTransaction);
        }
        pendingTransaction.incrementChangesCount();
    }

    @Nullable
    public PendingTransaction getPendingTransaction(VirtualFile file, SessionId sessionId) {
        return CollectionUtil.first(entries, transaction ->
                file.equals(transaction.getFile()) &&
                transaction.getSessionId().equals(sessionId));
    }

    public List<PendingTransaction> getEntries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
