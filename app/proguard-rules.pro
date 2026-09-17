-keepattributes Signature
-keepattributes *Annotation*

-keep class br.investcalc.app.data.api.** { *; }
-keep class br.investcalc.app.domain.model.** { *; }
# (gerado pelo R8 - crashlytics) profiling da JVM do app no POJO do release
-dontwarn android.os.ProfilingTrigger
-dontwarn android.os.ProfilingTrigger$Builder
