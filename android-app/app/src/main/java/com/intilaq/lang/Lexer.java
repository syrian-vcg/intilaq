package com.intilaq.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * المحلل المعجمي (Lexer): يحوّل نص لغة "الانطلاق" الخام إلى قائمة من الرموز (Tokens).
 * يدعم الحروف العربية في الأسماء والكلمات المحجوزة، والأرقام، والنصوص المقتبسة،
 * والأقواس { }، وعلامة المساواة =، والتعليقات التي تبدأ بـ #.
 */
public class Lexer {

    private final String source;
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    public Lexer(String source) {
        this.source = source == null ? "" : source;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipWhitespaceAndComments(tokens);
            if (isAtEnd()) {
                tokens.add(new Token(TokenType.EOF, "", line, column));
                break;
            }

            char c = peek();

            if (c == '{') {
                tokens.add(new Token(TokenType.LBRACE, "{", line, column));
                advance();
                continue;
            }
            if (c == '}') {
                tokens.add(new Token(TokenType.RBRACE, "}", line, column));
                advance();
                continue;
            }
            if (c == '=') {
                tokens.add(new Token(TokenType.EQUALS, "=", line, column));
                advance();
                continue;
            }
            if (c == '"') {
                tokens.add(readString());
                continue;
            }
            if (isDigit(c) || (c == '-' && isDigit(peekAt(1)))) {
                tokens.add(readNumber());
                continue;
            }
            if (isIdentifierStart(c)) {
                tokens.add(readWord());
                continue;
            }

            throw new IntilaqException("رمز غير معروف: '" + c + "'", line, column);
        }
        return tokens;
    }

    private void skipWhitespaceAndComments(List<Token> tokens) {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
            } else if (c == '\n') {
                advance();
                line++;
                column = 1;
            } else if (c == '#') {
                // تعليق إلى آخر السطر
                StringBuilder sb = new StringBuilder();
                while (!isAtEnd() && peek() != '\n') {
                    sb.append(peek());
                    advance();
                }
                tokens.add(new Token(TokenType.COMMENT, sb.toString(), line, column));
            } else {
                break;
            }
        }
    }

    private Token readString() {
        int startLine = line, startCol = column;
        advance(); // تجاوز علامة التنصيص الافتتاحية
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            char c = peek();
            if (c == '\\' && peekAt(1) == '"') {
                sb.append('"');
                advance();
                advance();
                continue;
            }
            if (c == '\n') {
                throw new IntilaqException("نص غير منتهٍ (سطر جديد قبل إغلاق علامة التنصيص)", line, column);
            }
            sb.append(c);
            advance();
        }
        if (isAtEnd()) {
            throw new IntilaqException("نص غير منتهٍ، ينقصه علامة تنصيص الإغلاق \"", startLine, startCol);
        }
        advance(); // تجاوز علامة التنصيص الختامية
        return new Token(TokenType.STRING, sb.toString(), startLine, startCol);
    }

    private Token readNumber() {
        int startLine = line, startCol = column;
        StringBuilder sb = new StringBuilder();
        if (peek() == '-') {
            sb.append(peek());
            advance();
        }
        while (!isAtEnd() && isDigit(peek())) {
            sb.append(peek());
            advance();
        }
        if (!isAtEnd() && peek() == '.' && isDigit(peekAt(1))) {
            sb.append(peek());
            advance();
            while (!isAtEnd() && isDigit(peek())) {
                sb.append(peek());
                advance();
            }
        }
        return new Token(TokenType.NUMBER, sb.toString(), startLine, startCol);
    }

    private Token readWord() {
        int startLine = line, startCol = column;
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && isIdentifierPart(peek())) {
            sb.append(peek());
            advance();
        }
        String word = sb.toString();

        if (word.equals("صحيح") || word.equals("خاطئ")) {
            return new Token(TokenType.BOOLEAN, word, startLine, startCol);
        }
        if (word.equals("فراغ")) {
            return new Token(TokenType.NULL, word, startLine, startCol);
        }
        if (Keyword.isKeyword(word)) {
            return new Token(TokenType.KEYWORD, word, startLine, startCol);
        }
        return new Token(TokenType.IDENTIFIER, word, startLine, startCol);
    }

    // ---------- أدوات مساعدة ----------

    private boolean isAtEnd() {
        return pos >= source.length();
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(pos);
    }

    private char peekAt(int offset) {
        int idx = pos + offset;
        return idx >= source.length() ? '\0' : source.charAt(idx);
    }

    private void advance() {
        pos++;
        column++;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** يسمح بالحروف العربية (يونيكود) واللاتينية والشرطة السفلية كبداية اسم. */
    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_';
    }
}
