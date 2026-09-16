# AL-thmany Sender 3.4.1 — Professional RTL / Join / Scan / Diagnostics Design

## Goal
تحويل واجهة 3.4.1 إلى واجهة عربية RTL حقيقية، ربط اختيار واتساب بالمحرك الأصلي، جعل الانضمام يستخدم Sender الأصلي، جعل الفحص تصنيفياً فقط، عرض أول أربعة روابط مع عرض المزيد، وإضافة تشخيص Runtime حقيقي.

## Architecture
- تبقى `com.althmany.extractor.MainActivity` واجهة التشغيل الأساسية.
- الانضمام لا يستخدم `ScanController`. بل يستخدم `MainViewModel` مع `QuickJoinAccessibilityService` / `ShizukuAutomationService` الأصليين.
- الفحص يستخدم `ScanController` في `SCAN_ONLY` فقط، ولا ينفذ Join أو Request.
- اختيار واتساب في Workspace يزامن إعدادات Sender الأصلية.
- التشخيص يجمع `AccessibilityStatus`, `ShizukuBridge`, `RuntimeOperationCoordinator`, `RuntimeHealthMonitor`, `RuntimeDiagnosticStore`.
- حقول الروابط تعرض أربع أسطر في الوضع المطوي مع عرض المزيد/عرض أقل.

## Runtime invariants
- مالك واحد فقط لواجهة واتساب في أي لحظة.
- لا يتم اختلاق عدد الأعضاء إذا لم يظهر في واتساب.
- الفحص لا ينفذ أي إجراء عضوية.
- الانضمام يحافظ على Queue / Pause / Resume / Stop / verification للمحرك الأصلي.
- دعم Work / Secure / Dual المتقدم يبقى متاحاً عبر إعدادات Sender الأصلية.
