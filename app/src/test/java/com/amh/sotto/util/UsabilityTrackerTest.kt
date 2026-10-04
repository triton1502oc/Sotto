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
    fun `recordEvent enforces FIFO cap of 50 events`() {
        for (i in 1..60) {
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
        assertEquals(50, pending.size)
        // First 10 should have been discarded (FIFO)
        assertEquals("Intent_11", pending.first().intent)
        assertEquals("Intent_60", pending.last().intent)
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
    fun `recordCardTap emits only coarse contextTag and does not record phrase text or hash`() {
        UsabilityTracker.recordCardTap(context, "My Private Custom Category", false, 12345)

        val pending = UsabilityTracker.getPendingEvents(context)
        assertEquals(1, pending.size)
        val event = pending.first()
        assertEquals("SpeakCard", event.intent)
        assertEquals("cat_custom", event.contextTag)
        assertFalse(event.contextTag.contains("Private"))
        assertFalse(event.contextTag.contains("12345"))
        val json = event.toJsonObject().toString()
        assertFalse(json.contains("Private"))
        assertFalse(json.contains("12345"))
    }
}
