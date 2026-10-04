package com.amh.sotto.util

import com.amh.sotto.data.WhyArea
import com.amh.sotto.data.WhyLogEntry
import com.amh.sotto.data.WhyOutcome
import com.amh.sotto.data.WhyQuestion
import com.amh.sotto.data.WhyQuestionType
import com.amh.sotto.data.WhyStep
import com.amh.sotto.data.WhyTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WhyFinderJsonTest {

    @Test
    fun `tree serialization and deserialization round trip`() {
        val tree = WhyTree(
            areas = listOf(
                WhyArea(
                    id = "body",
                    question = "Is it your body?",
                    questions = listOf(
                        WhyQuestion(id = "body_0", text = "Are you in pain?", type = WhyQuestionType.BODY_MAP),
                        WhyQuestion(id = "body_1", text = "Are you tired?", type = WhyQuestionType.YES_NO)
                    )
                ),
                WhyArea(
                    id = "senses",
                    question = "Is it too loud?",
                    questions = listOf(
                        WhyQuestion(id = "senses_0", text = "Is it loud?", type = WhyQuestionType.YES_NO)
                    )
                )
            )
        )

        val json = WhyFinderJson.treeToJson(tree)
        val deserialized = WhyFinderJson.treeFromJson(json)

        assertEquals(2, deserialized.areas.size)
        assertEquals("body", deserialized.areas[0].id)
        assertEquals("Is it your body?", deserialized.areas[0].question)
        assertEquals(2, deserialized.areas[0].questions.size)
        assertEquals(WhyQuestionType.BODY_MAP, deserialized.areas[0].questions[0].type)
        assertEquals("senses", deserialized.areas[1].id)
    }

    @Test
    fun `log entry serialization and deserialization round trip`() {
        val entry = WhyLogEntry(
            id = "test-log-1",
            startedAt = 1000L,
            endedAt = 5000L,
            outcome = WhyOutcome.FOUND,
            areaId = "body",
            causeText = "Are you in pain?",
            bodyPart = "head",
            intensity = 4,
            note = "After crowded transit",
            steps = listOf(
                WhyStep(questionText = "Is it your body?", answer = "yes"),
                WhyStep(questionText = "Are you in pain?", answer = "yes"),
                WhyStep(questionText = "Body part: head", answer = "selected"),
                WhyStep(questionText = "Intensity: 4/5", answer = "selected")
            )
        )

        val json = WhyFinderJson.logEntryToJson(entry)
        val deserialized = WhyFinderJson.logEntryFromJson(json)

        assertEquals(entry.id, deserialized.id)
        assertEquals(entry.startedAt, deserialized.startedAt)
        assertEquals(entry.endedAt, deserialized.endedAt)
        assertEquals(WhyOutcome.FOUND, deserialized.outcome)
        assertEquals("body", deserialized.areaId)
        assertEquals("Are you in pain?", deserialized.causeText)
        assertEquals("head", deserialized.bodyPart)
        assertEquals(4, deserialized.intensity)
        assertEquals("After crowded transit", deserialized.note)
        assertEquals(4, deserialized.steps.size)
        assertEquals("yes", deserialized.steps[0].answer)
    }

    @Test
    fun `log list serialization handles empty and multiple items`() {
        val entries = listOf(
            WhyLogEntry(id = "e1", startedAt = 100L, endedAt = 200L, outcome = WhyOutcome.NOT_FOUND),
            WhyLogEntry(id = "e2", startedAt = 300L, endedAt = 400L, outcome = WhyOutcome.STOPPED)
        )

        val array = WhyFinderJson.logListToJson(entries)
        val deserialized = WhyFinderJson.logListFromJson(array)

        assertEquals(2, deserialized.size)
        assertEquals("e1", deserialized[0].id)
        assertEquals(WhyOutcome.NOT_FOUND, deserialized[0].outcome)
        assertEquals("e2", deserialized[1].id)
        assertEquals(WhyOutcome.STOPPED, deserialized[1].outcome)
    }
}
