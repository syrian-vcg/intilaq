package com.intilaq.app;

/**
 * تمثّل مرحلة واحدة في خط الأنابيب (Pipeline) لمعالجة نص "الانطلاق".
 * تُستخدم لعرض حالة كل مرحلة (انتظار / تشغيل / نجاح / فشل) في الواجهة.
 */
public class PipelineStage {

    public enum State { WAITING, RUNNING, SUCCESS, FAILED }

    public final String nameAr;
    public final String nameEn;
    public final String description;
    public final int iconRes;

    private State state = State.WAITING;
    private String detail = "";

    public PipelineStage(String nameAr, String nameEn, String description, int iconRes) {
        this.nameAr      = nameAr;
        this.nameEn      = nameEn;
        this.description = description;
        this.iconRes     = iconRes;
    }

    public State getState()  { return state; }
    public String getDetail(){ return detail; }

    public void reset()                       { state = State.WAITING; detail = ""; }
    public void running()                     { state = State.RUNNING;  detail = ""; }
    public void success(String detail)        { state = State.SUCCESS;  this.detail = detail; }
    public void failed(String errorMsg)       { state = State.FAILED;   this.detail = errorMsg; }
}
