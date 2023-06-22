package com.dci.intellij.dbn.diagnostics.ui.model;

import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionRef;
import com.dci.intellij.dbn.diagnostics.DiagnosticsManager;
import com.dci.intellij.dbn.diagnostics.data.DiagnosticBundle;
import com.dci.intellij.dbn.diagnostics.data.DiagnosticEntry;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class MetadataDiagnosticsTableModel2 extends AbstractDiagnosticsTableModel<String> {
    private final ConnectionRef connection;

    private static final String[] COLUMN_NAMES = new String[]{
            "Identifier",                  // 0
            "Invocations",                 // 1
            "Failures",                    // 2
            "Timeouts",                    // 3
            "Best (Query / Load - ms)",    // 4
            "Worst (Query / Load - ms)",   // 5
            "Average (Query / Load - ms)", // 6
            "Total (Query / Load - ms)",   // 7
            "Fetch Block Size"};           // 8

    public MetadataDiagnosticsTableModel2(ConnectionHandler connection) {
        super(connection.getProject());
        this.connection = connection.ref();
    }

    @NotNull
    @Override
    protected String[] getColumnNames() {
        return COLUMN_NAMES;
    }

    @NotNull
    @Override
    protected DiagnosticBundle<String> resolveDiagnostics() {
        DiagnosticsManager diagnosticsManager = DiagnosticsManager.getInstance(getProject());
        return diagnosticsManager.getMetadataInterfaceDiagnostics(connection.getConnectionId());
    }

    @Override
    public Object getValue(DiagnosticEntry<String> entry, int column) {
        DiagnosticEntry<String> q = entry.getDetail("QUERY");
        DiagnosticEntry<String> l = entry.getDetail("LOAD");
        switch (column) {
            case 0: return q.getIdentifier();
            case 1: return q.getInvocations();
            case 2: return q.getFailures();
            case 3: return q.getTimeouts();
            case 4: return q.getBest() + " / " + l.getBest();
            case 5: return q.getWorst() + " / " + l.getWorst();
            case 6: return q.getAverage() + " / " + l.getAverage();
            case 7: return q.getTotal() + " / " + l.getTotal();
            case 8: return entry.getDetail("FETCH_BLOCK").getAverage();
        }
        return "";
    }

    @Override
    public String getPresentableValue(DiagnosticEntry<String> entry, int column) {
        return getValue(entry, column).toString();
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }
}
