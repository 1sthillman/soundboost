# android.media.audiofx.* sınıfları platformun kendi sınıflarıdır, ek keep
# kuralı gerekmez. DataStore ve Compose için varsayılan AGP kuralları yeterlidir.

-keepattributes *Annotation*
-dontwarn kotlinx.coroutines.**

# Keep main application classes
-keep class com.soundboost.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontwarn kotlinx.serialization.**

# Keep UUID-related classes
-dontwarn kotlin.uuid.**
-keep class kotlin.uuid.** { *; }
