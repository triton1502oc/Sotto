package com.amh.sotto.data

import android.content.Context
import android.content.SharedPreferences
import com.amh.sotto.R
import com.amh.sotto.util.WhyFinderJson
import org.json.JSONArray
import org.json.JSONObject

interface WhyFinderRepository {
    fun getTree(): WhyTree
    fun saveTree(tree: WhyTree)
    fun resetTreeToDefault(): WhyTree
    fun getLogs(): List<WhyLogEntry>
    fun addLog(entry: WhyLogEntry)
    fun deleteLog(id: String): Boolean
    fun clearLogs()
}

class SharedPreferencesWhyFinderRepository(
    private val context: Context,
    prefsName: String = "sotto_why_finder"
) : WhyFinderRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    companion object {
        const val KEY_TREE = "why_tree"
        const val KEY_LOGS = "why_logs"
        const val MAX_LOG_ENTRIES = 500

        const val AREA_BODY = "body"
        const val AREA_SENSES = "senses"
        const val AREA_FEELINGS = "feelings"
        const val AREA_PERSON = "person"
        const val AREA_HAPPENED = "happened"
        const val AREA_COMING_UP = "coming_up"

        val BUILT_IN_AREAS = listOf(
            AREA_BODY,
            AREA_SENSES,
            AREA_FEELINGS,
            AREA_PERSON,
            AREA_HAPPENED,
            AREA_COMING_UP
        )
    }

    fun getDefaultTree(): WhyTree {
        return try {
            val res = context.resources

            fun loadQuestions(arrayResId: Int, prefix: String, firstIsBodyMap: Boolean = false): List<WhyQuestion> {
                val items = res.getStringArray(arrayResId)
                return items.mapIndexed { index, text ->
                    WhyQuestion(
                        id = "${prefix}_$index",
                        text = text,
                        type = if (index == 0 && firstIsBodyMap) WhyQuestionType.BODY_MAP else WhyQuestionType.YES_NO
                    )
                }
            }

            val areas = listOf(
                WhyArea(
                    id = AREA_BODY,
                    question = res.getString(R.string.why_area_body_q),
                    questions = loadQuestions(R.array.default_why_questions_body, "body", firstIsBodyMap = true)
                ),
                WhyArea(
                    id = AREA_SENSES,
                    question = res.getString(R.string.why_area_senses_q),
                    questions = loadQuestions(R.array.default_why_questions_senses, "senses")
                ),
                WhyArea(
                    id = AREA_FEELINGS,
                    question = res.getString(R.string.why_area_feelings_q),
                    questions = loadQuestions(R.array.default_why_questions_feelings, "feelings")
                ),
                WhyArea(
                    id = AREA_PERSON,
                    question = res.getString(R.string.why_area_person_q),
                    questions = loadQuestions(R.array.default_why_questions_person, "person")
                ),
                WhyArea(
                    id = AREA_HAPPENED,
                    question = res.getString(R.string.why_area_happened_q),
                    questions = loadQuestions(R.array.default_why_questions_happened, "happened")
                ),
                WhyArea(
                    id = AREA_COMING_UP,
                    question = res.getString(R.string.why_area_coming_up_q),
                    questions = loadQuestions(R.array.default_why_questions_coming_up, "coming_up")
                )
            )
            WhyTree(areas)
        } catch (_: Exception) {
            getHardcodedFallbackTree()
        }
    }

    private fun getHardcodedFallbackTree(): WhyTree {
        return WhyTree(
            areas = listOf(
                WhyArea(
                    id = AREA_BODY,
                    question = "Is it something with your body or physical comfort?",
                    questions = listOf(
                        WhyQuestion("body_0", "Are you in pain or hurting somewhere?", WhyQuestionType.BODY_MAP),
                        WhyQuestion("body_1", "Are you hungry or thirsty?"),
                        WhyQuestion("body_2", "Are you feeling very tired or exhausted?"),
                        WhyQuestion("body_3", "Are you feeling too cold or too hot?"),
                        WhyQuestion("body_4", "Do you need the restroom?")
                    )
                ),
                WhyArea(
                    id = AREA_SENSES,
                    question = "Is it something around you overwhelming your senses?",
                    questions = listOf(
                        WhyQuestion("senses_0", "Is it too loud or noisy?"),
                        WhyQuestion("senses_1", "Is it too bright or glaring?"),
                        WhyQuestion("senses_2", "Is it too crowded or too many people around?"),
                        WhyQuestion("senses_3", "Is there an unpleasant smell?"),
                        WhyQuestion("senses_4", "Are your clothes or something touching you uncomfortable?")
                    )
                ),
                WhyArea(
                    id = AREA_FEELINGS,
                    question = "Is it a strong feeling or emotion inside?",
                    questions = listOf(
                        WhyQuestion("feelings_0", "Are you feeling scared or anxious?"),
                        WhyQuestion("feelings_1", "Are you feeling angry or frustrated?"),
                        WhyQuestion("feelings_2", "Are you feeling sad or lonely?"),
                        WhyQuestion("feelings_3", "Are you feeling overwhelmed or overloaded?"),
                        WhyQuestion("feelings_4", "Are you feeling embarrassed or misunderstood?")
                    )
                ),
                WhyArea(
                    id = AREA_PERSON,
                    question = "Is it about a person or someone's behavior?",
                    questions = listOf(
                        WhyQuestion("person_0", "Is it about someone here at home or family?"),
                        WhyQuestion("person_1", "Is it about someone from school, work, or day program?"),
                        WhyQuestion("person_2", "Did something happen online or in a message?"),
                        WhyQuestion("person_3", "Did a stranger or bystander do or say something?")
                    )
                ),
                WhyArea(
                    id = AREA_HAPPENED,
                    question = "Did something unexpected or frustrating happen earlier?",
                    questions = listOf(
                        WhyQuestion("happened_0", "Did a plan, schedule, or routine change?"),
                        WhyQuestion("happened_1", "Did something get lost, broken, or not work?"),
                        WhyQuestion("happened_2", "Did someone say something sharp, reprimand, or correct you?"),
                        WhyQuestion("happened_3", "Were you stopped from finishing something you wanted to do?")
                    )
                ),
                WhyArea(
                    id = AREA_COMING_UP,
                    question = "Is it worry about something that is going to happen next?",
                    questions = listOf(
                        WhyQuestion("coming_up_0", "Are we going somewhere you don't want to go?"),
                        WhyQuestion("coming_up_1", "Is it about going to school, work, or an activity soon?"),
                        WhyQuestion("coming_up_2", "Is it about an upcoming appointment or transition?"),
                        WhyQuestion("coming_up_3", "Is there a change in tomorrow or upcoming routine?")
                    )
                )
            )
        )
    }

    override fun getTree(): WhyTree {
        val jsonStr = prefs.getString(KEY_TREE, null) ?: return getDefaultTree()
        return try {
            val tree = WhyFinderJson.treeFromJson(JSONObject(jsonStr))
            if (tree.areas.isNotEmpty()) tree else getDefaultTree()
        } catch (_: Exception) {
            getDefaultTree()
        }
    }

    override fun saveTree(tree: WhyTree) {
        val json = WhyFinderJson.treeToJson(tree)
        prefs.edit().putString(KEY_TREE, json.toString()).apply()
    }

    override fun resetTreeToDefault(): WhyTree {
        val defaultTree = getDefaultTree()
        saveTree(defaultTree)
        return defaultTree
    }

    override fun getLogs(): List<WhyLogEntry> {
        val jsonStr = prefs.getString(KEY_LOGS, null) ?: return emptyList()
        return try {
            val list = WhyFinderJson.logListFromJson(JSONArray(jsonStr))
            list.sortedByDescending { it.startedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun addLog(entry: WhyLogEntry) {
        val current = getLogs().toMutableList()
        current.add(0, entry)
        if (current.size > MAX_LOG_ENTRIES) {
            val truncated = current.take(MAX_LOG_ENTRIES)
            saveLogsInternal(truncated)
        } else {
            saveLogsInternal(current)
        }
    }

    override fun deleteLog(id: String): Boolean {
        val current = getLogs().toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            saveLogsInternal(current)
        }
        return removed
    }

    override fun clearLogs() {
        prefs.edit().remove(KEY_LOGS).apply()
    }

    private fun saveLogsInternal(entries: List<WhyLogEntry>) {
        val array = WhyFinderJson.logListToJson(entries)
        prefs.edit().putString(KEY_LOGS, array.toString()).apply()
    }
}
