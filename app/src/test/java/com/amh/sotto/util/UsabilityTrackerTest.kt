package com.amh.sotto.util

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class UsabilityTrackerTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tempDir: File

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("sotto_test_files").toFile()
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)

        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 10000L

        every { context.filesDir } returns tempDir
        every { context.getSharedPreferences("sotto_voice_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        // Enable tracking for functional tests
        every { sharedPreferences.getBoolean("share_usability_metrics", false) } returns true
    }

    @After
    fun teardown() {
        unmockkStatic(SystemClock::class)
        tempDir.deleteRecursively()
    }

    @Test
    fun `event json serialization round-trip`() {
        val event = UsabilityTracker.UsabilityEvent(
            intent = "QuickSpeak",
            durationMs = 2450L,
            outcome = "completed",
            frictionTag = "none",
            contextTag = "len_12"
        )

        val json = event.toJsonObject()
        val deserialized = UsabilityTracker.UsabilityEvent.fromJsonObject(json)

        assertEquals("QuickSpeak", deserialized.intent)
        assertEquals(2450L, deserialized.durationMs)
        assertEquals("completed", deserialized.outcome)
        assertEquals("none", deserialized.frictionTag)
        assertEquals("len_12", deserialized.contextTag)
        assertEquals(event.day, deserialized.day)
        assertEquals(event.hourBucket, deserialized.hourBucket)
    }

    @Test
    fun `recordEvent ignores events when opt-in is false`() {
        every { sharedPreferences.getBoolean("share_usability_metrics", false) } returns false

        val event = UsabilityTracker.UsabilityEvent(
            intent = "SpeakCard",
            durationMs = 1200L,
            outcome = "completed"
        )

        UsabilityTracker.recordEvent(context, event)

        val pending = UsabilityTracker.getPendingEvents(context)
        assertTrue(pending.isEmpty())
        assertFalse(File(tempDir, UsabilityTracker.BUFFER_FILE_NAME).exists())
    }

    @Test
    fun `recordEvent appends event to file when opt-in is true`() {
        val event = UsabilityTracker.UsabilityEvent(
            intent = "SpeakCard",
            durationMs = 1200L,
            outcome = "completed"
        )

        UsabilityTracker.recordEvent(context, event)

        val pending = UsabilityTracker.getPendingEvents(context)
        assertEquals(1, pending.size)
        assertEquals("SpeakCard", pending[0].intent)
        assertEquals(1200L, pending[0].durationMs)
    }

    @Test
    fun `recordEvent enforces FIFO cap of 500 events`() {
        for (i in 1..510) {
            UsabilityTracker.recordEvent(
                context,
                UsabilityTracker.UsabilityEvent(
                    intent = "Intent_$i",
                    durationMs = i.toLong(),
                    outcome = "completed"
                )
            )
        }

        val pending = UsabilityTracker.getPendingEvents(context)
        assertEquals(500, pending.size)
        // First 10 should have been discarded (FIFO)
        assertEquals("Intent_11", pending.first().intent)
        assertEquals("Intent_510", pending.last().intent)
    }

    @Test
    fun `removeEvents removes dispatched events and deletes file when empty`() {
        val event1 = UsabilityTracker.UsabilityEvent("Intent_1", 100L, "completed")
        val event2 = UsabilityTracker.UsabilityEvent("Intent_2", 200L, "completed")

        UsabilityTracker.recordEvent(context, event1)
        UsabilityTracker.recordEvent(context, event2)

        assertEquals(2, UsabilityTracker.getPendingEvents(context).size)

        UsabilityTracker.removeEvents(context, listOf(event1))
        val remaining = UsabilityTracker.getPendingEvents(context)
        assertEquals(1, remaining.size)
        assertEquals("Intent_2", remaining[0].intent)

        UsabilityTracker.removeEvents(context, listOf(event2))
        assertTrue(UsabilityTracker.getPendingEvents(context).isEmpty())
        assertFalse(File(tempDir, UsabilityTracker.BUFFER_FILE_NAME).exists())
    }

    @Test
    fun `clearBuffer deletes buffer file immediately`() {
        val event = UsabilityTracker.UsabilityEvent("Intent_1", 100L, "completed")
        UsabilityTracker.recordEvent(context, event)

        assertTrue(File(tempDir, UsabilityTracker.BUFFER_FILE_NAME).exists())

        UsabilityTracker.clearBuffer(context)

        assertFalse(File(tempDir, UsabilityTracker.BUFFER_FILE_NAME).exists())
        assertTrue(UsabilityTracker.getPendingEvents(context).isEmpty())
    }

    @Test
    fun `categoryType correctly collapses custom categories and preserves standard categories`() {
        assertEquals("emergency", UsabilityTracker.categoryType("Emergency", false))
        assertEquals("emergency", UsabilityTracker.categoryType("General", true))
        assertEquals("needs", UsabilityTracker.categoryType("Needs", false))
        assertEquals("social", UsabilityTracker.categoryType("Social", false))
        assertEquals("care", UsabilityTracker.categoryType("Care", false))
        assertEquals("general", UsabilityTracker.categoryType("General", false))
        assertEquals("custom", UsabilityTracker.categoryType("My Private Custom Category", false))
    }

    @Test
    fun `recordCardTap aggregates routine taps into summary event`() {
        UsabilityTracker.recordCardTap(context, "My Private Custom Category", false, 12345)
        UsabilityTracker.recordCardTap(context, "Needs", false, 67890)

        val pending = UsabilityTracker.getPendingEvents(context)
        assertEquals(1, pending.size)
        val event = pending.first()
        assertEquals("CardTapsSummary", event.intent)
        assertTrue(event.contextTag.contains("custom=1"))
        assertTrue(event.contextTag.contains("needs=1"))
        assertFalse(event.contextTag.contains("Private"))
        assertFalse(event.contextTag.contains("12345"))
    }

    @Test
    fun `recordCardTap emits rapid tap immediately when clicking rapidly`() {
        UsabilityTracker.recordCardTap(context, "Social", false, 123)
        // Same target within threshold
        every { SystemClock.elapsedRealtime() } returns 10100L
        UsabilityTracker.recordCardTap(context, "Social", false, 123)

        val pending = UsabilityTracker.getPendingEvents(context)
        val rapidEvent = pending.firstOrNull { it.intent == "SpeakCard" }
        assertNotNull(rapidEvent)
        assertEquals("rapid_tap", rapidEvent!!.outcome)
        assertEquals("rapid_clicking", rapidEvent.frictionTag)
        assertEquals("cat_social", rapidEvent.contextTag)
    }

    @Test
    fun `recordWhyFinder emits correct coarse tags without recording personal text`() {
        UsabilityTracker.recordWhyFinder(
            context = context,
            outcome = "found",
            areaId = "senses",
            depth = 3,
            notSureCount = 1,
            bodyPart = null,
            intensity = null,
            durationMs = 25000L
        )

        val pending = UsabilityTracker.getPendingEvents(context)
        // Note: recordWhyFinder also emits an Activation event if first time
        val whyEvent = pending.firstOrNull { it.intent == "WhyFinder" }
        assertNotNull(whyEvent)
        assertEquals("found", whyEvent!!.outcome)
        assertEquals(25000L, whyEvent.durationMs)
        assertTrue(whyEvent.contextTag.contains("area_senses_d3_ns1"))
    }

    @Test
    fun `recordWhyLogViewed records correct count bucket`() {
        UsabilityTracker.recordWhyLogViewed(context, 0)
        UsabilityTracker.recordWhyLogViewed(context, 3)
        UsabilityTracker.recordWhyLogViewed(context, 12)
        UsabilityTracker.recordWhyLogViewed(context, 45)

        val pending = UsabilityTracker.getPendingEvents(context).filter { it.intent == "WhyLogViewed" }
        assertEquals(4, pending.size)
        assertEquals("0", pending[0].outcome)
        assertEquals("1_5", pending[1].outcome)
        assertEquals("6_20", pending[2].outcome)
        assertEquals("20_plus", pending[3].outcome)
    }

    @Test
    fun `generatePreviewPayload formats valid schema 3 JSON`() {
        UsabilityTracker.recordEvent(context, UsabilityTracker.UsabilityEvent("TestIntent", 100L, "completed"))
        val preview = UsabilityTracker.generatePreviewPayload(context)
        assertTrue(preview.contains("\"schema\": 3"))
        assertTrue(preview.contains("\"eventsCount\": 1"))
        assertTrue(preview.contains("\"TestIntent\""))
    }

    @Test
    fun `recordSettingsState records coarse adoption tags`() {
        val settings = com.amh.sotto.data.VoiceSettings(
            speechRate = 0.8f,
            playAttentionChime = true,
            showLanguageSwitcher = true,
            secondaryLanguage = "id"
        )
        UsabilityTracker.recordSettingsState(context, settings, 7)

        val pending = UsabilityTracker.getPendingEvents(context).filter { it.intent == "SettingsState" }
        assertEquals(1, pending.size)
        val event = pending.first()
        assertEquals("active", event.outcome)
        assertTrue(event.contextTag.contains("chime=1"))
        assertTrue(event.contextTag.contains("bi=1"))
        assertTrue(event.contextTag.contains("sec=id"))
        assertTrue(event.contextTag.contains("rate=slow"))
        assertTrue(event.contextTag.contains("cust=6_20"))
    }

    @Test
    fun `QuickSpeak intent concludes with saved_as_card and does not duplicate abort on clear`() {
        UsabilityTracker.startIntent("QuickSpeak")
        UsabilityTracker.endIntent(context, "QuickSpeak", outcome = "saved_as_card", contextTag = "len_10")
        // Subsequent clear text should not emit a second event because intent is no longer active
        UsabilityTracker.endIntent(context, "QuickSpeak", outcome = "aborted", frictionTag = "cleared_text")

        val pending = UsabilityTracker.getPendingEvents(context).filter { it.intent == "QuickSpeak" }
        assertEquals(1, pending.size)
        assertEquals("saved_as_card", pending.first().outcome)
    }

    @Test
    fun `QuickSpeak intent concludes with shown_fullscreen and does not duplicate abort on clear`() {
        UsabilityTracker.startIntent("QuickSpeak")
        UsabilityTracker.endIntent(context, "QuickSpeak", outcome = "shown_fullscreen", contextTag = "len_15")
        UsabilityTracker.endIntent(context, "QuickSpeak", outcome = "aborted", frictionTag = "cleared_text")

        val pending = UsabilityTracker.getPendingEvents(context).filter { it.intent == "QuickSpeak" }
        assertEquals(1, pending.size)
        assertEquals("shown_fullscreen", pending.first().outcome)
    }

    @Test
    fun `event json array tuple serialization round-trip`() {
        val event = UsabilityTracker.UsabilityEvent(
            intent = "TwoWay",
            durationMs = 4200L,
            outcome = "completed",
            frictionTag = "none",
            contextTag = "neutral_exit",
            day = "2026-10-08",
            hourBucket = 2
        )
        val array = event.toJsonArray()
        val deserialized = UsabilityTracker.UsabilityEvent.fromJsonArray(array)
        assertEquals(event, deserialized)
    }
}
