package com.dci.intellij.dbn.language.sql.dialect.mysql;

import com.dci.intellij.dbn.language.sql.SQLSyntaxHighlighter;
import com.dci.intellij.dbn.language.sql.dialect.SQLLanguageDialect;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.FlexLexer;
import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.Lexer;
import org.jetbrains.annotations.NotNull;

public class MysqlSQLHighlighter extends SQLSyntaxHighlighter {
    MysqlSQLHighlighter(SQLLanguageDialect languageDialect) {
        super(languageDialect, "mysql_sql_highlighter_tokens.xml");
    }

    @Override
    @NotNull
    protected Lexer createLexer() {
        FlexLexer flexLexer = new MysqlSQLHighlighterFlexLexer(getTokenTypes());
        return new LayeredLexer(new FlexAdapter(flexLexer));
    }
}