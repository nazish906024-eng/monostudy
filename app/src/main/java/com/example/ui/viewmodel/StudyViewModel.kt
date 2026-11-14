package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BundledFlashcards
import com.example.data.model.Chapter
import com.example.data.model.ErrorLogEntry
import com.example.data.model.PyqAttempt
import com.example.data.model.PyqQuestion
import com.example.data.model.StudyAnalytics
import com.example.data.model.StudyConstants
import com.example.data.model.StudyNote
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.UserProfile
import com.example.data.repository.CatalogAssetLoader
import com.example.data.repository.StudyRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PyqAnswerOutcome(
    val selectedOption: Int,
    val correct: Boolean,
    val secs: Int,
    val attemptId: String,
    val errorId: String?,
    val selectedErrorCategory: String = ""
)

data class PomodoroUiState(
    val presetMinutes: Int = 25,
    val secondsLeft: Int = 25 * 60,
    val isRunning: Boolean = false,
    val statusLabel: String = "READY TO FOCUS",
    val tag: String = "",
    val soundEnabled: Boolean = true
)

data class StudyDashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val profile: UserProfile = UserProfile(),
    val chapters: List<Chapter> = emptyList(),
    val chapterMap: Map<String, Chapter> = emptyMap(),
    val chapterStatuses: Map<String, String> = emptyMap(),
    val tasks: List<StudyTask> = emptyList(),
    val notes: List<StudyNote> = emptyList(),
    val noteImages: Map<String, String> = emptyMap(),
    val attempts: List<PyqAttempt> = emptyList(),
    val errors: List<ErrorLogEntry> = emptyList(),
    val cardProgress: Map<String, String> = emptyMap(),
    val sessions: List<StudySession> = emptyList(),
    val basePyqs: List<PyqQuestion> = emptyList(),
    val customPyqs: List<PyqQuestion> = emptyList(),
    val allPyqs: List<PyqQuestion> = emptyList(),
    val pyqNotice: String = ""
)

@Serializable
data class StudyBackupPayload(
    val v: Int = 2,
    val uid: String = "",
    val profile: BackupProfile = BackupProfile(),
    val status: Map<String, String> = emptyMap(),
    val tasks: List<BackupTask> = emptyList(),
    val notes: List<BackupNote> = emptyList(),
    val sessions: List<BackupSession> = emptyList(),
    val customPyq: List<PyqQuestion> = emptyList()
)

@Serializable
data class BackupProfile(
    val name: String = "",
    val cls: String = "11",
    val exam: String = "JEE",
    val goal: Int = 180
)

@Serializable
data class BackupTask(
    val text: String = "",
    val subject: String = "General",
    val done: Boolean = false,
    val date: String = ""
)

@Serializable
data class BackupNote(
    val title: String = "",
    val subject: String = "General",
    val body: String = ""
)

@Serializable
data class BackupSession(
    val date: String = "",
    val m: Int = 25,
    val tag: String = ""
)

@Serializable
data class QuestionTemplatePayload(
    val notice: String,
    val questions: List<PyqQuestion>
)

class StudyViewModel(
    private val repository: StudyRepository,
    assetLoader: CatalogAssetLoader,
    val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    private val chapterCatalog = assetLoader.loadChapters()
    private val pyqCatalog = assetLoader.loadBundledPyqs()
    private val chapterMap = chapterCatalog.chapters.associateBy { it.id }

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _pomodoro = MutableStateFlow(PomodoroUiState())
    val pomodoro: StateFlow<PomodoroUiState> = _pomodoro.asStateFlow()

    private val _answeredPyqs = MutableStateFlow<Map<String, PyqAnswerOutcome>>(emptyMap())
    val answeredPyqs: StateFlow<Map<String, PyqAnswerOutcome>> = _answeredPyqs.asStateFlow()

    private val qStartTimes = mutableMapOf<String, Long>()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                repository.ensureProfile(currentUserId, initialDisplayName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "ensureProfile failed: ${e.message}", e)
            }
        }
    }

    private fun <T> FlowWrapper(flow: kotlinx.coroutines.flow.Flow<T>, defaultValue: T) =
        flow.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Firestore stream error: ${root.message}", root)
            emit(defaultValue)
        }

    private val profileFlow = FlowWrapper(repository.observeProfile(currentUserId), null)
    private val statusesFlow = FlowWrapper(repository.observeChapterStatuses(currentUserId), emptyMap())
    private val tasksFlow = FlowWrapper(repository.observeTasks(currentUserId), emptyList())
    private val notesFlow = FlowWrapper(repository.observeNotes(currentUserId), emptyList())
    private val noteImagesFlow = FlowWrapper(repository.observeNoteImages(currentUserId), emptyMap())
    private val attemptsFlow = FlowWrapper(repository.observeAttempts(currentUserId), emptyList())
    private val errorsFlow = FlowWrapper(repository.observeErrors(currentUserId), emptyList())
    private val cardProgressFlow = FlowWrapper(repository.observeCardProgress(currentUserId), emptyMap())
    private val sessionsFlow = FlowWrapper(repository.observeSessions(currentUserId), emptyList())
    private val customPyqsFlow = FlowWrapper(repository.observeCustomPyqs(currentUserId), emptyList())

    private val corePart1 = combine(
        profileFlow,
        statusesFlow,
        tasksFlow,
        notesFlow,
        noteImagesFlow
    ) { profile, statuses, tasks, notes, images ->
        CorePart1(profile, statuses, tasks, notes, images)
    }

    private val corePart2 = combine(
        attemptsFlow,
        errorsFlow,
        cardProgressFlow,
        sessionsFlow,
        customPyqsFlow
    ) { attempts, errors, cards, sessions, customPyqs ->
        CorePart2(attempts, errors, cards, sessions, customPyqs)
    }

    val uiState: StateFlow<StudyDashboardUiState> = combine(corePart1, corePart2) { p1, p2 ->
        val mergedPyqs = LinkedHashMap<String, PyqQuestion>().apply {
            pyqCatalog.questions.forEach { put(it.id, it) }
            p2.customPyqs.forEach { put(it.id, it) }
        }.values.toList()

        StudyDashboardUiState(
            isLoading = false,
            errorMessage = null,
            profile = p1.profile ?: UserProfile(userId = currentUserId, name = initialDisplayName),
            chapters = chapterCatalog.chapters,
            chapterMap = chapterMap,
            chapterStatuses = p1.statuses,
            tasks = p1.tasks,
            notes = p1.notes,
            noteImages = p1.images,
            attempts = p2.attempts,
            errors = p2.errors,
            cardProgress = p2.cards,
            sessions = p2.sessions,
            basePyqs = pyqCatalog.questions,
            customPyqs = p2.customPyqs,
            allPyqs = mergedPyqs,
            pyqNotice = pyqCatalog.notice
        )
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        emit(
            StudyDashboardUiState(
                isLoading = false,
                errorMessage = root.message ?: "Failed to load study data",
                chapters = chapterCatalog.chapters,
                chapterMap = chapterMap,
                basePyqs = pyqCatalog.questions,
                allPyqs = pyqCatalog.questions,
                pyqNotice = pyqCatalog.notice
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StudyDashboardUiState(
            isLoading = true,
            chapters = chapterCatalog.chapters,
            chapterMap = chapterMap,
            basePyqs = pyqCatalog.questions,
            allPyqs = pyqCatalog.questions,
            pyqNotice = pyqCatalog.notice
        )
    )

    private data class CorePart1(
        val profile: UserProfile?,
        val statuses: Map<String, String>,
        val tasks: List<StudyTask>,
        val notes: List<StudyNote>,
        val images: Map<String, String>
    )

    private data class CorePart2(
        val attempts: List<PyqAttempt>,
        val errors: List<ErrorLogEntry>,
        val cards: Map<String, String>,
        val sessions: List<StudySession>,
        val customPyqs: List<PyqQuestion>
    )

    private fun emitToast(msg: String) {
        _toastEvents.tryEmit(msg)
    }

    fun markQuestionVisible(qid: String) {
        if (!qStartTimes.containsKey(qid)) {
            qStartTimes[qid] = System.currentTimeMillis()
        }
    }

    fun saveProfile(name: String, cls: String, exam: String, goalMinutes: Long) {
        if (goalMinutes !in 15L..900L) {
            emitToast("Goal must be 15–900 minutes")
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.saveProfile(currentUserId, name, cls, exam, goalMinutes)
            }.onSuccess {
                emitToast("Profile saved")
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun updateProfileSelection(cls: String, exam: String) {
        val current = uiState.value.profile
        viewModelScope.launch {
            runCatching {
                repository.saveProfile(currentUserId, current.name, cls, exam, current.goalMinutes)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to update settings")
            }
        }
    }

    fun setChapterStatus(chapterId: String, status: String) {
        viewModelScope.launch {
            runCatching {
                repository.setChapterStatus(currentUserId, chapterId, status)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to update chapter status")
            }
        }
    }

    fun answerPyq(question: PyqQuestion, selectedOption: Int, confidence: String) {
        if (_answeredPyqs.value.containsKey(question.id)) return
        val started = qStartTimes[question.id] ?: System.currentTimeMillis()
        val secs = ((System.currentTimeMillis() - started) / 1000L).toInt().coerceIn(1, 7200)
        val openErrors = uiState.value.errors.filter { it.qid == question.id && !it.resolved }

        viewModelScope.launch {
            runCatching {
                repository.logAttemptAndSyncErrors(
                    uid = currentUserId,
                    question = question,
                    selectedOption = selectedOption,
                    secs = secs,
                    confidence = confidence,
                    existingOpenErrorsForQid = openErrors
                )
            }.onSuccess { (attemptId, errorId) ->
                _answeredPyqs.update { map ->
                    map + (question.id to PyqAnswerOutcome(
                        selectedOption = selectedOption,
                        correct = selectedOption == question.answer,
                        secs = secs,
                        attemptId = attemptId,
                        errorId = errorId
                    ))
                }
            }.onFailure {
                emitToast(it.localizedMessage ?: "Could not save attempt")
            }
        }
    }

    fun setPyqErrorCategory(qid: String, category: String) {
        val outcome = _answeredPyqs.value[qid] ?: return
        viewModelScope.launch {
            runCatching {
                repository.setAttemptErrorCategory(currentUserId, outcome.attemptId, outcome.errorId, category)
            }.onSuccess {
                _answeredPyqs.update { map ->
                    map + (qid to outcome.copy(selectedErrorCategory = category))
                }
                emitToast("Error category saved")
            }.onFailure {
                emitToast(it.localizedMessage ?: "Could not save error category")
            }
        }
    }

    fun setFlashcardStatus(cardId: String, status: String) {
        viewModelScope.launch {
            runCatching {
                repository.setFlashcardStatus(currentUserId, cardId, status)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to save card status")
            }
        }
    }

    fun toggleErrorResolved(entry: ErrorLogEntry) {
        viewModelScope.launch {
            runCatching {
                repository.toggleErrorResolved(currentUserId, entry)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to update error log")
            }
        }
    }

    fun deleteErrorEntry(errorId: String) {
        viewModelScope.launch {
            runCatching {
                repository.deleteErrorEntry(currentUserId, errorId)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to remove entry")
            }
        }
    }

    fun createNote(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.createNote(currentUserId)
            }.onSuccess { id ->
                onCreated(id)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to create note")
            }
        }
    }

    fun updateNote(note: StudyNote, title: String, subject: String, body: String) {
        viewModelScope.launch {
            runCatching {
                repository.updateNote(currentUserId, note.id, title, subject, body, note.imageIds)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to save note")
            }
        }
    }

    fun deleteNote(note: StudyNote, onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.deleteNote(currentUserId, note)
            }.onSuccess {
                onDeleted()
                emitToast("Note deleted")
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to delete note")
            }
        }
    }

    fun attachNoteImage(context: Context, note: StudyNote, uri: Uri) {
        if (note.imageIds.size >= 6) {
            emitToast("Max 6 images per note")
            return
        }
        viewModelScope.launch {
            val encodedResult = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("Could not open image")
                    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: throw IllegalArgumentException("Invalid image format")
                    val maxDim = max(original.width, original.height).toFloat()
                    val scale = min(1f, 800f / maxDim)
                    val targetW = max(1, (original.width * scale).roundToInt())
                    val targetH = max(1, (original.height * scale).roundToInt())
                    val outBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(outBitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)
                    canvas.drawBitmap(scaled, 0f, 0f, null)
                    val baos = ByteArrayOutputStream()
                    outBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                    val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    "data:image/jpeg;base64,$base64"
                }
            }
            encodedResult.onSuccess { dataUrl ->
                runCatching {
                    repository.addNoteImage(currentUserId, note, dataUrl)
                }.onSuccess {
                    emitToast("Image attached")
                }.onFailure {
                    emitToast(it.localizedMessage ?: "Failed to upload image")
                }
            }.onFailure {
                emitToast("Could not read that image")
            }
        }
    }

    fun removeNoteImage(note: StudyNote, imageId: String) {
        viewModelScope.launch {
            runCatching {
                repository.removeNoteImage(currentUserId, note, imageId)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to remove image")
            }
        }
    }

    fun addTask(text: String, subject: String) {
        if (text.trim().isEmpty()) {
            emitToast("Type a task first")
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.addTask(currentUserId, text, subject)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to add task")
            }
        }
    }

    fun toggleTask(task: StudyTask) {
        viewModelScope.launch {
            runCatching {
                repository.toggleTask(currentUserId, task)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to update task")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            runCatching {
                repository.deleteTask(currentUserId, taskId)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to delete task")
            }
        }
    }

    fun clearCompletedTasks() {
        val tasks = uiState.value.tasks
        viewModelScope.launch {
            runCatching {
                repository.clearCompletedTasks(currentUserId, tasks)
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to clear tasks")
            }
        }
    }

    fun setPomodoroTag(tag: String) {
        _pomodoro.update { it.copy(tag = tag) }
    }

    fun setPomodoroSound(enabled: Boolean) {
        _pomodoro.update { it.copy(soundEnabled = enabled) }
    }

    fun selectPomodoroPreset(minutes: Int) {
        timerJob?.cancel()
        _pomodoro.update {
            it.copy(
                presetMinutes = minutes,
                secondsLeft = minutes * 60,
                isRunning = false,
                statusLabel = "READY TO FOCUS"
            )
        }
    }

    fun togglePomodoroRun() {
        val current = _pomodoro.value
        if (current.isRunning) {
            timerJob?.cancel()
            _pomodoro.update { it.copy(isRunning = false, statusLabel = "PAUSED") }
            return
        }
        _pomodoro.update { it.copy(isRunning = true, statusLabel = "FOCUSING") }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_pomodoro.value.secondsLeft > 0) {
                delay(1000L)
                _pomodoro.update { st -> st.copy(secondsLeft = (st.secondsLeft - 1).coerceAtLeast(0)) }
            }
            val finished = _pomodoro.value
            if (finished.soundEnabled) playCompletionBeep()
            logStudyMinutes(finished.presetMinutes, finished.tag)
            emitToast("Session complete — ${finished.presetMinutes} min logged")
            _pomodoro.update {
                it.copy(
                    secondsLeft = it.presetMinutes * 60,
                    isRunning = false,
                    statusLabel = "SESSION COMPLETE"
                )
            }
        }
    }

    fun resetPomodoro() {
        timerJob?.cancel()
        _pomodoro.update {
            it.copy(
                secondsLeft = it.presetMinutes * 60,
                isRunning = false,
                statusLabel = "READY TO FOCUS"
            )
        }
    }

    private fun playCompletionBeep() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
        } catch (_: Exception) {
        }
    }

    fun logStudyMinutes(minutes: Int, tag: String) {
        viewModelScope.launch {
            runCatching {
                repository.logStudySession(currentUserId, minutes, tag)
            }.onSuccess {
                emitToast("$minutes minutes logged")
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to log study time")
            }
        }
    }

    fun importQuestionsFromJson(rawJson: String) {
        viewModelScope.launch {
            runCatching {
                val parsed = CatalogAssetLoader.parseCustomQuestionsJson(rawJson, chapterMap.keys)
                if (parsed.valid.isEmpty()) {
                    emitToast("No valid questions found (${parsed.rejectedCount} rejected)")
                    return@launch
                }
                repository.importCustomQuestions(currentUserId, parsed.valid)
                val rejNote = if (parsed.rejectedCount > 0) ", ${parsed.rejectedCount} rejected" else ""
                emitToast("Imported ${parsed.valid.size} question(s)$rejNote")
            }.onFailure {
                emitToast(it.localizedMessage ?: "That file is not valid JSON")
            }
        }
    }

    fun clearCustomQuestions() {
        val ids = uiState.value.customPyqs.map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                repository.clearCustomQuestions(currentUserId, ids)
            }.onSuccess {
                emitToast("Imported questions removed")
            }.onFailure {
                emitToast(it.localizedMessage ?: "Failed to remove imported questions")
            }
        }
    }

    fun buildQuestionTemplateJson(): String {
        val sample = PyqQuestion(
            id = "my_phy_001",
            exam = "JEE",
            year = null,
            subject = "Physics",
            chapterId = "class-11-physics-02",
            difficulty = "Easy",
            tags = listOf("kinematics"),
            q = "A car accelerates from rest at ${'$'}2\\,\\text{m/s}^2${'$'} for ${'$'}5\\,\\text{s}${'$'}. Final speed?",
            options = listOf("5 m/s", "10 m/s", "15 m/s", "20 m/s"),
            answer = 1,
            explain = "${'$'}v=u+at=10\\,\\text{m/s}${'$'}."
        )
        return Json { prettyPrint = true }.encodeToString(
            QuestionTemplatePayload(
                notice = "My own practice questions",
                questions = listOf(sample)
            )
        )
    }

    fun buildBackupJson(): String {
        val st = uiState.value
        val payload = StudyBackupPayload(
            uid = currentUserId,
            profile = BackupProfile(
                name = st.profile.name,
                cls = st.profile.cls,
                exam = st.profile.exam,
                goal = st.profile.goalMinutes.toInt()
            ),
            status = st.chapterStatuses,
            tasks = st.tasks.map { BackupTask(it.text, it.subject, it.done, it.date) },
            notes = st.notes.map { BackupNote(it.title, it.subject, it.body) },
            sessions = st.sessions.map { BackupSession(it.dateKey, it.minutes.toInt(), it.tag) },
            customPyq = st.customPyqs
        )
        return Json { prettyPrint = true }.encodeToString(payload)
    }

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            runCatching {
                val json = Json { ignoreUnknownKeys = true; isLenient = true }
                val payload = json.decodeFromString<StudyBackupPayload>(rawJson)
                repository.saveProfile(
                    currentUserId,
                    payload.profile.name,
                    payload.profile.cls,
                    payload.profile.exam,
                    payload.profile.goal.toLong()
                )
                payload.status.forEach { (chId, status) ->
                    if (chId in chapterMap) {
                        repository.setChapterStatus(currentUserId, chId, status)
                    }
                }
                payload.tasks.take(50).forEach { t ->
                    if (t.text.isNotBlank()) repository.addTask(currentUserId, t.text, t.subject)
                }
                payload.notes.take(50).forEach { n ->
                    repository.createNote(currentUserId, n.title, n.subject, n.body)
                }
                payload.sessions.take(100).forEach { s ->
                    if (s.m > 0) repository.logStudySession(currentUserId, s.m, s.tag)
                }
                if (payload.customPyq.isNotEmpty()) {
                    repository.importCustomQuestions(currentUserId, payload.customPyq)
                }
            }.onSuccess {
                emitToast("Backup imported")
            }.onFailure {
                emitToast("Invalid backup file")
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            runCatching {
                repository.resetAllUserData(currentUserId)
                _answeredPyqs.value = emptyMap()
            }.onSuccess {
                emitToast("Data reset")
            }.onFailure {
                emitToast(it.localizedMessage ?: "Reset failed")
            }
        }
    }

    suspend fun generateWeeklySummaryPng(context: Context): File = withContext(Dispatchers.IO) {
        val st = uiState.value
        val days = StudyAnalytics.lastNDays(7)
        val keys = days.map { StudyAnalytics.todayKey(it) }.toSet()
        val mins = keys.sumOf { StudyAnalytics.minutesOnDate(st.sessions, it) }
        val weekAttempts = st.attempts.filter { it.dateKey in keys }
        val okAttempts = weekAttempts.count { it.correct }
        val accText = if (weekAttempts.isEmpty()) "—" else "${StudyAnalytics.pct(okAttempts, weekAttempts.size)}%"
        val streakInfo = StudyAnalytics.streak(st.sessions, st.attempts, st.tasks)

        val w = 900
        val h = 500
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                android.graphics.Color.parseColor("#1B1647"),
                android.graphics.Color.parseColor("#0B1020"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#8B7DFF")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("monostudy · WEEKLY SUMMARY", 50f, 70f, eyebrowPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(st.profile.name.ifBlank { "Student" }, 50f, 130f, namePaint)

        val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#8F9BC4")
            textSize = 20f
        }
        canvas.drawText("${days.first().format(fmt)} – ${days.last().format(fmt)}", 50f, 165f, subPaint)

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#141B33")
        }
        val cardLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#8F9BC4")
            textSize = 18f
        }
        val cardValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val tiles = listOf(
            "Minutes studied" to "$mins",
            "PYQs attempted" to "${weekAttempts.size}",
            "Accuracy" to accText,
            "Streak" to "${streakInfo.days} days"
        )
        tiles.forEachIndexed { i, (lbl, v) ->
            val px = 50f + (i % 2) * 420f
            val py = 225f + (i / 2) * 120f
            canvas.drawRoundRect(RectF(px, py, px + 380f, py + 98f), 14f, 14f, cardBgPaint)
            canvas.drawText(lbl, px + 22f, py + 34f, cardLabelPaint)
            canvas.drawText(v, px + 22f, py + 78f, cardValPaint)
        }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "monostudy-weekly-summary.png")
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    private companion object {
        const val TAG = "StudyViewModel"
    }
}
