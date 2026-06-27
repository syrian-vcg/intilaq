package com.intilaq.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.intilaq.lang.Intilaq;
import com.intilaq.lang.IntilaqValue;
import com.intilaq.lang.Lexer;
import com.intilaq.lang.Parser;
import com.intilaq.lang.Token;
import com.intilaq.lang.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * الشاشة الرئيسية لتطبيق "الانطلاق".
 *
 * تحتوي على:
 *  - محرر نص بتلوين صياغة حي
 *  - خط أنابيب مرئي (Pipeline) يوضح مراحل المعالجة: Lexer → Parser → Emitter
 *  - عرض النتيجة بصيغة JSON قياسية أو رسالة خطأ واضحة
 */
public class MainActivity extends AppCompatActivity {

    // ============ المتغيرات ============
    private IntilaqEditText editor;
    private PipelineView    pipelineView;
    private TextView        resultStatus;
    private TextView        resultOutput;
    private MaterialButton  btnCopy;
    private MaterialButton  btnRun;

    private List<PipelineStage> stages;
    private final Handler        uiHandler  = new Handler(Looper.getMainLooper());
    private final ExecutorService executor  = Executors.newSingleThreadExecutor();

    // ============ نص المثال ============
    private static final String SAMPLE =
            "كائن {\n"
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

    // ============ دورة الحياة ============
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editor       = findViewById(R.id.editor);
        pipelineView = findViewById(R.id.pipelineView);
        resultStatus = findViewById(R.id.resultStatus);
        resultOutput = findViewById(R.id.resultOutput);
        btnCopy      = findViewById(R.id.btnCopy);
        btnRun       = findViewById(R.id.btnRun);

        MaterialButton btnClear  = findViewById(R.id.btnClear);
        MaterialButton btnSample = findViewById(R.id.btnSample);

        // إعداد مراحل Pipeline
        stages = Arrays.asList(
            new PipelineStage("المعجمي",   "Lexer",   "تحويل النص إلى رموز",    0),
            new PipelineStage("النحوي",    "Parser",  "بناء شجرة البيانات",      0),
            new PipelineStage("المُصدِّر", "Emitter", "توليد JSON قياسي",        0)
        );
        pipelineView.setStages(stages);

        // الأزرار
        btnRun.setOnClickListener(v -> runPipeline());
        btnClear.setOnClickListener(v -> {
            editor.setText("");
            resetAll();
        });
        btnSample.setOnClickListener(v -> editor.setText(SAMPLE));
        btnCopy.setOnClickListener(v -> copyResultToClipboard());

        editor.setText(SAMPLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // ============ تشغيل Pipeline ============
    private void runPipeline() {
        String source = editor.getText() == null ? "" : editor.getText().toString();
        if (source.trim().isEmpty()) {
            showError("الرجاء كتابة نص لغة الانطلاق أولاً.");
            return;
        }

        // تعطيل الزر أثناء التشغيل
        btnRun.setEnabled(false);
        btnRun.setText(R.string.btn_running);

        // إعادة ضبط المراحل
        resetPipelineStages();
        resetResult();

        executor.execute(() -> executePipeline(source));
    }

    private void executePipeline(String source) {
        // ── المرحلة 1: Lexer ──
        setStageRunning(0);
        sleep(150);

        List<Token> tokens;
        try {
            Lexer lexer = new Lexer(source);
            tokens = lexer.tokenize();
            long tokenCount = tokens.stream()
                    .filter(t -> t.type != TokenType.COMMENT && t.type != TokenType.EOF)
                    .count();
            setStageSuccess(0, tokenCount + " رمز");
        } catch (Exception e) {
            setStageFailed(0, e.getMessage());
            finishPipeline(false, e.getMessage());
            return;
        }

        // ── المرحلة 2: Parser ──
        setStageRunning(1);
        sleep(350);

        IntilaqValue ast;
        try {
            Parser parser = new Parser(tokens);
            ast = parser.parse();
            int nodeCount = countNodes(ast);
            setStageSuccess(1, nodeCount + " عقدة");
        } catch (Exception e) {
            setStageFailed(1, e.getMessage());
            finishPipeline(false, e.getMessage());
            return;
        }

        // ── المرحلة 3: Emitter ──
        setStageRunning(2);
        sleep(300);

        try {
            String json = prettyJson(ast.toJson());
            setStageSuccess(2, json.length() + " حرف");
            finishPipeline(true, json);
        } catch (Exception e) {
            setStageFailed(2, e.getMessage());
            finishPipeline(false, e.getMessage());
        }
    }

    // ============ تحديثات Pipeline (Thread-safe) ============
    private void setStageRunning(int idx) {
        stages.get(idx).running();
        pipelineView.updateStages(stages);
    }

    private void setStageSuccess(int idx, String detail) {
        stages.get(idx).success(detail);
        pipelineView.updateStages(stages);
    }

    private void setStageFailed(int idx, String msg) {
        String brief = msg != null && msg.length() > 30 ? msg.substring(0, 30) + "…" : msg;
        stages.get(idx).failed(brief != null ? brief : "خطأ");
        pipelineView.updateStages(stages);
    }

    private void finishPipeline(boolean ok, String payload) {
        sleep(100);
        uiHandler.post(() -> {
            btnRun.setEnabled(true);
            btnRun.setText(R.string.btn_run);
            if (ok) showSuccess(payload);
            else    showError(payload);
        });
    }

    // ============ عرض النتائج ============
    private void showSuccess(String json) {
        resultStatus.setText(getString(R.string.success_message));
        resultStatus.setTextColor(getColorCompat(R.color.intilaq_blue_dark));
        resultStatus.setVisibility(View.VISIBLE);
        resultOutput.setText(json);
        resultOutput.setTextColor(getColorCompat(R.color.black));
        resultOutput.setVisibility(View.VISIBLE);
        btnCopy.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        resultStatus.setText(getString(R.string.error_message));
        resultStatus.setTextColor(getColorCompat(R.color.syntax_error));
        resultStatus.setVisibility(View.VISIBLE);
        resultOutput.setText(message);
        resultOutput.setTextColor(getColorCompat(R.color.syntax_error));
        resultOutput.setVisibility(View.VISIBLE);
        btnCopy.setVisibility(View.GONE);
    }

    private void resetAll() {
        resetPipelineStages();
        resetResult();
    }

    private void resetPipelineStages() {
        for (PipelineStage s : stages) s.reset();
        pipelineView.updateStages(stages);
    }

    private void resetResult() {
        uiHandler.post(() -> {
            resultStatus.setVisibility(View.GONE);
            resultOutput.setVisibility(View.GONE);
            btnCopy.setVisibility(View.GONE);
        });
    }

    // ============ مساعدات ============

    /** ينسّق JSON مضغوطاً بمسافات بادئة لسهولة القراءة. */
    private String prettyJson(String compact) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inStr = false;
        for (int i = 0; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (c == '"' && (i == 0 || compact.charAt(i - 1) != '\\')) {
                inStr = !inStr;
                sb.append(c);
                continue;
            }
            if (inStr) { sb.append(c); continue; }
            switch (c) {
                case '{': case '[':
                    sb.append(c).append('\n');
                    indent++;
                    appendIndent(sb, indent);
                    break;
                case '}': case ']':
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

    /** يعدّ العقد في الشجرة (AST) لعرضها كإحصائية. */
    private int countNodes(IntilaqValue val) {
        if (val == null) return 0;
        int count = 1;
        if (val.isObject()) {
            for (IntilaqValue v : val.asObjectMap().values()) count += countNodes(v);
        } else if (val.isArray()) {
            for (IntilaqValue v : val.asList()) count += countNodes(v);
        }
        return count;
    }

    private void copyResultToClipboard() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(
                "نتيجة الانطلاق",
                resultOutput.getText().toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
    }

    private int getColorCompat(int colorRes) {
        return androidx.core.content.ContextCompat.getColor(this, colorRes);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
