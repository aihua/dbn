package com.dci.intellij.dbn.language.sql.dialect.sqlite;

import com.dci.intellij.dbn.language.sql.SQLParserDefinition;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;


public class SqliteSQLParserDefinition extends SQLParserDefinition {

    public SqliteSQLParserDefinition(SqliteSQLParser parser) {
        super(parser);
    }

    @Override
    @NotNull
    public Lexer createLexer(Project project) {
        return new FlexAdapter(new SqliteSQLParserFlexLexer(getTokenTypes()));
    }

}