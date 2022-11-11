package com.dci.intellij.dbn.language.sql.dialect.postgres;

import com.dci.intellij.dbn.language.common.ChameleonTokenType;
import com.dci.intellij.dbn.language.common.DBLanguageDialectIdentifier;
import com.dci.intellij.dbn.language.common.DBLanguageSyntaxHighlighter;
import com.dci.intellij.dbn.language.sql.dialect.SQLLanguageDialect;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class PostgresSQLLanguageDialect extends SQLLanguageDialect {
    public PostgresSQLLanguageDialect() {
        super(DBLanguageDialectIdentifier.POSTGRES_SQL);
    }

    @Override
    protected Set<ChameleonTokenType> createChameleonTokenTypes() {
        return null;
    }

    @Override
    protected DBLanguageSyntaxHighlighter createSyntaxHighlighter() {
        return new PostgresSQLHighlighter(this);
}

    @Override
    protected PostgresSQLParserDefinition createParserDefinition() {
        PostgresSQLParser parser = new PostgresSQLParser(this);
        return new PostgresSQLParserDefinition(parser);
    }

    @Nullable
    @Override
    protected DBLanguageDialectIdentifier getChameleonDialectIdentifier() {
        return DBLanguageDialectIdentifier.POSTGRES_PSQL;
    }
}