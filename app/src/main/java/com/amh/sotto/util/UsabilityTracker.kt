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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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
    const val MAX_BUFFER_SIZE = 50
    const val RAPID_TAP_THRESHOLD_MS = 350L
    const val HESITATION_THRESHOLD_MS = 10000L
    const val WORK_NAME_SYNC = "usability_metrics_sync"

    val sessionId: String = UUID.randomUUID().toString().take(8)

    private val activeIntents = ConcurrentHashMap<String, Long>()
    private val bufferLock = Any()

    private var lastTapTimestamp = 0L
    private var lastTapTarget = 0

    data class UsabilityEvent(
        val intent: String,
        val durationMs: Long,
        val outcome: String,
        val frictionTag: String = "none",
        val contextTag: String = ""
    ) {
        fun toJsonObject(): JSONObject = JSONObject().apply {
            put("intent", intent)
            put("durationMs", durationMs)
            put("outcome", outcome)
            put("frictionTag", frictionTag)
            put("contextTag", contextTag)
        }

        companion object {
            fun fromJsonObject(json: JSONObject): UsabilityEvent = UsabilityEvent(
                intent = json.optString("intent", ""),
                durationMs = json.optLong("durationMs", 0L),
                outcome = json.optString("outcome", ""),
                frictionTag = json.optString("frictionTag", "none"),
                contextTag = json.optString("contextTag", "")
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
        return builtIn?.lowercase(java.util.Locale.ROOT) ?: "custom"
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
