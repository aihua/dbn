package com.dci.intellij.dbn.database.interfaces;

import com.dci.intellij.dbn.connection.jdbc.DBNConnection;

import java.sql.SQLException;

public interface DatabaseInterface {

    default void reset() {
    }

    interface Callable<T> {
        T call() throws SQLException;
    }

    interface Runnable {
        void run() throws SQLException;
    }

    interface ConnectionCallable<T> {
        T call(DBNConnection conn) throws SQLException;
    }

    interface ConnectionRunnable {
        void run(DBNConnection conn) throws SQLException;
    }

}
