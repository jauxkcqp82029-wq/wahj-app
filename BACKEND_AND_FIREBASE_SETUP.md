# دليل تشغيل Backend وإعداد Firebase لتطبيق وهج (WAHJ)

## 1. البنية الحالية (Local-First + Wi-Fi LAN Sync)
يعمل تطبيق **وهج | WAHJ** حالياً بشكل كامل ومستقل بدون الحاجة لمفاتيح سحابية مدفوعة:
- **قاعدة البيانات المحلية (Room SQLite):** تخزين مشفر بالملح (Salted SHA-256) للحسابات، ملفات الأطفال، قواعد التطبيقات، جداول الدراسة والنوم، الرسائل، طلبات الوقت الإضافي، المناطق الآمنة، وسجل التنبيهات.
- **المزامنة المباشرة بين جهازين (LocalLanSyncEngine):** خادم TCP/JSON محلي يعمل تلقائياً على المنفذ `8765` (أو منفذ متاح)، ويتم تضمين عنوان IP والمنفذ ورمز التشفير المؤقت أحادي الاستخدام داخل رمز **QR** لتنفيذ الربط الفعلي بين جهاز ولي الأمر وجهاز الطفل على نفس شبكة Wi-Fi.
- **مهام الخلفية (WorkManager):**
  - `ScheduledMessageWorker`: لإرسال الرسائل المجدولة وإطلاق إشعارات باسم وهج.
  - `MonitoringSyncWorker`: لمزامنة البطارية وإحصائيات الاستخدام بشكل دوري كل 15 دقيقة.

## 2. خطوات ربط Firebase Cloud (اختياري للتشغيل عبر شبكات مختلفة)
إذا رغبت مستقبلاً في تفعيل المزامنة السحابية عبر الإنترنت خارج شبكة Wi-Fi المحلية:
1. أنشئ مشروعاً جديداً في **Firebase Console** وأضف تطبيق Android بمعرّف الحزمة:
   `com.aistudio.wahj.krmxvp`
2. أضف بصمات التوقيع (SHA-1 و SHA-256) في إعدادات التطبيق بـ Firebase، ثم حمّل ملف `google-services.json` وضعه داخل مجلد `app/`.
3. فعّل المنتجات التالية من لوحة Firebase:
   - **Authentication:** فعّل مزوّد تسجيل الدخول (Google Sign-In أو البريد وكلمة المرور).
   - **Cloud Firestore:** أنشئ قاعدة البيانات وطبّق قواعد أمان تتحقق من `request.auth != null` وعزل بيانات كل عائلة (`parentId == request.auth.uid`).
   - **Firebase Cloud Messaging (FCM):** لإرسال الإشعارات الفورية عند إغلاق التطبيق.
   - **Firebase Storage (اختياري):** لحفظ صور الملف الشخصي.
4. في ملف `app/build.gradle.kts`، قم بإزالة التعليق عن تبعيات `firebase-bom` و `firebase-firestore` و `firebase-auth` ثم أعد بناء ملف APK.

## 3. بناء ملف APK Release قابل للتثبيت
- عبر Android Studio: اختر **Build > Generate Signed Bundle / APK > APK** وحدد `release`.
- أو عبر سطر الأوامر: `gradle :app:assembleRelease` (بعد ضبط متغيرات التوقيع `KEYSTORE_PATH` و `STORE_PASSWORD` و `KEY_PASSWORD`).
