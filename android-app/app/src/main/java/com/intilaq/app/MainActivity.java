package com.intilaq.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.intilaq.lang.Intilaq;
import com.intilaq.lang.IntilaqValue;

/**
 * الشاشة الرئيسية لتطبيق "الانطلاق":
 * تحتوي على محرر نص بتلوين الصياغة، وزر تشغيل يحلل النص ويعرض النتيجة
 * بصيغة JSON قياسية، مع عرض رسائل خطأ واضحة بالعربية عند فشل التحليل.
 */
public class MainActivity extends AppCompatActivity {

    private IntilaqEditText editor;
    private TextView resultStatus;
    private TextView resultOutput;
    private MaterialButton btnCopy;

    private static final String SAMPLE = ""
            + "كائن {\n"
            + "    معرف = \"1001\"\n"
            + "    اسم = \"أحمد\"\n"
            + "    خانة = 25\n"
            + "    نجح = صحيح\n"
            + "    تسمية = فراغ\n"
            + "\n"
            + "    # مجموعة من القيم النصية\n"
            + "    هوايات = مجموعة {\n"
            + "        حلقة { بند = \"قراءة\" }\n"
            + "        حلقة { بند = \"برمجة\" }\n"
            + "    حاجز\n"
            + "    }\n"
            + "\n"
            + "    عنوان = كائن {\n"
            + "        مدينة = \"الرياض\"\n"
            + "        رمز = 11564\n"
            + "    حاجز\n"
            + "    }\n"
            + "حاجز\n"
            + "}\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editor = findViewById(R.id.editor);
        resultStatus = findViewById(R.id.resultStatus);
        resultOutput = findViewById(R.id.resultOutput);
        btnCopy = findViewById(R.id.btnCopy);

        MaterialButton btnRun = findViewById(R.id.btnRun);
        MaterialButton btnClear = findViewById(R.id.btnClear);
        MaterialButton btnSample = findViewById(R.id.btnSample);

        btnRun.setOnClickListener(v -> runAnalysis());
        btnClear.setOnClickListener(v -> {
            editor.setText("");
            resetResult();
        });
        btnSample.setOnClickListener(v -> editor.setText(SAMPLE));
        btnCopy.setOnClickListener(v -> copyResultToClipboard());

        // عرض مثال جاهز عند أول فتح للتطبيق لتسهيل الفهم
        editor.setText(SAMPLE);
    }

    private void runAnalysis() {
        String source = editor.getText() == null ? "" : editor.getText().toString();

        if (source.trim().isEmpty()) {
            showError("الرجاء كتابة نص لغة الانطلاق أولاً قبل التشغيل.");
            return;
        }

        Intilaq.Result result = Intilaq.tryParse(source);

        if (result.success) {
            IntilaqValue value = result.value;
            String json = value.toJson();
            showSuccess(prettyJson(json));
        } else {
            showError(result.errorMessage);
        }
    }

    /** ينسّق نص JSON المُنتج بمسافات بادئة لسهولة القراءة (تنسيق بسيط بدون مكتبات خارجية). */
    private String prettyJson(String compactJson) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        for (int i = 0; i < compactJson.length(); i++) {
            char c = compactJson.charAt(i);

            if (c == '"' && (i == 0 || compactJson.charAt(i - 1) != '\\')) {
                inString = !inString;
                sb.append(c);
                continue;
            }
            if (inString) {
                sb.append(c);
                continue;
            }

            switch (c) {
                case '{':
                case '[':
                    sb.append(c).append('\n');
                    indent++;
                    appendIndent(sb, indent);
                    break;
                case '}':
                case ']':
                    sb.append('\n');
                    indent--;
                    appendIndent(sb, indent);
                    sb.append(c);
                    break;
                case ',':
                    sb.append(c).append('\n');
                    appendIndent(sb, indent);
                    break;
                case ':':
                    sb.append(c).append(' ');
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) sb.append("  ");
    }

    private void showSuccess(String json) {
        resultStatus.setText(getString(R.string.success_message));
        resultStatus.setTextColor(getColorCompat(R.color.intilaq_blue_dark));
        resultOutput.setText(json);
        resultOutput.setTextColor(getColorCompat(R.color.black));
        btnCopy.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        resultStatus.setText(getString(R.string.error_message));
        resultStatus.setTextColor(getColorCompat(R.color.syntax_error));
        resultOutput.setText(message);
        resultOutput.setTextColor(getColorCompat(R.color.syntax_error));
        btnCopy.setVisibility(View.GONE);
    }

    private void resetResult() {
        resultStatus.setText(getString(R.string.result_title));
        resultStatus.setTextColor(getColorCompat(R.color.intilaq_blue_dark));
        resultOutput.setText("");
        btnCopy.setVisibility(View.GONE);
    }

    private int getColorCompat(int colorRes) {
        return androidx.core.content.ContextCompat.getColor(this, colorRes);
    }

    private void copyResultToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("نتيجة الانطلاق", resultOutput.getText().toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
    }
}
