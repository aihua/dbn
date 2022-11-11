package com.dci.intellij.dbn.language.psql.dialect.sqlite;

import com.dci.intellij.dbn.language.psql.dialect.PSQLLanguageDialect;
import com.dci.intellij.dbn.language.sql.dialect.sqlite.SqliteSQLHighlighter;

public class SqlitePSQLHighlighter extends SqliteSQLHighlighter {
    SqlitePSQLHighlighter(PSQLLanguageDialect languageDialect) {
        super(languageDialect);
    }

    @Override
    protected Class getResourceLookupClass() {
        return SqliteSQLHighlighter.class;
    }
}
