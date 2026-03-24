# ── General attributes ──────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── youtubedl-android ───────────────────────────────────────────────────────
# JNI and reflection used internally; keep the entire library namespace
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-keep class com.yausername.aria2c.** { *; }

# ── Room ────────────────────────────────────────────────────────────────────
# Keep all RoomDatabase subclasses (generated _Impl classes)
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep all @Entity annotated classes and their fields
-keep @androidx.room.Entity class * { *; }

# Keep all @Dao annotated interfaces and their @Query methods
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Dao interface * {
    @androidx.room.Query <methods>;
    @androidx.room.Insert <methods>;
    @androidx.room.Update <methods>;
    @androidx.room.Delete <methods>;
    @androidx.room.Transaction <methods>;
}

# ── Firebase ─────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ── Play Billing ─────────────────────────────────────────────────────────────
-keep class com.android.vending.billing.** { *; }

# ── Kotlin serialization ─────────────────────────────────────────────────────
# Keep Companion objects, serializer() methods, and $$serializer classes
-keepclassmembers class com.socialvideodownloader.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.socialvideodownloader.**$$serializer { *; }
-keepclassmembers class com.socialvideodownloader.** {
    public static ** Companion;
}
-keepclassmembers class com.socialvideodownloader.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Hilt ─────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
