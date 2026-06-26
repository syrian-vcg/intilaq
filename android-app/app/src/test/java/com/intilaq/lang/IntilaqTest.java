package com.intilaq.lang;

import org.junit.Test;
import static org.junit.Assert.*;

/** اختبارات وحدة أساسية لمحرك لغة "الانطلاق" (Lexer + Parser + IntilaqValue). */
public class IntilaqTest {

    @Test
    public void testSimpleObject() {
        String src = "كائن {\n اسم = \"أحمد\"\n خانة = 25\nحاجز\n}";
        IntilaqValue v = Intilaq.parse(src);
        assertTrue(v.isObject());
        assertEquals("أحمد", v.get("اسم").asString());
        assertEquals(25.0, v.get("خانة").asNumber(), 0.0001);
    }

    @Test
    public void testNestedObject() {
        String src = "كائن {\n عنوان = كائن {\n مدينة = \"الرياض\"\nحاجز\n}\nحاجز\n}";
        IntilaqValue v = Intilaq.parse(src);
        IntilaqValue addr = v.get("عنوان");
        assertTrue(addr.isObject());
        assertEquals("الرياض", addr.get("مدينة").asString());
    }

    @Test
    public void testArray() {
        String src = "كائن {\n هوايات = مجموعة {\n حلقة { بند = \"قراءة\" }\n حلقة { بند = \"برمجة\" }\nحاجز\n}\nحاجز\n}";
        IntilaqValue v = Intilaq.parse(src);
        IntilaqValue hobbies = v.get("هوايات");
        assertTrue(hobbies.isArray());
        assertEquals(2, hobbies.asList().size());
        assertEquals("قراءة", hobbies.asList().get(0).asString());
        assertEquals("برمجة", hobbies.asList().get(1).asString());
    }

    @Test
    public void testBooleanAndNull() {
        String src = "كائن {\n نجح = صحيح\n خطاء = خاطئ\n تسمية = فراغ\nحاجز\n}";
        IntilaqValue v = Intilaq.parse(src);
        assertTrue(v.get("نجح").asBoolean());
        assertFalse(v.get("خطاء").asBoolean());
        assertTrue(v.get("تسمية").isNull());
    }

    @Test
    public void testToJson() {
        String src = "كائن {\n اسم = \"سارة\"\n خانة = 30\nحاجز\n}";
        IntilaqValue v = Intilaq.parse(src);
        String json = v.toJson();
        assertTrue(json.contains("\"اسم\":\"سارة\""));
        assertTrue(json.contains("\"خانة\":30"));
    }

    @Test
    public void testComments() {
        String src = "كائن {\n # هذا تعليق\n اسم = \"ليلى\"\nحاجز\n}";
        IntilaqValue v = Intilaq.parse(src);
        assertEquals("ليلى", v.get("اسم").asString());
    }

    @Test(expected = IntilaqException.class)
    public void testMissingHajez() {
        String src = "كائن {\n اسم = \"خطأ\"\n}";
        Intilaq.parse(src);
    }

    @Test(expected = IntilaqException.class)
    public void testMissingEquals() {
        String src = "كائن {\n اسم \"بدون يساوي\"\nحاجز\n}";
        Intilaq.parse(src);
    }

    @Test
    public void testTryParseFailureMessage() {
        String src = "كائن { اسم = حاجز }"; // قيمة ناقصة بعد =
        Intilaq.Result result = Intilaq.tryParse(src);
        assertFalse(result.success);
        assertNotNull(result.errorMessage);
    }

    @Test
    public void testRoundTripIntilaqSource() {
        String src = "كائن {\n اسم = \"محمد\"\n خانة = 40\nحاجز\n}";
        IntilaqValue v = Intilaq.parse(src);
        String regenerated = v.toIntilaqSource();
        IntilaqValue v2 = Intilaq.parse(regenerated);
        assertEquals(v.get("اسم").asString(), v2.get("اسم").asString());
        assertEquals(v.get("خانة").asNumber(), v2.get("خانة").asNumber(), 0.0001);
    }
}
