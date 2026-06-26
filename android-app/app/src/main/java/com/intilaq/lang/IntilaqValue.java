package com.intilaq.lang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * يمثّل أي قيمة في لغة "الانطلاق": كائن (خرائط مفاتيح/قيم)، مجموعة (قوائم)،
 * نص، رقم، منطقي (صحيح/خاطئ)، أو فراغ (null).
 * يشبه دور JsonNode أو JSONObject في المكتبات المعروفة، لكنه مصمم خصيصاً
 * لبنية "كائن { ... حاجز }" و"مجموعة { حلقة {...} حاجز }".
 */
public class IntilaqValue {

    public enum Kind { OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL }

    private final Kind kind;
    private Map<String, IntilaqValue> objectValue;
    private List<IntilaqValue> arrayValue;
    private String stringValue;
    private double numberValue;
    private boolean boolValue;

    private IntilaqValue(Kind kind) {
        this.kind = kind;
    }

    public static IntilaqValue ofObject() {
        IntilaqValue v = new IntilaqValue(Kind.OBJECT);
        v.objectValue = new LinkedHashMap<>();
        return v;
    }

    public static IntilaqValue ofArray() {
        IntilaqValue v = new IntilaqValue(Kind.ARRAY);
        v.arrayValue = new ArrayList<>();
        return v;
    }

    public static IntilaqValue ofString(String s) {
        IntilaqValue v = new IntilaqValue(Kind.STRING);
        v.stringValue = s;
        return v;
    }

    public static IntilaqValue ofNumber(double n) {
        IntilaqValue v = new IntilaqValue(Kind.NUMBER);
        v.numberValue = n;
        return v;
    }

    public static IntilaqValue ofBoolean(boolean b) {
        IntilaqValue v = new IntilaqValue(Kind.BOOLEAN);
        v.boolValue = b;
        return v;
    }

    public static IntilaqValue ofNull() {
        return new IntilaqValue(Kind.NULL);
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isObject() { return kind == Kind.OBJECT; }
    public boolean isArray() { return kind == Kind.ARRAY; }
    public boolean isString() { return kind == Kind.STRING; }
    public boolean isNumber() { return kind == Kind.NUMBER; }
    public boolean isBoolean() { return kind == Kind.BOOLEAN; }
    public boolean isNull() { return kind == Kind.NULL; }

    // ---------- كائن ----------

    public void put(String key, IntilaqValue value) {
        requireKind(Kind.OBJECT);
        objectValue.put(key, value);
    }

    public IntilaqValue get(String key) {
        requireKind(Kind.OBJECT);
        return objectValue.get(key);
    }

    public Map<String, IntilaqValue> asObjectMap() {
        requireKind(Kind.OBJECT);
        return objectValue;
    }

    // ---------- مجموعة ----------

    public void add(IntilaqValue value) {
        requireKind(Kind.ARRAY);
        arrayValue.add(value);
    }

    public List<IntilaqValue> asList() {
        requireKind(Kind.ARRAY);
        return arrayValue;
    }

    // ---------- قيم بسيطة ----------

    public String asString() {
        requireKind(Kind.STRING);
        return stringValue;
    }

    public double asNumber() {
        requireKind(Kind.NUMBER);
        return numberValue;
    }

    public boolean asBoolean() {
        requireKind(Kind.BOOLEAN);
        return boolValue;
    }

    private void requireKind(Kind expected) {
        if (kind != expected) {
            throw new IntilaqException("توقعت قيمة من نوع " + expected + " لكن وجدت " + kind);
        }
    }

    /** يحوّل القيمة إلى نص JSON قياسي، مفيد للتبادل مع أنظمة أخرى. */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        writeJson(sb);
        return sb.toString();
    }

    private void writeJson(StringBuilder sb) {
        switch (kind) {
            case OBJECT:
                sb.append('{');
                boolean firstObj = true;
                for (Map.Entry<String, IntilaqValue> e : objectValue.entrySet()) {
                    if (!firstObj) sb.append(',');
                    firstObj = false;
                    sb.append('"').append(escapeJson(e.getKey())).append('"').append(':');
                    e.getValue().writeJson(sb);
                }
                sb.append('}');
                break;
            case ARRAY:
                sb.append('[');
                boolean firstArr = true;
                for (IntilaqValue v : arrayValue) {
                    if (!firstArr) sb.append(',');
                    firstArr = false;
                    v.writeJson(sb);
                }
                sb.append(']');
                break;
            case STRING:
                sb.append('"').append(escapeJson(stringValue)).append('"');
                break;
            case NUMBER:
                if (numberValue == Math.floor(numberValue) && !Double.isInfinite(numberValue)) {
                    sb.append((long) numberValue);
                } else {
                    sb.append(numberValue);
                }
                break;
            case BOOLEAN:
                sb.append(boolValue ? "true" : "false");
                break;
            case NULL:
                sb.append("null");
                break;
        }
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /** يحوّل القيمة مرة أخرى إلى نص لغة "الانطلاق" نفسها (تنسيق جميل بمسافات بادئة). */
    public String toIntilaqSource() {
        StringBuilder sb = new StringBuilder();
        writeIntilaq(sb, 0);
        return sb.toString();
    }

    private void writeIntilaq(StringBuilder sb, int indent) {
        switch (kind) {
            case OBJECT:
                sb.append("كائن {\n");
                for (Map.Entry<String, IntilaqValue> e : objectValue.entrySet()) {
                    indent(sb, indent + 1);
                    sb.append(e.getKey()).append(" = ");
                    e.getValue().writeIntilaq(sb, indent + 1);
                    sb.append('\n');
                }
                indent(sb, indent);
                sb.append("حاجز }");
                break;
            case ARRAY:
                sb.append("مجموعة {\n");
                for (IntilaqValue v : arrayValue) {
                    indent(sb, indent + 1);
                    sb.append("حلقة { بند = ");
                    v.writeIntilaq(sb, indent + 1);
                    sb.append(" }\n");
                }
                indent(sb, indent);
                sb.append("حاجز }");
                break;
            case STRING:
                sb.append('"').append(stringValue.replace("\"", "\\\"")).append('"');
                break;
            case NUMBER:
                if (numberValue == Math.floor(numberValue) && !Double.isInfinite(numberValue)) {
                    sb.append((long) numberValue);
                } else {
                    sb.append(numberValue);
                }
                break;
            case BOOLEAN:
                sb.append(boolValue ? "صحيح" : "خاطئ");
                break;
            case NULL:
                sb.append("فراغ");
                break;
        }
    }

    private static void indent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) sb.append("    ");
    }

    @Override
    public String toString() {
        return toJson();
    }
}
