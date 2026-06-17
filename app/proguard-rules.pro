# Keep kotlinx.serialization generated serializers for the data models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.animesh.timetable.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.animesh.timetable.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
