# ProGuard rules for Sotto

# Keep data models used in SharedPreferences JSON serialization
-keepclassmembers class com.amh.sotto.data.** { *; }

# Keep TTS OnInitListener callback
-keepclassmembers class * implements android.speech.tts.TextToSpeech$OnInitListener {
    public void onInit(int);
}

# Keep Google ML Kit Translation and Play Services components
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_translate.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Keep Usability & Telemetry WorkManager Worker and Models
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.InputMerger { *; }
-keep class * extends androidx.room.RoomDatabase { public <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { public <init>(); }
-keep class androidx.work.impl.** { *; }
-keep class com.amh.sotto.util.UsabilitySyncWorker { *; }
-keep class com.amh.sotto.util.UsabilityTracker$UsabilityEvent { *; }

