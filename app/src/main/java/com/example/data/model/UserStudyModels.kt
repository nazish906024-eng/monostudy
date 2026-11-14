package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StudyConstants {
    val SUBJECTS = listOf("Physics", "Chemistry", "Mathematics", "Biology")
    val TASK_SUBJECTS = SUBJECTS + "General"
    val CHAPTER_STATUSES = listOf("Not Started", "In Progress", "Revised", "Mastered")
    val DONE_STATUSES = setOf("Revised", "Mastered")
    val CLASSES = listOf("11", "12")
    val EXAMS = listOf("JEE", "NEET", "Both")
    val DIFFICULTIES = listOf("Easy", "Medium", "Hard")
    val CONFIDENCE_LEVELS = listOf("LOW", "MEDIUM", "HIGH")
    val ERROR_CATEGORIES = listOf("Concept gap", "Calculation", "Silly mistake", "Ran out of time")
    val POMODORO_PRESETS = listOf(25, 45, 50, 60)

    fun subjectsForExam(exam: String): List<String> = when (exam) {
        "JEE" -> listOf("Physics", "Chemistry", "Mathematics")
        "NEET" -> listOf("Physics", "Chemistry", "Biology")
        else -> SUBJECTS
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val cls: String = "11",
    val exam: String = "JEE",
    val goalMinutes: Long = 180L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChapterStatusDoc(
    @DocumentId val id: String = "",
    val chapterId: String = "",
    val status: String = "Not Started",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyTask(
    @DocumentId val id: String = "",
    val text: String = "",
    val subject: String = "General",
    val done: Boolean = false,
    val date: String = "",
    val doneOn: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyNote(
    @DocumentId val id: String = "",
    val title: String = "",
    val subject: String = "General",
    val body: String = "",
    val imageIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NoteImage(
    @DocumentId val id: String = "",
    val data: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PyqAttempt(
    @DocumentId val id: String = "",
    val qid: String = "",
    val chapterId: String = "",
    val subject: String = "Physics",
    val selectedOption: Long = 0L,
    val correct: Boolean = false,
    val secs: Long = 1L,
    val confidence: String = "MEDIUM",
    val errorCategory: String = "",
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ErrorLogEntry(
    @DocumentId val id: String = "",
    val qid: String = "",
    val resolved: Boolean = false,
    val category: String = "",
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FlashcardProgress(
    @DocumentId val id: String = "",
    val cardId: String = "",
    val status: String = "review",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudySession(
    @DocumentId val id: String = "",
    val dateKey: String = "",
    val minutes: Long = 25L,
    val tag: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomPyq(
    @DocumentId val id: String = "",
    val exam: String = "JEE",
    val year: Long? = null,
    val subject: String = "Physics",
    val chapterId: String = "",
    val difficulty: String = "Medium",
    val tags: List<String> = emptyList(),
    val question: String = "",
    val options: List<String> = emptyList(),
    val answerIndex: Long = 0L,
    val explain: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toPyqQuestion(): PyqQuestion = PyqQuestion(
        id = id,
        exam = exam,
        year = year?.toInt(),
        subject = subject,
        chapterId = chapterId,
        difficulty = difficulty,
        tags = tags,
        q = question,
        options = options,
        answer = answerIndex.toInt(),
        explain = explain,
        custom = true
    )
}

data class ChapterAccStat(
    val total: Int = 0,
    val correct: Int = 0
) {
    val accuracyPct: Int?
        get() = if (total > 0) ((correct.toFloat() / total) * 100f).toInt().coerceIn(0, 100) else null
}

data class WeakSpot(
    val chapter: Chapter,
    val attemptsCount: Int,
    val accuracyPct: Int?
)

data class StreakInfo(
    val days: Int,
    val gracesUsed: Int
)

object StudyAnalytics {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayKey(date: LocalDate = LocalDate.now()): String = date.format(dateFormatter)

    fun lastNDays(n: Int, endDate: LocalDate = LocalDate.now()): List<LocalDate> =
        (n - 1 downTo 0).map { offset -> endDate.minusDays(offset.toLong()) }

    fun pct(part: Int, total: Int): Int =
        if (total <= 0) 0 else Math.round((part.toFloat() / total.toFloat()) * 100f)

    fun filteredChapters(allChapters: List<Chapter>, profile: UserProfile): List<Chapter> {
        val clsNum = profile.cls.toIntOrNull() ?: 11
        val allowedSubjects = StudyConstants.subjectsForExam(profile.exam).toSet()
        return allChapters.filter { it.cls == clsNum && it.subject in allowedSubjects }
    }

    fun chapterAccuracyMap(attempts: List<PyqAttempt>): Map<String, ChapterAccStat> {
        val map = mutableMapOf<String, ChapterAccStat>()
        for (a in attempts) {
            val prev = map[a.chapterId] ?: ChapterAccStat()
            map[a.chapterId] = ChapterAccStat(
                total = prev.total + 1,
                correct = prev.correct + if (a.correct) 1 else 0
            )
        }
        return map
    }

    fun overallAccuracyPct(attempts: List<PyqAttempt>): Int? =
        if (attempts.isEmpty()) null else pct(attempts.count { it.correct }, attempts.size)

    fun minutesOnDate(sessions: List<StudySession>, dateKey: String): Int =
        sessions.filter { it.dateKey == dateKey }.sumOf { it.minutes.toInt() }

    fun activeDateKeys(
        sessions: List<StudySession>,
        attempts: List<PyqAttempt>,
        tasks: List<StudyTask>
    ): Set<String> {
        val set = mutableSetOf<String>()
        sessions.filter { it.minutes > 0 && it.dateKey.isNotBlank() }.forEach { set.add(it.dateKey) }
        attempts.filter { it.dateKey.isNotBlank() }.forEach { set.add(it.dateKey) }
        tasks.filter { it.done && !it.doneOn.isNullOrBlank() }.forEach { set.add(it.doneOn!!) }
        return set
    }

    /**
     * Streak with 1 grace day: one isolated missed day is forgiven;
     * two missed days in a row break the streak.
     */
    fun streak(
        sessions: List<StudySession>,
        attempts: List<PyqAttempt>,
        tasks: List<StudyTask>,
        today: LocalDate = LocalDate.now()
    ): StreakInfo {
        val active = activeDateKeys(sessions, attempts, tasks)
        var d = today
        var count = 0
        var graces = 0
        var pending = 0
        var prevMiss = false
        for (i in 0 until 400) {
            val k = todayKey(d)
            if (k in active) {
                count++
                graces += pending
                pending = 0
                prevMiss = false
            } else if (i > 0) {
                if (prevMiss) break
                prevMiss = true
                pending++
            }
            d = d.minusDays(1)
        }
        return StreakInfo(days = count, gracesUsed = graces)
    }

    fun weakSpots(
        chapters: List<Chapter>,
        statusMap: Map<String, String>,
        attempts: List<PyqAttempt>
    ): List<WeakSpot> {
        val acc = chapterAccuracyMap(attempts)
        return chapters
            .filter { it.weightage == "High" }
            .map { chapter ->
                val stat = acc[chapter.id]
                WeakSpot(
                    chapter = chapter,
                    attemptsCount = stat?.total ?: 0,
                    accuracyPct = stat?.accuracyPct
                )
            }
            .filter { ws ->
                val isDone = statusMap[ws.chapter.id] in StudyConstants.DONE_STATUSES
                if (ws.attemptsCount == 0) !isDone else (ws.attemptsCount >= 2 && (ws.accuracyPct ?: 100) < 60)
            }
            .sortedBy { it.accuracyPct ?: -1 }
    }
}
