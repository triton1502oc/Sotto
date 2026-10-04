package com.amh.sotto.util

import android.content.Context
import android.os.SystemClock
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.amh.sotto.data.Phrase
import com.amh.sotto.data.SharedPreferencesVoiceSettingsRepository
import com.amh.sotto.data.SharedPreferencesWhyFinderRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight, privacy-preserving usability metrics tracker.
 * Strictly collects anonymous interaction durations, rapid-tap signals, and intent completions.
 * Strictly NEVER collects user-typed text, phrase content, or audio recordings.
 */
object UsabilityTracker {

    const val DEFAULT_WEBHOOK_URL =
        "https://script.google.com/macros/s/AKfycbyvnBbI2WBF4UhSoZc-kb9AIbh3-ofDdzwleyUmTFeujYePSLqeAJbR28C_OdvXamHn/exec"

    const val BUFFER_FILE_NAME = "usability_events.json"
    const val MAX_BUFFER_SIZE = 500
    const val RAPID_TAP_THRESHOLD_MS = 350L
    const val HESITATION_THRESHOLD_MS = 10000L
    const val WORK_NAME_SYNC = "usability_metrics_sync"

    val sessionId: String = UUID.randomUUID().toString().take(8)

    private val activeIntents = ConcurrentHashMap<String, Long>()
    private val bufferLock = Any()

    private var lastTapTimestamp = 0L
    private var lastTapTarget = 0

    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getCurrentHourBucket(): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return (hour / 4).coerceIn(0, 5)
    }

    data class UsabilityEvent(
        val intent: String,
        val durationMs: Long,
        val outcome: String,
        val frictionTag: String = "none",
        val contextTag: String = "",
        val day: String = getTodayDate(),
        val hourBucket: Int = getCurrentHourBucket()
    ) {
        fun toJsonObject(): JSONObject = JSONObject().apply {
            put("intent", intent)
            put("durationMs", durationMs)
            put("outcome", outcome)
            put("frictionTag", frictionTag)
            put("contextTag", contextTag)
            put("day", day)
            put("hourBucket", hourBucket)
        }

        companion object {
            fun fromJsonObject(json: JSONObject): UsabilityEvent = UsabilityEvent(
                intent = json.optString("intent", ""),
                durationMs = json.optLong("durationMs", 0L),
                outcome = json.optString("outcome", ""),
                frictionTag = json.optString("frictionTag", "none"),
                contextTag = json.optString("contextTag", ""),
                day = json.optString("day", getTodayDate()),
                hourBucket = json.optInt("hourBucket", getCurrentHourBucket())
            )
        }
    }

    fun startIntent(name: String) {
        activeIntents[name] = SystemClock.elapsedRealtime()
    }

    fun endIntent(
        context: Context,
        name: String,
        outcome: String,
        frictionTag: String = "none",
        contextTag: String = ""
    ) {
        val startTime = activeIntents.remove(name)
        val duration = if (startTime != null) {
            (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(0L)
        } else {
            0L
        }

        // Flag high hesitation if duration exceeds threshold and no specific friction was set
        val resolvedFriction = if (frictionTag == "none" && duration >= HESITATION_THRESHOLD_MS) {
            "hesitation_high"
        } else {
            frictionTag
        }

        recordEvent(context, UsabilityEvent(name, duration, outcome, resolvedFriction, contextTag))
    }

    /**
     * Maps a category to a coarse, non-identifying type. Custom category names are user-authored
     * text and must never be transmitted, so they collapse to "custom".
     */
    fun categoryType(category: String, isEmergency: Boolean): String {
        if (isEmergency || category.equals(Phrase.CATEGORY_EMERGENCY, ignoreCase = true)) return "emergency"
        val builtIn = Phrase.DEFAULT_CATEGORIES.firstOrNull { it.equals(category, ignoreCase = true) }
        return builtIn?.lowercase(Locale.ROOT) ?: "custom"
    }

    /**
     * Records a card tap. [localTapKey] is used only in memory to detect rapid repeated taps
     * on the same card and is never persisted or transmitted.
     */
    fun recordCardTap(context: Context, category: String, isEmergency: Boolean, localTapKey: Int) {
        val now = SystemClock.elapsedRealtime()
        val interval = now - lastTapTimestamp
        val isRapid = (localTapKey == lastTapTarget && interval < RAPID_TAP_THRESHOLD_MS)

        lastTapTimestamp = now
        lastTapTarget = localTapKey

        val outcome = if (isRapid) "rapid_tap" else "completed"
        val friction = if (isRapid) "rapid_clicking" else "none"

        recordEvent(
            context,
            UsabilityEvent(
                intent = "SpeakCard",
                durationMs = interval.coerceAtMost(30000L),
                outcome = outcome,
                frictionTag = friction,
                contextTag = "cat_${categoryType(category, isEmergency)}"
            )
        )
    }

    /**
     * Records AppOpen at most once per day and hourBucket block.
     */
    fun recordAppOpen(context: Context) {
        val today = getTodayDate()
        val hourBucket = getCurrentHourBucket()
        val prefs = context.getSharedPreferences("sotto_app_open", Context.MODE_PRIVATE)
        val lastDay = prefs.getString("last_day", "")
        val lastHour = prefs.getInt("last_hour", -1)

        if (lastDay != today || lastHour != hourBucket) {
            prefs.edit().putString("last_day", today).putInt("last_hour", hourBucket).apply()
            recordEvent(
                context,
                UsabilityEvent(
                    intent = "AppOpen",
                    durationMs = 0L,
                    outcome = "active",
                    day = today,
                    hourBucket = hourBucket
                )
            )
        }
    }

    fun recordRoleSet(context: Context, role: String) {
        recordEvent(
            context,
            UsabilityEvent(
                intent = "RoleSet",
                durationMs = 0L,
                outcome = role
            )
        )
    }

    fun recordActivation(context: Context, activationKey: String) {
        val prefs = context.getSharedPreferences("sotto_activations", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(activationKey, false)) {
            prefs.edit().putBoolean(activationKey, true).apply()
            recordEvent(
                context,
                UsabilityEvent(
                    intent = "Activation",
                    durationMs = 0L,
                    outcome = activationKey
                )
            )
        }
    }

    fun recordWhyFinder(
        context: Context,
        outcome: String,
        areaId: String?,
        depth: Int,
        notSureCount: Int,
        bodyPart: String?,
        intensity: Int?,
        durationMs: Long
    ) {
        val safeArea = if (areaId != null && SharedPreferencesWhyFinderRepository.BUILT_IN_AREAS.contains(areaId)) {
            areaId
        } else if (!areaId.isNullOrBlank()) {
            "custom"
        } else {
            "none"
        }
        val safePart = bodyPart ?: "none"
        val safeIntensity = intensity?.toString() ?: "none"
        recordEvent(
            context,
            UsabilityEvent(
                intent = "WhyFinder",
                durationMs = durationMs,
                outcome = outcome,
                contextTag = "area_${safeArea}_d${depth}_ns${notSureCount}_bp_${safePart}_int_${safeIntensity}"
            )
        )
        recordActivation(context, "why_finder")
    }

    fun recordWhyLogViewed(context: Context, count: Int) {
        val bucket = when {
            count == 0 -> "0"
            count in 1..5 -> "1_5"
            count in 6..20 -> "6_20"
            else -> "20_plus"
        }
        recordEvent(
            context,
            UsabilityEvent(
                intent = "WhyLogViewed",
                durationMs = 0L,
                outcome = bucket
            )
        )
    }

    fun recordBackupAction(
        context: Context,
        isExport: Boolean,
        includesPhrases: Boolean,
        includesTree: Boolean,
        includesLog: Boolean,
        isReplace: Boolean
    ) {
        val intentName = if (isExport) "Export" else "Import"
        val p = if (includesPhrases) "1" else "0"
        val t = if (includesTree) "1" else "0"
        val l = if (includesLog) "1" else "0"
        val mode = if (!isExport && isReplace) "replace" else "merge"
        recordEvent(
            context,
            UsabilityEvent(
                intent = intentName,
                durationMs = 0L,
                outcome = mode,
                contextTag = "p${p}_t${t}_l${l}"
            )
        )
    }

    fun recordSurvey(
        context: Context,
        pmfScore: String,
        primaryBenefit: String,
        confirmedRole: String
    ) {
        recordEvent(
            context,
            UsabilityEvent(
                intent = "Survey",
                durationMs = 0L,
                outcome = pmfScore,
                contextTag = "benefit=${primaryBenefit};role=${confirmedRole}"
            )
        )
    }

    fun recordEvent(context: Context, event: UsabilityEvent) {
        val repo = SharedPreferencesVoiceSettingsRepository(context)
        if (!repo.getVoiceSettings().shareUsabilityMetrics) {
            return // Strict Opt-In: do nothing if user has not enabled tracking
        }

        synchronized(bufferLock) {
            val file = getBufferFile(context)
            val currentList = readEventsFromFile(file).toMutableList()

            if (currentList.size >= MAX_BUFFER_SIZE) {
                // Drop oldest elements to respect maximum cap
                currentList.removeAt(0)
            }
            currentList.add(event)
            writeEventsToFile(file, currentList)
        }

        // Schedule unmetered Wi-Fi sync
        enqueueSync(context)
    }

    fun generatePreviewPayload(context: Context): String {
        val repo = SharedPreferencesVoiceSettingsRepository(context)
        val installId = repo.getInstallId() ?: "none"
        val role = repo.getUserRole()
        val events = getPendingEvents(context)

        val root = JSONObject().apply {
            put("schema", 2)
            put("installId", installId)
            put("role", role)
            put("uiLang", LocaleHelper.getLanguage(context))
            put("eventsCount", events.size)
            val jsonEvents = JSONArray()
            events.forEach { jsonEvents.put(it.toJsonObject()) }
            put("events", jsonEvents)
        }
        return root.toString(2)
    }

    fun enqueueSync(context: Context) {
        runCatching {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // Strictly Wi-Fi only
                .setRequiresBatteryNotLow(true)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<UsabilitySyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_SYNC,
                ExistingWorkPolicy.KEEP,
                syncWorkRequest
            )
        }
    }

    fun getPendingEvents(context: Context): List<UsabilityEvent> {
        synchronized(bufferLock) {
            val file = getBufferFile(context)
            return readEventsFromFile(file)
        }
    }

    fun removeEvents(context: Context, eventsToRemove: List<UsabilityEvent>) {
        synchronized(bufferLock) {
            val file = getBufferFile(context)
            val currentList = readEventsFromFile(file).toMutableList()
            for (toRemove in eventsToRemove) {
                currentList.remove(toRemove)
            }
            if (currentList.isEmpty()) {
                file.delete()
            } else {
                writeEventsToFile(file, currentList)
            }
        }
    }

    fun clearBuffer(context: Context) {
        synchronized(bufferLock) {
            getBufferFile(context).delete()
        }
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_SYNC)
        }
    }

    private fun getBufferFile(context: Context): File {
        return File(context.filesDir, BUFFER_FILE_NAME)
    }

    private fun readEventsFromFile(file: File): List<UsabilityEvent> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val jsonStr = file.readText()
            val jsonArray = JSONArray(jsonStr)
            val result = ArrayList<UsabilityEvent>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                result.add(UsabilityEvent.fromJsonObject(jsonArray.getJSONObject(i)))
            }
            result
        }.getOrDefault(emptyList())
    }

    private fun writeEventsToFile(file: File, events: List<UsabilityEvent>) {
        runCatching {
            val jsonArray = JSONArray()
            for (e in events) {
                jsonArray.put(e.toJsonObject())
            }
            file.writeText(jsonArray.toString())
        }
    }
}
