package com.dci.intellij.dbn.language.sql.dialect.oracle;

import com.dci.intellij.dbn.language.common.ChameleonTokenType;
import com.dci.intellij.dbn.language.common.DBLanguageDialect;
import com.dci.intellij.dbn.language.common.DBLanguageDialectIdentifier;
import com.dci.intellij.dbn.language.common.DBLanguageSyntaxHighlighter;
import com.dci.intellij.dbn.language.common.element.TokenPairTemplate;
import com.dci.intellij.dbn.language.sql.dialect.SQLLanguageDialect;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class OracleSQLLanguageDialect extends SQLLanguageDialect {
    private static final TokenPairTemplate[] TOKEN_PAIR_TEMPLATES = new TokenPairTemplate[] {
            TokenPairTemplate.PARENTHESES,
            TokenPairTemplate.BEGIN_END};

    public OracleSQLLanguageDialect() {
        super(DBLanguageDialectIdentifier.ORACLE_SQL);
    }


    @Override
    protected Set<ChameleonTokenType> createChameleonTokenTypes() {
        Set<ChameleonTokenType> tokenTypes = new HashSet<ChameleonTokenType>();
        DBLanguageDialect plsql = DBLanguageDialect.getLanguageDialect(DBLanguageDialectIdentifier.ORACLE_PLSQL);
        tokenTypes.add(new ChameleonTokenType(this, plsql));
        return tokenTypes;
    }

    @Nullable
    @Override
    protected DBLanguageDialectIdentifier getChameleonDialectIdentifier() {
        return DBLanguageDialectIdentifier.ORACLE_PLSQL;
    }

    @Override
    public TokenPairTemplate[] getTokenPairTemplates() {
        return TOKEN_PAIR_TEMPLATES;
    }

    @Override
    protected DBLanguageSyntaxHighlighter createSyntaxHighlighter() {
        return new OracleSQLHighlighter(this);
}

    @Override
    protected OracleSQLParserDefinition createParserDefinition() {
        OracleSQLParser parser = new OracleSQLParser(this);
        return new OracleSQLParserDefinition(parser);
    }

}
