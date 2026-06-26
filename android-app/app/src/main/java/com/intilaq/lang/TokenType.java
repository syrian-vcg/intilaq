package com.intilaq.lang;

public enum TokenType {
    KEYWORD,        // كلمة محجوزة: اسم، معرف، مجموعة...
    IDENTIFIER,     // اسم حقل غير محجوز (نادر الاستخدام في هذه اللغة، لكن مسموح)
    STRING,         // "نص بين علامتي تنصيص"
    NUMBER,         // 123 أو 12.5
    BOOLEAN,        // صحيح / خطأ (كقيمة منطقية -- تُكتب: صحيح أو خاطئ)
    NULL,           // فراغ
    EQUALS,         // =
    LBRACE,         // {
    RBRACE,         // }
    COMMENT,        // # ... تعليق
    EOF
}
