package com.intilaq.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * المحلل النحوي (Parser): يأخذ قائمة الرموز (Tokens) الناتجة عن Lexer
 * ويبني منها شجرة قيم (IntilaqValue) تمثّل البيانات الموصوفة في نص "الانطلاق".
 *
 * قواعد الصياغة المدعومة:
 *   كائن { مفتاح = قيمة  ...  حاجز }
 *   مجموعة { حلقة { بند = قيمة } ... حاجز }
 *   قيمة: نص "..." | رقم | صحيح | خاطئ | فراغ | كائن{...} | مجموعة{...}
 */
public class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        // التعليقات لا تهم بنية البيانات، نُرشّحها مسبقاً
        this.tokens = new ArrayList<>();
        for (Token t : tokens) {
            if (t.type != TokenType.COMMENT) {
                this.tokens.add(t);
            }
        }
    }

    /** نقطة الدخول: يحلل نص الانطلاق كاملاً ويرجّع القيمة الجذرية. */
    public IntilaqValue parse() {
        IntilaqValue result = parseValue();
        expect(TokenType.EOF, "نهاية الملف");
        return result;
    }

    private IntilaqValue parseValue() {
        Token t = peek();

        if (t.type == TokenType.KEYWORD && t.text.equals(Keyword.KAEN.getText())) {
            return parseObject();
        }
        if (t.type == TokenType.KEYWORD && t.text.equals(Keyword.MAJMOUAA.getText())) {
            return parseArray();
        }
        if (t.type == TokenType.STRING) {
            advance();
            return IntilaqValue.ofString(t.text);
        }
        if (t.type == TokenType.NUMBER) {
            advance();
            return IntilaqValue.ofNumber(Double.parseDouble(t.text));
        }
        if (t.type == TokenType.BOOLEAN) {
            advance();
            return IntilaqValue.ofBoolean(t.text.equals("صحيح"));
        }
        if (t.type == TokenType.NULL) {
            advance();
            return IntilaqValue.ofNull();
        }

        throw new IntilaqException(
                "قيمة غير متوقعة: '" + t.text + "'، كان متوقَّعاً: كائن، مجموعة، نص، رقم، صحيح، خاطئ، أو فراغ",
                t.line, t.column);
    }

    /** كائن { مفتاح = قيمة ... حاجز } */
    private IntilaqValue parseObject() {
        expectKeyword(Keyword.KAEN);
        expect(TokenType.LBRACE, "{");

        IntilaqValue obj = IntilaqValue.ofObject();

        while (!isAtHajezClose()) {
            Token keyToken = peek();
            String key;
            if (keyToken.type == TokenType.KEYWORD || keyToken.type == TokenType.IDENTIFIER) {
                key = keyToken.text;
                advance();
            } else {
                throw new IntilaqException(
                        "توقعت اسم حقل (مفتاح) لكن وجدت '" + keyToken.text + "'",
                        keyToken.line, keyToken.column);
            }

            expect(TokenType.EQUALS, "=");
            IntilaqValue value = parseValue();
            obj.put(key, value);
        }

        consumeHajezClose();
        return obj;
    }

    /** مجموعة { حلقة { بند = قيمة } ... حاجز } */
    private IntilaqValue parseArray() {
        expectKeyword(Keyword.MAJMOUAA);
        expect(TokenType.LBRACE, "{");

        IntilaqValue arr = IntilaqValue.ofArray();

        while (!isAtHajezClose()) {
            expectKeyword(Keyword.HALQA);
            expect(TokenType.LBRACE, "{");

            // الصيغة المعتادة: بند = قيمة
            Token fieldToken = peek();
            if (fieldToken.type == TokenType.KEYWORD || fieldToken.type == TokenType.IDENTIFIER) {
                advance();
                expect(TokenType.EQUALS, "=");
            }
            IntilaqValue itemValue = parseValue();
            arr.add(itemValue);

            expect(TokenType.RBRACE, "}");
        }

        consumeHajezClose();
        return arr;
    }

    /** يتحقق إن كان الرمز التالي هو بداية "حاجز }" المغلقة للكتلة الحالية. */
    private boolean isAtHajezClose() {
        Token t = peek();
        return t.type == TokenType.KEYWORD && t.text.equals(Keyword.HAJEZ.getText());
    }

    private void consumeHajezClose() {
        expectKeyword(Keyword.HAJEZ);
        expect(TokenType.RBRACE, "}");
    }

    // ---------- أدوات مساعدة ----------

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        Token t = tokens.get(pos);
        if (pos < tokens.size() - 1) pos++;
        return t;
    }

    private Token expect(TokenType type, String expectedDesc) {
        Token t = peek();
        if (t.type != type) {
            throw new IntilaqException(
                    "متوقَّع '" + expectedDesc + "' لكن وُجد '" + t.text + "'",
                    t.line, t.column);
        }
        return advance();
    }

    private void expectKeyword(Keyword keyword) {
        Token t = peek();
        if (t.type != TokenType.KEYWORD || !t.text.equals(keyword.getText())) {
            throw new IntilaqException(
                    "متوقَّع الكلمة المحجوزة '" + keyword.getText() + "' لكن وُجد '" + t.text + "'",
                    t.line, t.column);
        }
        advance();
    }
}
