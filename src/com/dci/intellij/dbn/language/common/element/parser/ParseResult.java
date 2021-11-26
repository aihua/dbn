package com.dci.intellij.dbn.language.common.element.parser;

import lombok.Getter;

@Getter
public class ParseResult{
    private static final ParseResult NO_MATCH = new ParseResult(ParseResultType.NO_MATCH, 0);

    private final ParseResultType type;
    private final int matchedTokens;

    private ParseResult(ParseResultType type, int matchedTokens) {
        this.type = type;
        this.matchedTokens = matchedTokens;
    }

    public static ParseResult createFullMatchResult(int matchedTokens) {
        return new ParseResult(ParseResultType.FULL_MATCH, matchedTokens);
    }

    public static ParseResult createPartialMatchResult(int matchedTokens) {
        return new ParseResult(ParseResultType.PARTIAL_MATCH, matchedTokens);
    }

    public static ParseResult createNoMatchResult() {
        return NO_MATCH;
    }

    public boolean isNoMatch() {
        return type == ParseResultType.NO_MATCH;
    }

    public boolean isFullMatch() {
        return type == ParseResultType.FULL_MATCH;
    }

    public boolean isPartialMatch() {
        return type == ParseResultType.PARTIAL_MATCH;
    }


    public boolean isMatch() {
        return isFullMatch() || isPartialMatch();
    }

    @Override
    public String toString() {
        return type.toString();
    }

    public boolean isBetterThan(ParseResult result) {
        return type.getScore() >= result.type.getScore() && matchedTokens > result.matchedTokens;
    }
}
