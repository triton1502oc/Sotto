package com.amh.sotto.data

enum class WhyQuestionType {
    YES_NO,
    BODY_MAP,
    SCALE
}

data class WhyQuestion(
    val id: String,
    val text: String,
    val type: WhyQuestionType = WhyQuestionType.YES_NO
)

data class WhyArea(
    val id: String,
    val question: String,
    val questions: List<WhyQuestion>
)

data class WhyTree(
    val areas: List<WhyArea>
)

enum class WhyOutcome {
    FOUND,
    NOT_FOUND,
    STOPPED
}

data class WhyStep(
    val questionText: String,
    val answer: String // "yes" | "no" | "not_sure"
)

data class WhyLogEntry(
    val id: String,
    val startedAt: Long,
    val endedAt: Long,
    val outcome: WhyOutcome,
    val areaId: String? = null,
    val causeText: String? = null,
    val bodyPart: String? = null,
    val intensity: Int? = null, // 1 to 5
    val note: String? = null,
    val steps: List<WhyStep> = emptyList()
)

object BodyRegions {
    const val HEAD = "head"
    const val EYES = "eyes"
    const val EARS = "ears"
    const val TEETH = "teeth"
    const val THROAT = "throat"
    const val CHEST = "chest"
    const val STOMACH = "stomach"
    const val BACK = "back"
    const val ARMS_HANDS = "arms_hands"
    const val LEGS_FEET = "legs_feet"
    const val SKIN = "skin"

    val ALL = listOf(
        HEAD,
        EYES,
        EARS,
        TEETH,
        THROAT,
        CHEST,
        STOMACH,
        BACK,
        ARMS_HANDS,
        LEGS_FEET,
        SKIN
    )
}
