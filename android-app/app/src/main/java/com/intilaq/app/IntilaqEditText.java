package com.intilaq.app;

import android.content.Context;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.graphics.Typeface;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intilaq.lang.Keyword;

/**
 * مكوّن تحرير نصوص مخصص يقوم بتلوين الصياغة (Syntax Highlighting) لنصوص لغة "الانطلاق"
 * أثناء الكتابة مباشرة، دون الحاجة لمحرر خارجي.
 *
 * يلوّن:
 *  - الكلمات المحجوزة بالأزرق
 *  - النصوص بين علامتي تنصيص باللون الأخضر
 *  - الأرقام بالبرتقالي
 *  - صحيح/خاطئ/فراغ بالبنفسجي
 *  - التعليقات (# ...) بالرمادي
 *  - الأقواس { } بلون مميز وبخط عريض
 */
public class IntilaqEditText extends AppCompatEditText {

    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(?<![\\p{L}_])-?\\d+(\\.\\d+)?(?![\\p{L}_])");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("#.*");
    private static final Pattern BRACE_PATTERN = Pattern.compile("[{}]");
    private static final Pattern LITERAL_PATTERN = Pattern.compile("صحيح|خاطئ|فراغ");

    private static final Pattern KEYWORD_PATTERN = buildKeywordPattern();

    private boolean isHighlighting = false;

    public IntilaqEditText(Context context) {
        super(context);
        init();
    }

    public IntilaqEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IntilaqEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private static Pattern buildKeywordPattern() {
        StringBuilder sb = new StringBuilder();
        String[] words = Keyword.allTexts();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) sb.append('|');
            sb.append(Pattern.quote(words[i]));
        }
        // نتأكد أن الكلمة لا تكون جزءًا من كلمة أطول (حدود كلمة تدعم العربية)
        return Pattern.compile("(?<![\\p{L}_])(" + sb + ")(?![\\p{L}_])");
    }

    private void init() {
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (isHighlighting) return;
                isHighlighting = true;
                try {
                    highlight(editable);
                } finally {
                    isHighlighting = false;
                }
            }
        });
    }

    private void highlight(Editable editable) {
        String text = editable.toString();

        // إزالة كل التلوين السابق قبل إعادة تطبيقه
        clearSpans(editable);

        applyPattern(editable, text, COMMENT_PATTERN, R.color.syntax_comment, false);
        applyPattern(editable, text, STRING_PATTERN, R.color.syntax_string, false);
        applyPattern(editable, text, NUMBER_PATTERN, R.color.syntax_number, false);
        applyPattern(editable, text, LITERAL_PATTERN, R.color.syntax_boolean, true);
        applyPattern(editable, text, KEYWORD_PATTERN, R.color.syntax_keyword, true);
        applyPattern(editable, text, BRACE_PATTERN, R.color.syntax_brace, true);
    }

    private void clearSpans(Editable editable) {
        ForegroundColorSpan[] colorSpans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : colorSpans) {
            editable.removeSpan(span);
        }
        StyleSpan[] styleSpans = editable.getSpans(0, editable.length(), StyleSpan.class);
        for (StyleSpan span : styleSpans) {
            editable.removeSpan(span);
        }
    }

    private void applyPattern(Editable editable, String text, Pattern pattern, int colorRes, boolean bold) {
        Matcher matcher = pattern.matcher(text);
        int color = ContextCompat.getColor(getContext(), colorRes);
        while (matcher.find()) {
            editable.setSpan(
                    new ForegroundColorSpan(color),
                    matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (bold) {
                editable.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        matcher.start(), matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
