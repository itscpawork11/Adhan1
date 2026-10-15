# Keep kotlinx.serialization generated serializers for our data models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.ahmoodiy.prayer.** {
    *** Companion;
}
-keepclasseswithmembers class com.ahmoodiy.prayer.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ahmoodiy.prayer.**$$serializer { *; }
