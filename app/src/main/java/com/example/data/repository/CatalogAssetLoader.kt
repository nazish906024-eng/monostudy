package com.example.data.repository

import android.content.Context
import com.example.data.model.ChapterCatalog
import com.example.data.model.PyqCatalog
import com.example.data.model.PyqQuestion
import com.example.data.model.StudyConstants
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CatalogAssetLoader(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadChapters(): ChapterCatalog {
        val raw = context.assets.open("chapter-data.json").bufferedReader().use { it.readText() }
        return json.decodeFromString<ChapterCatalog>(raw)
    }

    fun loadBundledPyqs(): PyqCatalog {
        val raw = context.assets.open("pyq-data.json").bufferedReader().use { it.readText() }
        return json.decodeFromString<PyqCatalog>(raw)
    }

    companion object {
        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        data class ImportParseResult(
            val valid: List<PyqQuestion>,
            val rejectedCount: Int
        )

        fun parseCustomQuestionsJson(
            rawJson: String,
            validChapterIds: Set<String>
        ): ImportParseResult {
            val element = jsonParser.parseToJsonElement(rawJson)
            val items: JsonArray = when (element) {
                is JsonArray -> element
                is JsonObject -> element["questions"]?.jsonArray
                    ?: throw IllegalArgumentException("Expected a list, or an object with a \"questions\" list")
                else -> throw IllegalArgumentException("Invalid JSON structure")
            }

            val ok = mutableListOf<PyqQuestion>()
            var bad = 0
            for (item in items) {
                val obj = (item as? JsonObject) ?: run {
                    bad++
                    continue
                }
                val rawId = obj["id"]?.jsonPrimitive?.content?.trim().orEmpty()
                val safeId = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
                val qText = obj["q"]?.jsonPrimitive?.content?.trim().orEmpty().take(2000)
                val subject = obj["subject"]?.jsonPrimitive?.content.orEmpty()
                val optionsArray = obj["options"] as? JsonArray
                val options = optionsArray?.mapNotNull {
                    it.jsonPrimitive.content.trim().takeIf { s -> s.isNotEmpty() }?.take(300)
                }.orEmpty()
                val answerIdx = obj["answer"]?.jsonPrimitive?.intOrNull

                val valid = safeId.isNotEmpty() &&
                    qText.isNotEmpty() &&
                    subject in StudyConstants.SUBJECTS &&
                    optionsArray != null &&
                    options.size == optionsArray.size &&
                    options.size in 2..4 &&
                    answerIdx != null &&
                    answerIdx in options.indices

                if (!valid) {
                    bad++
                    continue
                }

                val examRaw = obj["exam"]?.jsonPrimitive?.content.orEmpty()
                val exam = if (examRaw in listOf("JEE", "NEET")) examRaw else "JEE"
                val year = obj["year"]?.jsonPrimitive?.intOrNull?.takeIf { it in 1970..2100 }
                val chapterRaw = obj["chapterId"]?.jsonPrimitive?.content.orEmpty()
                val chapterId = if (chapterRaw in validChapterIds) chapterRaw else ""
                val diffRaw = obj["difficulty"]?.jsonPrimitive?.content.orEmpty()
                val difficulty = if (diffRaw in StudyConstants.DIFFICULTIES) diffRaw else "Medium"
                val tags = (obj["tags"] as? JsonArray)
                    ?.mapNotNull { it.jsonPrimitive.content.trim().takeIf { t -> t.isNotEmpty() }?.take(30) }
                    ?.take(8)
                    .orEmpty()
                val explain = obj["explain"]?.jsonPrimitive?.content.orEmpty().take(1500)

                ok.add(
                    PyqQuestion(
                        id = safeId,
                        exam = exam,
                        year = year,
                        subject = subject,
                        chapterId = chapterId,
                        difficulty = difficulty,
                        tags = tags,
                        q = qText,
                        options = options,
                        answer = answerIdx!!,
                        explain = explain,
                        custom = true
                    )
                )
            }
            return ImportParseResult(valid = ok, rejectedCount = bad)
        }
    }
}
