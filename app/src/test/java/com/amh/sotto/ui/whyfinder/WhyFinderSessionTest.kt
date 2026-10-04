package com.amh.sotto.ui.whyfinder

import com.amh.sotto.data.WhyArea
import com.amh.sotto.data.WhyOutcome
import com.amh.sotto.data.WhyQuestion
import com.amh.sotto.data.WhyQuestionType
import com.amh.sotto.data.WhyTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WhyFinderSessionTest {

    private lateinit var testTree: WhyTree

    @Before
    fun setup() {
        testTree = WhyTree(
            areas = listOf(
                WhyArea(
                    id = "body",
                    question = "Is it your body?",
                    questions = listOf(
                        WhyQuestion("body_pain", "Are you in pain?", WhyQuestionType.BODY_MAP),
                        WhyQuestion("body_tired", "Are you tired?", WhyQuestionType.YES_NO)
                    )
                ),
                WhyArea(
                    id = "senses",
                    question = "Is it your senses?",
                    questions = listOf(
                        WhyQuestion("senses_loud", "Is it too loud?", WhyQuestionType.YES_NO),
                        WhyQuestion("senses_bright", "Is it too bright?", WhyQuestionType.YES_NO)
                    )
                )
            )
        )
    }

    @Test
    fun `successful path finding regular yes-no cause`() {
        val session = WhyFinderSession(testTree)

        // Initial: Asking Area 0 (body)
        assertTrue(session.currentState is WhyFinderState.AskingArea)
        assertEquals("body", (session.currentState as WhyFinderState.AskingArea).area.id)

        // No to body -> moves to senses
        session.answerNo()
        assertTrue(session.currentState is WhyFinderState.AskingArea)
        assertEquals("senses", (session.currentState as WhyFinderState.AskingArea).area.id)

        // Yes to senses -> moves to question 0 (too loud)
        session.answerYes()
        assertTrue(session.currentState is WhyFinderState.AskingQuestion)
        assertEquals("senses_loud", (session.currentState as WhyFinderState.AskingQuestion).question.id)

        // Yes to too loud -> moves to ConfirmingCause
        session.answerYes()
        assertTrue(session.currentState is WhyFinderState.ConfirmingCause)
        assertEquals("Is it too loud?", (session.currentState as WhyFinderState.ConfirmingCause).causeText)

        // Yes to confirm -> Finished with FOUND
        session.answerYes()
        assertTrue(session.currentState is WhyFinderState.Finished)
        val finished = session.currentState as WhyFinderState.Finished
        assertEquals(WhyOutcome.FOUND, finished.outcome)
        assertEquals("senses", finished.areaId)
        assertEquals("Is it too loud?", finished.causeText)

        val log = session.toLogEntry(note = "Noise at restaurant")
        assertNotNull(log)
        assertEquals(WhyOutcome.FOUND, log!!.outcome)
        assertEquals("Noise at restaurant", log.note)
    }

    @Test
    fun `body map and intensity scale flow`() {
        val session = WhyFinderSession(testTree)

        // Yes to body
        session.answerYes()
        assertTrue(session.currentState is WhyFinderState.AskingQuestion)
        val qState = session.currentState as WhyFinderState.AskingQuestion
        assertEquals("body_pain", qState.question.id)

        // Yes to pain -> SelectingBodyPart
        session.answerYes()
        assertTrue(session.currentState is WhyFinderState.SelectingBodyPart)

        // Select "head" -> SelectingIntensity
        session.selectBodyPart("head")
        assertTrue(session.currentState is WhyFinderState.SelectingIntensity)
        assertEquals("head", (session.currentState as WhyFinderState.SelectingIntensity).bodyPart)

        // Select intensity 4 -> ConfirmingCause
        session.selectIntensity(4)
        assertTrue(session.currentState is WhyFinderState.ConfirmingCause)
        val confirmState = session.currentState as WhyFinderState.ConfirmingCause
        assertEquals("head", confirmState.bodyPart)
        assertEquals(4, confirmState.intensity)

        // Confirm yes -> Finished
        session.answerYes()
        assertTrue(session.currentState is WhyFinderState.Finished)
        val log = session.toLogEntry()
        assertNotNull(log)
        assertEquals("head", log!!.bodyPart)
        assertEquals(4, log.intensity)
        assertEquals(WhyOutcome.FOUND, log.outcome)
    }

    @Test
    fun `exhausting all questions leads to NOT_FOUND`() {
        val session = WhyFinderSession(testTree)

        session.answerNotSure() // Body? -> Not sure
        session.answerNo()      // Senses? -> No

        assertTrue(session.currentState is WhyFinderState.Finished)
        val finished = session.currentState as WhyFinderState.Finished
        assertEquals(WhyOutcome.NOT_FOUND, finished.outcome)
    }

    @Test
    fun `stop at any point immediately terminates with STOPPED`() {
        val session = WhyFinderSession(testTree)
        session.answerYes() // In body questions
        session.stop()

        assertTrue(session.currentState is WhyFinderState.Finished)
        val finished = session.currentState as WhyFinderState.Finished
        assertEquals(WhyOutcome.STOPPED, finished.outcome)
    }

    @Test
    fun `answering no on cause confirmation resumes next question`() {
        val session = WhyFinderSession(testTree)
        session.answerNo() // Skip body
        session.answerYes() // Enter senses
        session.answerYes() // Yes to too loud -> ConfirmingCause
        assertTrue(session.currentState is WhyFinderState.ConfirmingCause)

        // User says "No" to "Is this it?"
        session.answerNo()
        // Should resume to next question in senses: "Is it too bright?"
        assertTrue(session.currentState is WhyFinderState.AskingQuestion)
        assertEquals("senses_bright", (session.currentState as WhyFinderState.AskingQuestion).question.id)
    }

    @Test
    fun `stepBack reverts to previous state and removes step`() {
        val session = WhyFinderSession(testTree)
        assertTrue(session.currentState is WhyFinderState.AskingArea)
        assertEquals(false, session.canStepBack())

        session.answerNo() // Moved to senses
        assertTrue(session.canStepBack())
        assertEquals("senses", (session.currentState as WhyFinderState.AskingArea).area.id)
        assertEquals(1, session.steps.size)

        val reverted = session.stepBack()
        assertTrue(reverted)
        assertEquals("body", (session.currentState as WhyFinderState.AskingArea).area.id)
        assertEquals(0, session.steps.size)
        assertEquals(false, session.canStepBack())
    }

    @Test
    fun `overrideCurrentQuestion changes active question text`() {
        val session = WhyFinderSession(testTree)
        session.overrideCurrentQuestion("Is the room too cold?")
        val state = session.currentState as WhyFinderState.AskingArea
        assertEquals("Is the room too cold?", state.area.question)
    }

    @Test
    fun `jumpToArea jumps to specified area and supports stepBack`() {
        val session = WhyFinderSession(testTree)
        session.jumpToArea(1)
        val state = session.currentState as WhyFinderState.AskingArea
        assertEquals("senses", state.area.id)
        assertTrue(session.canStepBack())

        val reverted = session.stepBack()
        assertTrue(reverted)
        assertEquals("body", (session.currentState as WhyFinderState.AskingArea).area.id)
    }

    @Test
    fun `jumpToQuestion jumps directly to question in area and can answer`() {
        val session = WhyFinderSession(testTree)
        session.jumpToQuestion(1, 0) // senses_loud
        val state = session.currentState as WhyFinderState.AskingQuestion
        assertEquals("senses_loud", state.question.id)

        session.answerYes()
        val confirmState = session.currentState as WhyFinderState.ConfirmingCause
        assertEquals("Is it too loud?", confirmState.causeText)
    }

    @Test
    fun `restart resets session to initial state`() {
        val session = WhyFinderSession(testTree)
        session.answerYes()
        session.restart()
        assertTrue(session.currentState is WhyFinderState.AskingArea)
        assertEquals("body", (session.currentState as WhyFinderState.AskingArea).area.id)
        assertEquals(0, session.steps.size)
    }
}
