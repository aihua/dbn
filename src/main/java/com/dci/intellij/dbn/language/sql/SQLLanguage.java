package com.dci.intellij.dbn.language.sql;

import com.dci.intellij.dbn.code.sql.style.SQLCodeStyle;
import com.dci.intellij.dbn.code.sql.style.options.SQLCodeStyleSettings;
import com.dci.intellij.dbn.language.common.DBLanguage;
import com.dci.intellij.dbn.language.sql.dialect.SQLLanguageDialect;
import com.dci.intellij.dbn.language.sql.dialect.iso92.Iso92SQLLanguageDialect;
import com.dci.intellij.dbn.language.sql.dialect.mysql.MysqlSQLLanguageDialect;
import com.dci.intellij.dbn.language.sql.dialect.oracle.OracleSQLLanguageDialect;
import com.dci.intellij.dbn.language.sql.dialect.postgres.PostgresSQLLanguageDialect;
import com.dci.intellij.dbn.language.sql.dialect.sqlite.SqliteSQLLanguageDialect;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.Nullable;

public class SQLLanguage extends DBLanguage<SQLLanguageDialect> {
    public static final String ID = "DBN-SQL";
    public static final SQLLanguage INSTANCE = new SQLLanguage();

    @Override
    protected SQLLanguageDialect[] createLanguageDialects() {
        SQLLanguageDialect oracleSQLLanguageDialect = new OracleSQLLanguageDialect();
        SQLLanguageDialect mysqlSQLLanguageDialect = new MysqlSQLLanguageDialect();
        SQLLanguageDialect postgresSQLLanguageDialect = new PostgresSQLLanguageDialect();
        SQLLanguageDialect sqliteSQLLanguageDialect = new SqliteSQLLanguageDialect();
        SQLLanguageDialect iso92SQLLanguageDialect = new Iso92SQLLanguageDialect();
        return new SQLLanguageDialect[]{
                oracleSQLLanguageDialect,
                mysqlSQLLanguageDialect,
                postgresSQLLanguageDialect,
                sqliteSQLLanguageDialect,
                iso92SQLLanguageDialect};
    }

    @Override
    public SQLLanguageDialect getMainLanguageDialect() {
        return getAvailableLanguageDialects()[0];
    }

    @Override
    protected IFileElementType createFileElementType() {
        return new SQLFileElementType(this);
    }

    private SQLLanguage() {
        super(ID, "text/sql");
    }


    @Override
    public SQLCodeStyleSettings codeStyleSettings(@Nullable Project project) {
        return SQLCodeStyle.settings(project);
    }
}
