# HydraDroid release rules — Compose + Retrofit/Moshi + Room уже имеют встроенные rules,
# здесь только страховка от вырезания моделей (парсятся рефлексией Moshi codegen — не трогаем).
-keep class com.hydradroid.data.model.** { *; }
-keep class com.hydradroid.data.local.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
