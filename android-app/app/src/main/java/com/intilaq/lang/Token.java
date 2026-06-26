package com.intilaq.lang;

/** يمثل وحدة معجمية واحدة (Token) ناتجة عن تحليل النص. */
public class Token {
    public final TokenType type;
    public final String text;
    public final int line;
    public final int column;

    public Token(TokenType type, String text, int line, int column) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return "Token(" + type + ", '" + text + "', سطر=" + line + ", عمود=" + column + ")";
    }
}
