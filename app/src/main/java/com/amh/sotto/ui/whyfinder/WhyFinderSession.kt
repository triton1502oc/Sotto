package com.amh.sotto.ui.whyfinder

import com.amh.sotto.data.WhyArea
import com.amh.sotto.data.WhyLogEntry
import com.amh.sotto.data.WhyOutcome
import com.amh.sotto.data.WhyQuestion
import com.amh.sotto.data.WhyQuestionType
import com.amh.sotto.data.WhyStep
import com.amh.sotto.data.WhyTree
import java.util.UUID

sealed class WhyFinderState {
    data class AskingArea(
        val areaIndex: Int,
        val area: WhyArea
    ) : WhyFinderState()

    data class AskingQuestion(
        val areaIndex: Int,
        val questionIndex: Int,
        val question: WhyQuestion
    ) : WhyFinderState()

    data class SelectingBodyPart(
        val areaIndex: Int,
        val questionIndex: Int,
        val question: WhyQuestion
    ) : WhyFinderState()

    data class SelectingIntensity(
        val areaIndex: Int,
        val questionIndex: Int,
        val question: WhyQuestion,
        val bodyPart: String
    ) : WhyFinderState()

    data class ConfirmingCause(
        val areaIndex: Int,
        val questionIndex: Int,
        val causeText: String,
        val bodyPart: String? = null,
        val intensity: Int? = null
    ) : WhyFinderState()

    data class Finished(
        val outcome: WhyOutcome,
        val areaId: String? = null,
        val causeText: String? = null,
        val bodyPart: String? = null,
        val intensity: Int? = null,
        val steps: List<WhyStep> = emptyList(),
        val startedAt: Long,
        val endedAt: Long
    ) : WhyFinderState()
}

class WhyFinderSession(
    val tree: WhyTree,
    val startedAt: Long = System.currentTimeMillis()
) {
    val sessionId: String = UUID.randomUUID().toString()
    private val recordedSteps = mutableListOf<WhyStep>()

    var currentState: WhyFinderState = determineInitialState()
        private set

    val steps: List<WhyStep>
        get() = recordedSteps.toList()

    private fun determineInitialState(): WhyFinderState {
        return if (tree.areas.isNotEmpty()) {
            WhyFinderState.AskingArea(areaIndex = 0, area = tree.areas[0])
        } else {
            WhyFinderState.Finished(
                outcome = WhyOutcome.NOT_FOUND,
                steps = emptyList(),
                startedAt = startedAt,
                endedAt = System.currentTimeMillis()
            )
        }
    }

    fun answerYes() {
        when (val state = currentState) {
            is WhyFinderState.AskingArea -> {
                recordedSteps.add(WhyStep(questionText = state.area.question, answer = "yes"))
                if (state.area.questions.isNotEmpty()) {
                    currentState = WhyFinderState.AskingQuestion(
                        areaIndex = state.areaIndex,
                        questionIndex = 0,
                        question = state.area.questions[0]
                    )
                } else {
                    // No detailed questions in area; advance to next area or finish
                    moveToNextArea(state.areaIndex)
                }
            }
            is WhyFinderState.AskingQuestion -> {
                recordedSteps.add(WhyStep(questionText = state.question.text, answer = "yes"))
                when (state.question.type) {
                    WhyQuestionType.BODY_MAP -> {
                        currentState = WhyFinderState.SelectingBodyPart(
                            areaIndex = state.areaIndex,
                            questionIndex = state.questionIndex,
                            question = state.question
                        )
                    }
                    WhyQuestionType.SCALE -> {
                        currentState = WhyFinderState.SelectingIntensity(
                            areaIndex = state.areaIndex,
                            questionIndex = state.questionIndex,
                            question = state.question,
                            bodyPart = ""
                        )
                    }
                    WhyQuestionType.YES_NO -> {
                        currentState = WhyFinderState.ConfirmingCause(
                            areaIndex = state.areaIndex,
                            questionIndex = state.questionIndex,
                            causeText = state.question.text
                        )
                    }
                }
            }
            is WhyFinderState.ConfirmingCause -> {
                val areaId = tree.areas.getOrNull(state.areaIndex)?.id
                recordedSteps.add(WhyStep(questionText = state.causeText, answer = "yes"))
                currentState = WhyFinderState.Finished(
                    outcome = WhyOutcome.FOUND,
                    areaId = areaId,
                    causeText = state.causeText,
                    bodyPart = state.bodyPart,
                    intensity = state.intensity,
                    steps = recordedSteps.toList(),
                    startedAt = startedAt,
                    endedAt = System.currentTimeMillis()
                )
            }
            is WhyFinderState.SelectingBodyPart,
            is WhyFinderState.SelectingIntensity,
            is WhyFinderState.Finished -> {
                // No-op in these states
            }
        }
    }

    fun answerNo() {
        handleNegativeAnswer(answerKey = "no")
    }

    fun answerNotSure() {
        handleNegativeAnswer(answerKey = "not_sure")
    }

    private fun handleNegativeAnswer(answerKey: String) {
        when (val state = currentState) {
            is WhyFinderState.AskingArea -> {
                recordedSteps.add(WhyStep(questionText = state.area.question, answer = answerKey))
                moveToNextArea(state.areaIndex)
            }
            is WhyFinderState.AskingQuestion -> {
                recordedSteps.add(WhyStep(questionText = state.question.text, answer = answerKey))
                val area = tree.areas[state.areaIndex]
                val nextQ = state.questionIndex + 1
                if (nextQ < area.questions.size) {
                    currentState = WhyFinderState.AskingQuestion(
                        areaIndex = state.areaIndex,
                        questionIndex = nextQ,
                        question = area.questions[nextQ]
                    )
                } else {
                    moveToNextArea(state.areaIndex)
                }
            }
            is WhyFinderState.ConfirmingCause -> {
                // Caregiver asked "Is this it?" and user answered No / Not sure
                recordedSteps.add(WhyStep(questionText = state.causeText, answer = answerKey))
                val area = tree.areas.getOrNull(state.areaIndex)
                val nextQ = state.questionIndex + 1
                if (area != null && nextQ < area.questions.size) {
                    currentState = WhyFinderState.AskingQuestion(
                        areaIndex = state.areaIndex,
                        questionIndex = nextQ,
                        question = area.questions[nextQ]
                    )
                } else {
                    moveToNextArea(state.areaIndex)
                }
            }
            is WhyFinderState.SelectingBodyPart,
            is WhyFinderState.SelectingIntensity -> {
                // If user doesn't know or says no to body part / intensity, treat as cause confirmed without part
                val cause = (currentState as? WhyFinderState.SelectingBodyPart)?.question?.text
                    ?: (currentState as? WhyFinderState.SelectingIntensity)?.question?.text
                    ?: ""
                val areaId = (currentState as? WhyFinderState.SelectingBodyPart)?.areaIndex
                    ?: (currentState as? WhyFinderState.SelectingIntensity)?.areaIndex
                    ?: 0
                currentState = WhyFinderState.ConfirmingCause(
                    areaIndex = areaId,
                    questionIndex = 0,
                    causeText = cause
                )
            }
            is WhyFinderState.Finished -> {
                // No-op
            }
        }
    }

    private fun moveToNextArea(currentAreaIndex: Int) {
        val nextArea = currentAreaIndex + 1
        if (nextArea < tree.areas.size) {
            currentState = WhyFinderState.AskingArea(
                areaIndex = nextArea,
                area = tree.areas[nextArea]
            )
        } else {
            currentState = WhyFinderState.Finished(
                outcome = WhyOutcome.NOT_FOUND,
                steps = recordedSteps.toList(),
                startedAt = startedAt,
                endedAt = System.currentTimeMillis()
            )
        }
    }

    fun selectBodyPart(bodyPart: String) {
        val state = currentState as? WhyFinderState.SelectingBodyPart ?: return
        recordedSteps.add(WhyStep(questionText = "Body part: $bodyPart", answer = "selected"))
        currentState = WhyFinderState.SelectingIntensity(
            areaIndex = state.areaIndex,
            questionIndex = state.questionIndex,
            question = state.question,
            bodyPart = bodyPart
        )
    }

    fun selectIntensity(level: Int) {
        val state = currentState as? WhyFinderState.SelectingIntensity ?: return
        val clamped = level.coerceIn(1, 5)
        recordedSteps.add(WhyStep(questionText = "Intensity: $clamped/5", answer = "selected"))
        currentState = WhyFinderState.ConfirmingCause(
            areaIndex = state.areaIndex,
            questionIndex = state.questionIndex,
            causeText = state.question.text,
            bodyPart = state.bodyPart,
            intensity = clamped
        )
    }

    fun stop() {
        if (currentState is WhyFinderState.Finished) return
        recordedSteps.add(WhyStep(questionText = "User tapped stop", answer = "stop"))
        val areaId = when (val s = currentState) {
            is WhyFinderState.AskingArea -> s.area.id
            is WhyFinderState.AskingQuestion -> tree.areas.getOrNull(s.areaIndex)?.id
            is WhyFinderState.SelectingBodyPart -> tree.areas.getOrNull(s.areaIndex)?.id
            is WhyFinderState.SelectingIntensity -> tree.areas.getOrNull(s.areaIndex)?.id
            is WhyFinderState.ConfirmingCause -> tree.areas.getOrNull(s.areaIndex)?.id
            is WhyFinderState.Finished -> null
        }
        currentState = WhyFinderState.Finished(
            outcome = WhyOutcome.STOPPED,
            areaId = areaId,
            steps = recordedSteps.toList(),
            startedAt = startedAt,
            endedAt = System.currentTimeMillis()
        )
    }

    fun toLogEntry(note: String? = null): WhyLogEntry? {
        val finished = currentState as? WhyFinderState.Finished ?: return null
        return WhyLogEntry(
            id = sessionId,
            startedAt = finished.startedAt,
            endedAt = finished.endedAt,
            outcome = finished.outcome,
            areaId = finished.areaId,
            causeText = finished.causeText,
            bodyPart = finished.bodyPart,
            intensity = finished.intensity,
            note = note?.trim()?.takeIf { it.isNotBlank() },
            steps = finished.steps
        )
    }
}
