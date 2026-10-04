package com.amh.sotto.util

import com.amh.sotto.data.WhyArea
import com.amh.sotto.data.WhyLogEntry
import com.amh.sotto.data.WhyOutcome
import com.amh.sotto.data.WhyQuestion
import com.amh.sotto.data.WhyQuestionType
import com.amh.sotto.data.WhyStep
import com.amh.sotto.data.WhyTree
import org.json.JSONArray
import org.json.JSONObject

object WhyFinderJson {

    fun treeToJson(tree: WhyTree): JSONObject {
        val root = JSONObject()
        val areasArray = JSONArray()
        tree.areas.forEach { area ->
            val areaObj = JSONObject()
            areaObj.put("id", area.id)
            areaObj.put("question", area.question)

            val questionsArray = JSONArray()
            area.questions.forEach { q ->
                val qObj = JSONObject()
                qObj.put("id", q.id)
                qObj.put("text", q.text)
                qObj.put("type", q.type.name)
                questionsArray.put(qObj)
            }
            areaObj.put("questions", questionsArray)
            areasArray.put(areaObj)
        }
        root.put("areas", areasArray)
        return root
    }

    fun treeFromJson(json: JSONObject): WhyTree {
        val areasList = mutableListOf<WhyArea>()
        val areasArray = json.optJSONArray("areas")
        if (areasArray != null) {
            for (i in 0 until areasArray.length()) {
                val areaObj = areasArray.optJSONObject(i) ?: continue
                val id = areaObj.optString("id", "").trim()
                val question = areaObj.optString("question", "").trim()
                val questionsList = mutableListOf<WhyQuestion>()
                val questionsArray = areaObj.optJSONArray("questions")
                if (questionsArray != null) {
                    for (j in 0 until questionsArray.length()) {
                        val qObj = questionsArray.optJSONObject(j) ?: continue
                        val qId = qObj.optString("id", "").trim()
                        val qText = qObj.optString("text", "").trim()
                        val qTypeStr = qObj.optString("type", WhyQuestionType.YES_NO.name)
                        val qType = runCatching { WhyQuestionType.valueOf(qTypeStr) }.getOrDefault(WhyQuestionType.YES_NO)
                        if (qText.isNotBlank()) {
                            questionsList.add(
                                WhyQuestion(
                                    id = if (qId.isNotBlank()) qId else "q_${i}_$j",
                                    text = qText,
                                    type = qType
                                )
                            )
                        }
                    }
                }
                if (question.isNotBlank()) {
                    areasList.add(
                        WhyArea(
                            id = if (id.isNotBlank()) id else "area_$i",
                            question = question,
                            questions = questionsList
                        )
                    )
                }
            }
        }
        return WhyTree(areas = areasList)
    }

    fun logEntryToJson(entry: WhyLogEntry): JSONObject {
        val obj = JSONObject()
        obj.put("id", entry.id)
        obj.put("startedAt", entry.startedAt)
        obj.put("endedAt", entry.endedAt)
        obj.put("outcome", entry.outcome.name)
        if (!entry.areaId.isNullOrBlank()) obj.put("areaId", entry.areaId)
        if (!entry.causeText.isNullOrBlank()) obj.put("causeText", entry.causeText)
        if (!entry.bodyPart.isNullOrBlank()) obj.put("bodyPart", entry.bodyPart)
        if (entry.intensity != null) obj.put("intensity", entry.intensity)
        if (!entry.note.isNullOrBlank()) obj.put("note", entry.note)

        val stepsArray = JSONArray()
        entry.steps.forEach { step ->
            val stepObj = JSONObject()
            stepObj.put("questionText", step.questionText)
            stepObj.put("answer", step.answer)
            stepsArray.put(stepObj)
        }
        obj.put("steps", stepsArray)
        return obj
    }

    fun logEntryFromJson(json: JSONObject): WhyLogEntry {
        val id = json.optString("id", "")
        val startedAt = json.optLong("startedAt", 0L)
        val endedAt = json.optLong("endedAt", 0L)
        val outcomeStr = json.optString("outcome", WhyOutcome.NOT_FOUND.name)
        val outcome = runCatching { WhyOutcome.valueOf(outcomeStr) }.getOrDefault(WhyOutcome.NOT_FOUND)
        val areaId = json.optString("areaId", "").takeIf { it.isNotBlank() }
        val causeText = json.optString("causeText", "").takeIf { it.isNotBlank() }
        val bodyPart = json.optString("bodyPart", "").takeIf { it.isNotBlank() }
        val intensity = if (json.has("intensity")) json.optInt("intensity") else null
        val note = json.optString("note", "").takeIf { it.isNotBlank() }

        val stepsList = mutableListOf<WhyStep>()
        val stepsArray = json.optJSONArray("steps")
        if (stepsArray != null) {
            for (i in 0 until stepsArray.length()) {
                val sObj = stepsArray.optJSONObject(i) ?: continue
                val qText = sObj.optString("questionText", "")
                val ans = sObj.optString("answer", "")
                if (qText.isNotBlank()) {
                    stepsList.add(WhyStep(questionText = qText, answer = ans))
                }
            }
        }

        return WhyLogEntry(
            id = id,
            startedAt = startedAt,
            endedAt = endedAt,
            outcome = outcome,
            areaId = areaId,
            causeText = causeText,
            bodyPart = bodyPart,
            intensity = intensity,
            note = note,
            steps = stepsList
        )
    }

    fun logListToJson(entries: List<WhyLogEntry>): JSONArray {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(logEntryToJson(entry))
        }
        return array
    }

    fun logListFromJson(array: JSONArray): List<WhyLogEntry> {
        val list = mutableListOf<WhyLogEntry>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val entry = logEntryFromJson(obj)
            if (entry.id.isNotBlank()) {
                list.add(entry)
            }
        }
        return list
    }
}
