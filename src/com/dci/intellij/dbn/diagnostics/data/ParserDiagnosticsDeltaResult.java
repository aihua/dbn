package com.dci.intellij.dbn.diagnostics.data;


import com.dci.intellij.dbn.common.list.FilteredList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dci.intellij.dbn.diagnostics.data.ParserDiagnosticsUtil.computeStateTransition;


@Getter
public class ParserDiagnosticsDeltaResult {
    private ParserDiagnosticsFilter filter = ParserDiagnosticsFilter.EMPTY;
    private final List<ParserDiagnosticsEntry> entries = FilteredList.stateful(filter);

    private final ParserDiagnosticsResult previous;
    private final ParserDiagnosticsResult current;

    public ParserDiagnosticsDeltaResult(@Nullable ParserDiagnosticsResult previous, @NotNull ParserDiagnosticsResult current) {
        this.previous = previous;
        this.current = current;
        for (String s : current.getEntries().keySet()) {
            IssueCounter newIssues = current.getIssues(s);
            IssueCounter oldIssues = previous == null ? newIssues : previous.getIssues(s);
            addEntry(s, oldIssues, newIssues);
        }

        if (previous != null) {
            for (String file : previous.getFiles()) {
                if (!current.isPresent(file)) {
                    addEntry(file, previous.getIssues(file), null);
                }
            }
        }
    }

    public void setFilter(ParserDiagnosticsFilter filter) {
        this.filter = filter;
    }

    private void addEntry(String file, IssueCounter oldIssues, IssueCounter newIssues) {
        ParserDiagnosticsEntry diagnosticsCapture = new ParserDiagnosticsEntry(file, oldIssues, newIssues);
        entries.add(diagnosticsCapture);
    }

    public String getName() {
        if (previous == null) {
            return current.getName();
        } else {
            return "Result " + current.getName() + " compared to result " + previous.getName();
        }
    }

    public StateTransition getFilter() {
        IssueCounter oldIssues = previous == null ? current.getIssues() : previous.getIssues();
        IssueCounter newIssues = current.getIssues();

        return computeStateTransition(oldIssues, newIssues);
    }
}
