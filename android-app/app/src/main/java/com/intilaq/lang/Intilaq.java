package com.intilaq.lang;

import java.util.List;

/**
 * نقطة الدخول الرئيسية لاستخدام محرك لغة "الانطلاق".
 *
 * مثال استخدام:
 * <pre>
 *     IntilaqValue data = Intilaq.parse(sourceText);
 *     String json = data.toJson();
 * </pre>
 */
public final class Intilaq {

    private Intilaq() {}

    /** يحلل نص لغة الانطلاق ويرجّع القيمة الجذرية الناتجة. يرمي IntilaqException عند وجود خطأ. */
    public static IntilaqValue parse(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    /** يحلل النص ويرجّع نتيجة (نجاح/فشل) مع رسالة خطأ واضحة بدل رمي استثناء، مناسب لواجهات المستخدم. */
    public static Result tryParse(String source) {
        try {
            IntilaqValue value = parse(source);
            return Result.success(value);
        } catch (IntilaqException e) {
            return Result.failure(e.getMessage());
        } catch (Exception e) {
            return Result.failure("خطأ غير متوقع: " + e.getMessage());
        }
    }

    /** يحوّل نص الانطلاق مباشرة إلى JSON. */
    public static String toJson(String source) {
        return parse(source).toJson();
    }

    public static final class Result {
        public final boolean success;
        public final IntilaqValue value;
        public final String errorMessage;

        private Result(boolean success, IntilaqValue value, String errorMessage) {
            this.success = success;
            this.value = value;
            this.errorMessage = errorMessage;
        }

        static Result success(IntilaqValue value) {
            return new Result(true, value, null);
        }

        static Result failure(String message) {
            return new Result(false, null, message);
        }
    }
}
