# إسلام برو — تطبيق Android أصلي

هذا مجلد مشروع Android مستقل مكتوب بـ Kotlin وJetpack Compose. لا يستخدم WebView أو TWA.

## البناء
يتطلب JDK 17 وAndroid SDK Platform 35 وGradle 8.9:
`gradle assembleDebug`

ينتج الملف:
`app/build/outputs/apk/debug/app-debug.apk`

## الحالة
هذه نقطة انطلاق أصلية بواجهة عربية RTL وأقسام رئيسية. ليست إعادة تنفيذ مكتملة لجميع خدمات الموقع بعد؛ ربط المصحف والتلاوات والبث ومواقيت الصلاة والبيانات المحلية يحتاج نقل منطق المشروع وواجهات البيانات واختبارها على Android.
