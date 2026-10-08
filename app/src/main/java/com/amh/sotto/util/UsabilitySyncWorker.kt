package com.amh.sotto.util

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amh.sotto.BuildConfig
import com.amh.sotto.data.SharedPreferencesVoiceSettingsRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class UsabilitySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repo = SharedPreferencesVoiceSettingsRepository(applicationContext)
        if (!repo.getVoiceSettings().shareUsabilityMetrics) {
            UsabilityTracker.clearBuffer(applicationContext)
            return Result.success()
        }

        val pendingEvents = UsabilityTracker.getPendingEvents(applicationContext)
        if (pendingEvents.isEmpty()) {
            return Result.success()
        }

        val success = sendToGoogleSheet(pendingEvents)
        return if (success) {
            UsabilityTracker.removeEvents(applicationContext, pendingEvents)
            Result.success()
        } else {
            Result.retry()
        }
    }

    private fun sendToGoogleSheet(events: List<UsabilityTracker.UsabilityEvent>): Boolean {
        return runCatching {
            val repo = SharedPreferencesVoiceSettingsRepository(applicationContext)
            val installId = repo.getInstallId() ?: repo.getOrGenerateInstallId()
            val payload = JSONObject().apply {
                put("schema", 3)
                put("installId", installId)
                put("appVersion", "v${BuildConfig.VERSION_NAME}")
                put("role", repo.getUserRole())
                put("uiLang", LocaleHelper.getLanguage(applicationContext))
                val jsonEvents = JSONArray()
                for (e in events) {
                    jsonEvents.put(e.toJsonArray())
                }
                put("events", jsonEvents)
            }

            var currentUrl = UsabilityTracker.DEFAULT_WEBHOOK_URL
            var redirects = 0
            var success = false

            while (redirects < 3) {
                val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 4000
                    requestMethod = "POST"
                    instanceFollowRedirects = false
                    doOutput = true
                    setRequestProperty("Content-Type", "text/plain; charset=UTF-8")
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == 307 ||
                    responseCode == 308
                ) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location != null) {
                        currentUrl = location
                        redirects++
                        continue
                    } else {
                        break
                    }
                }

                success = responseCode in 200..299
                connection.disconnect()
                break
            }

            success
        }.getOrDefault(false)
    }
}
