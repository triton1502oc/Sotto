# ProGuard rules for Sotto

# Keep data models used in SharedPreferences JSON serialization
-keepclassmembers class com.amh.sotto.data.** { *; }

# Keep TTS OnInitListener callback
-keepclassmembers class * implements android.speech.tts.TextToSpeech$OnInitListener {
    public void onInit(int);
}
