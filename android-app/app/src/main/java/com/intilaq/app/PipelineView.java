package com.intilaq.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.List;

/**
 * مكوّن مرئي مخصص يرسم خط الأنابيب (Pipeline) على الشاشة.
 *
 * يرسم ثلاث مراحل (Lexer → Parser → Emitter) متصلة بخطوط
 * مع مؤشرات حالة متحركة لكل مرحلة.
 */
public class PipelineView extends View {

    // ألوان المراحل
    private static final int COLOR_WAITING  = Color.parseColor("#DDE3ED");
    private static final int COLOR_RUNNING  = Color.parseColor("#1E88E5");
    private static final int COLOR_SUCCESS  = Color.parseColor("#43A047");
    private static final int COLOR_FAILED   = Color.parseColor("#EF5350");
    private static final int COLOR_BG       = Color.parseColor("#F0F4F8");
    private static final int COLOR_LINE     = Color.parseColor("#C5D0E0");

    private final Paint circlePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flowPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subTextPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<PipelineStage> stages;

    // متغيرات الحركة
    private float flowOffset = 0f;          // موضع تدفق الخط المتحرك
    private float pulseRadius = 0f;         // نبضة الدائرة النشطة
    private ValueAnimator flowAnimator;
    private ValueAnimator pulseAnimator;

    public PipelineView(Context context) {
        super(context);
        init();
    }

    public PipelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PipelineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        linePaint.setStrokeWidth(dpToPx(2));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        flowPaint.setStrokeWidth(dpToPx(2));
        flowPaint.setStyle(Paint.Style.STROKE);
        flowPaint.setStrokeCap(Paint.Cap.ROUND);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(2));

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dpToPx(6));
        glowPaint.setAlpha(40);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setAlpha(140);

        detailPaint.setTextAlign(Paint.Align.CENTER);
        detailPaint.setFakeBoldText(true);

        iconPaint.setTextAlign(Paint.Align.CENTER);
        iconPaint.setFakeBoldText(true);

        startAnimators();
    }

    /** تعيين مراحل خط الأنابيب وإعادة الرسم */
    public void setStages(List<PipelineStage> stages) {
        this.stages = stages;
        invalidate();
    }

    /** تحديث المراحل وإعادة الرسم (يُستدعى من Thread غير الرئيسي بأمان) */
    public void updateStages(List<PipelineStage> stages) {
        this.stages = stages;
        post(this::invalidate);
    }

    private void startAnimators() {
        flowAnimator = ValueAnimator.ofFloat(0f, 1f);
        flowAnimator.setDuration(800);
        flowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        flowAnimator.setInterpolator(new LinearInterpolator());
        flowAnimator.addUpdateListener(a -> {
            flowOffset = (float) a.getAnimatedValue();
            if (hasRunningStage()) invalidate();
        });
        flowAnimator.start();

        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(a -> {
            pulseRadius = (float) a.getAnimatedValue();
            if (hasRunningStage()) invalidate();
        });
        pulseAnimator.start();
    }

    private boolean hasRunningStage() {
        if (stages == null) return false;
        for (PipelineStage s : stages)
            if (s.getState() == PipelineStage.State.RUNNING) return true;
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (stages == null || stages.isEmpty()) return;

        float w = getWidth();
        float h = getHeight();
        float circleR   = dpToPx(24);
        float textSize  = dpToPx(10);
        float subSize   = dpToPx(8.5f);
        float detailSize= dpToPx(8f);
        float iconSize  = dpToPx(13f);

        int count = stages.size();
        float sectionW = w / count;

        // حساب مراكز الدوائر
        float[] cx = new float[count];
        float   cy = circleR + dpToPx(10);
        for (int i = 0; i < count; i++) {
            cx[i] = sectionW * i + sectionW / 2f;
        }

        // رسم الخطوط الرابطة بين الدوائر
        for (int i = 0; i < count - 1; i++) {
            float startX = cx[i]   + circleR + dpToPx(2);
            float endX   = cx[i+1] - circleR - dpToPx(2);
            float midY   = cy;

            PipelineStage stageLeft  = stages.get(i);
            PipelineStage stageRight = stages.get(i + 1);

            // خط أساسي
            linePaint.setColor(
                stageLeft.getState() == PipelineStage.State.SUCCESS ? COLOR_SUCCESS : COLOR_LINE
            );
            linePaint.setPathEffect(null);
            canvas.drawLine(startX, midY, endX, midY, linePaint);

            // خط تدفق متحرك عند تشغيل المرحلة التالية
            if (stageRight.getState() == PipelineStage.State.RUNNING) {
                float lineLen = endX - startX;
                float segLen  = lineLen * 0.5f;
                float offset  = flowOffset * (lineLen + segLen) - segLen;

                LinearGradient grad = new LinearGradient(
                    startX + offset, midY,
                    startX + offset + segLen, midY,
                    Color.TRANSPARENT, COLOR_RUNNING,
                    Shader.TileMode.CLAMP
                );
                flowPaint.setShader(grad);
                flowPaint.setColor(COLOR_RUNNING);
                canvas.drawLine(
                    Math.max(startX, startX + offset), midY,
                    Math.min(endX,   startX + offset + segLen), midY,
                    flowPaint
                );
                flowPaint.setShader(null);
            }

            // رأس السهم ◀
            float arrowX = endX + dpToPx(3);
            float arrowY = midY;
            float arrowSize = dpToPx(5);
            Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrowPaint.setColor(
                stageLeft.getState() == PipelineStage.State.SUCCESS ? COLOR_SUCCESS : COLOR_LINE
            );
            arrowPaint.setStyle(Paint.Style.FILL);
            android.graphics.Path arrowPath = new android.graphics.Path();
            arrowPath.moveTo(arrowX + arrowSize, arrowY);
            arrowPath.lineTo(arrowX, arrowY - arrowSize * 0.6f);
            arrowPath.lineTo(arrowX, arrowY + arrowSize * 0.6f);
            arrowPath.close();
            canvas.drawPath(arrowPath, arrowPaint);
        }

        // رسم كل دائرة ومعلوماتها
        for (int i = 0; i < count; i++) {
            PipelineStage stage = stages.get(i);
            PipelineStage.State state = stage.getState();

            int fillColor, borderColor, labelColor;
            switch (state) {
                case RUNNING:
                    fillColor   = Color.parseColor("#E3F2FD");
                    borderColor = COLOR_RUNNING;
                    labelColor  = COLOR_RUNNING;
                    break;
                case SUCCESS:
                    fillColor   = Color.parseColor("#E8F5E9");
                    borderColor = COLOR_SUCCESS;
                    labelColor  = COLOR_SUCCESS;
                    break;
                case FAILED:
                    fillColor   = Color.parseColor("#FFEBEE");
                    borderColor = COLOR_FAILED;
                    labelColor  = COLOR_FAILED;
                    break;
                default:
                    fillColor   = Color.parseColor("#F5F7FA");
                    borderColor = COLOR_WAITING;
                    labelColor  = Color.parseColor("#90A4AE");
            }

            // نبضة للمرحلة النشطة
            if (state == PipelineStage.State.RUNNING) {
                float maxPulse = circleR + dpToPx(9);
                float r = circleR + (maxPulse - circleR) * pulseRadius;
                glowPaint.setColor(COLOR_RUNNING);
                glowPaint.setAlpha((int)(50 * (1 - pulseRadius)));
                canvas.drawCircle(cx[i], cy, r, glowPaint);
            }

            // دائرة الخلفية
            circlePaint.setColor(fillColor);
            canvas.drawCircle(cx[i], cy, circleR, circlePaint);

            // حدود الدائرة
            borderPaint.setColor(borderColor);
            borderPaint.setStrokeWidth(state == PipelineStage.State.RUNNING ? dpToPx(2.5f) : dpToPx(2));
            canvas.drawCircle(cx[i], cy, circleR, borderPaint);

            // أيقونة نصية داخل الدائرة
            iconPaint.setTextSize(iconSize);
            String symbol;
            switch (state) {
                case SUCCESS: symbol = "✓"; iconPaint.setColor(COLOR_SUCCESS); break;
                case FAILED:  symbol = "✕"; iconPaint.setColor(COLOR_FAILED);  break;
                default:      symbol = stage.nameEn.substring(0, 1); iconPaint.setColor(borderColor); break;
            }
            float iconY = cy + dpToPx(4);
            canvas.drawText(symbol, cx[i], iconY, iconPaint);

            // اسم المرحلة بالعربي
            float labelY = cy + circleR + dpToPx(14);
            textPaint.setTextSize(textSize);
            textPaint.setColor(labelColor);
            canvas.drawText(stage.nameAr, cx[i], labelY, textPaint);

            // الاسم الإنجليزي
            subTextPaint.setTextSize(subSize);
            subTextPaint.setColor(Color.parseColor("#90A4AE"));
            canvas.drawText(stage.nameEn, cx[i], labelY + dpToPx(11), subTextPaint);

            // تفاصيل (عدد رموز / عقد / أحرف أو خطأ)
            if (!stage.getDetail().isEmpty()) {
                detailPaint.setTextSize(detailSize);
                detailPaint.setColor(
                    state == PipelineStage.State.FAILED ? COLOR_FAILED : COLOR_SUCCESS
                );
                String detail = stage.getDetail();
                if (detail.length() > 18) detail = detail.substring(0, 18) + "…";
                canvas.drawText(detail, cx[i], labelY + dpToPx(22), detailPaint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        // ارتفاع ثابت كافٍ لعرض الدوائر + النصوص
        int h = (int) (dpToPx(24 + 10 + 24 + 14 + 11 + 22 + 12)); // ~130dp
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (flowAnimator  != null) flowAnimator.cancel();
        if (pulseAnimator != null) pulseAnimator.cancel();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
