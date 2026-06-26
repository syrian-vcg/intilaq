package com.intilaq.lang;

/** استثناء يُرمى عند وجود خطأ في تحليل أو تنفيذ نص لغة "الانطلاق". */
public class IntilaqException extends RuntimeException {
    private final int line;
    private final int column;

    public IntilaqException(String message, int line, int column) {
        super(message + " (السطر " + line + "، العمود " + column + ")");
        this.line = line;
        this.column = column;
    }

    public IntilaqException(String message) {
        super(message);
        this.line = -1;
        this.column = -1;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
