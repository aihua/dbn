package com.dci.intellij.dbn.language.sql.dialect.mysql;

import com.dci.intellij.dbn.language.sql.SQLParser;
import com.dci.intellij.dbn.language.sql.dialect.SQLLanguageDialect;

class MysqlSQLParser extends SQLParser {
    MysqlSQLParser(SQLLanguageDialect languageDialect) {
        super(languageDialect, "mysql_sql_parser_tokens.xml", "mysql_sql_parser_elements.xml", "sql_block");
    }
}