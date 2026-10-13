package com.amh.sotto.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WhyFinderRepositoryTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private val memoryStore = mutableMapOf<String, Any?>()

    @Before
    fun setup() {
        memoryStore.clear()
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)

        val editor: SharedPreferences.Editor = mockk(relaxed = true)
        every { sharedPreferences.edit() } returns editor

        every { editor.putString(any(), any()) } answers {
            val key = firstArg<String>()
            val value = secondArg<String>()
            memoryStore[key] = value
            editor
        }
        every { editor.remove(any()) } answers {
            val key = firstArg<String>()
            memoryStore.remove(key)
            editor
        }
        every { editor.apply() } returns Unit

        every { sharedPreferences.getString(any(), any()) } answers {
            val key = firstArg<String>()
            val def = secondArg<String?>()
            (memoryStore[key] as? String) ?: def
        }

        every { context.getSharedPreferences("sotto_why_finder", Context.MODE_PRIVATE) } returns sharedPreferences
    }

    @Test
    fun `getTree returns fallback tree when uninitialized`() {
        val repo = SharedPreferencesWhyFinderRepository(context)
        val tree = repo.getTree()
        assertTrue(tree.areas.isNotEmpty())
        assertEquals(6, tree.areas.size)
        assertEquals("body", tree.areas[0].id)
    }

    @Test
    fun `saveTree and getTree round trip`() {
        val repo = SharedPreferencesWhyFinderRepository(context)
        val customTree = WhyTree(
            areas = listOf(
                WhyArea(id = "custom_area", question = "Custom?", questions = emptyList())
            )
        )
        repo.saveTree(customTree)
        val retrieved = repo.getTree()
        assertEquals(1, retrieved.areas.size)
        assertEquals("custom_area", retrieved.areas[0].id)
    }

    @Test
    fun `resetTreeToDefault resets custom tree`() {
        val repo = SharedPreferencesWhyFinderRepository(context)
        val customTree = WhyTree(areas = emptyList())
        repo.saveTree(customTree)

        val reset = repo.resetTreeToDefault()
        assertTrue(reset.areas.isNotEmpty())
        assertEquals(6, repo.getTree().areas.size)
    }

    @Test
    fun `addLog prepends entries and truncates at max cap`() {
        val repo = SharedPreferencesWhyFinderRepository(context)

        // Add 505 entries
        for (i in 1..505) {
            repo.addLog(
                WhyLogEntry(
                    id = "entry_$i",
                    startedAt = i * 1000L,
                    endedAt = (i * 1000L) + 500L,
                    outcome = WhyOutcome.FOUND
                )
            )
        }

        val logs = repo.getLogs()
        assertEquals(SharedPreferencesWhyFinderRepository.MAX_LOG_ENTRIES, logs.size)
        // Newest should be entry_505
        assertEquals("entry_505", logs.first().id)
        // Oldest entries 1..5 should have been pruned
        assertFalse(logs.any { it.id == "entry_1" })
        assertFalse(logs.any { it.id == "entry_5" })
        assertTrue(logs.any { it.id == "entry_6" })
    }

    @Test
    fun `deleteLog removes entry by id and clearLogs purges all`() {
        val repo = SharedPreferencesWhyFinderRepository(context)
        repo.addLog(WhyLogEntry(id = "log_a", startedAt = 100L, endedAt = 200L, outcome = WhyOutcome.FOUND))
        repo.addLog(WhyLogEntry(id = "log_b", startedAt = 300L, endedAt = 400L, outcome = WhyOutcome.NOT_FOUND))

        assertEquals(2, repo.getLogs().size)

        val deleted = repo.deleteLog("log_a")
        assertTrue(deleted)
        val remaining = repo.getLogs()
        assertEquals(1, remaining.size)
        assertEquals("log_b", remaining[0].id)

        repo.clearLogs()
        assertTrue(repo.getLogs().isEmpty())
    }

    @Test
    fun `quick post-crisis pattern trigger entries are saved with cause and area`() {
        val repo = SharedPreferencesWhyFinderRepository(context)
        val triggerEntry = WhyLogEntry(
            id = "quick_trigger_1",
            startedAt = 1000L,
            endedAt = 4000L,
            outcome = WhyOutcome.FOUND,
            areaId = SharedPreferencesWhyFinderRepository.AREA_BODY,
            causeText = "Pain",
            bodyPart = "pain",
            steps = listOf(
                com.amh.sotto.data.WhyStep(questionText = "Are you in pain?", answer = "yes")
            )
        )
        repo.addLog(triggerEntry)

        val logs = repo.getLogs()
        assertEquals(1, logs.size)
        val saved = logs[0]
        assertEquals("quick_trigger_1", saved.id)
        assertEquals(WhyOutcome.FOUND, saved.outcome)
        assertEquals(SharedPreferencesWhyFinderRepository.AREA_BODY, saved.areaId)
        assertEquals("Pain", saved.causeText)
        assertEquals("pain", saved.bodyPart)
        assertEquals(1, saved.steps.size)
        assertEquals("yes", saved.steps[0].answer)
    }

    @Test
    fun `addLog upserts log entries with matching id instead of duplicating`() {
        val repo = SharedPreferencesWhyFinderRepository(context)
        val initialEntry = WhyLogEntry(
            id = "session_123",
            startedAt = 1000L,
            endedAt = 2000L,
            outcome = WhyOutcome.NOT_FOUND,
            areaId = "body"
        )
        repo.addLog(initialEntry)
        assertEquals(1, repo.getLogs().size)
        assertEquals(WhyOutcome.NOT_FOUND, repo.getLogs()[0].outcome)

        val updatedEntry = initialEntry.copy(
            endedAt = 3000L,
            outcome = WhyOutcome.FOUND,
            causeText = "Headache"
        )
        repo.addLog(updatedEntry)

        val logs = repo.getLogs()
        assertEquals(1, logs.size)
        assertEquals("session_123", logs[0].id)
        assertEquals(WhyOutcome.FOUND, logs[0].outcome)
        assertEquals("Headache", logs[0].causeText)
    }
}
