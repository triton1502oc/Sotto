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
}
