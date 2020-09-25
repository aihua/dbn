package com.dci.intellij.dbn.language.common;

public class QuotePair {
    private static String POSSIBLE_BEGIN_QUOTES = "";
    private static String POSSIBLE_END_QUOTES = "";

    public static final QuotePair DEFAULT_IDENTIFIER_QUOTE_PAIR = new QuotePair('"', '"');
    private final char beginChar;
    private final char endChar;

    public QuotePair(char beginChar, char endChar) {
        this.beginChar = beginChar;
        this.endChar = endChar;
        POSSIBLE_BEGIN_QUOTES = POSSIBLE_BEGIN_QUOTES.indexOf(beginChar) == -1 ? POSSIBLE_BEGIN_QUOTES + beginChar : POSSIBLE_BEGIN_QUOTES;
        POSSIBLE_END_QUOTES = POSSIBLE_END_QUOTES.indexOf(endChar) == -1 ? POSSIBLE_END_QUOTES + endChar : POSSIBLE_END_QUOTES;
    }

    public static boolean isPossibleBeginQuote(char chr) {
        return POSSIBLE_BEGIN_QUOTES.indexOf(chr) > -1;
    }

    public static boolean isPossibleEndQuote(char chr) {
        return POSSIBLE_END_QUOTES.indexOf(chr) > -1;
    }

    public char beginChar() {
        return beginChar;
    }

    public char endChar() {
        return endChar;
    }

    public boolean isQuoted(CharSequence charSequence) {
        char firstChar = charSequence.charAt(0);
        char lastChar = charSequence.charAt(charSequence.length() - 1);

        return firstChar == beginChar && lastChar == endChar;
    }

    public String replaceQuotes(String string, char character) {
        return string.replace(beginChar, character).replace(endChar, character);
    }

    public String quote(String identifier) {
        return beginChar + identifier + endChar;
    }
}
