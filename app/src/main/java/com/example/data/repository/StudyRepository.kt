package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChapterStatusDoc
import com.example.data.model.CustomPyq
import com.example.data.model.ErrorLogEntry
import com.example.data.model.FlashcardProgress
import com.example.data.model.NoteImage
import com.example.data.model.PyqAttempt
import com.example.data.model.PyqQuestion
import com.example.data.model.StudyAnalytics
import com.example.data.model.StudyConstants
import com.example.data.model.StudyNote
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StudyRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun <T> observeCollection(
        uid: String,
        subcollection: String,
        query: Query,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val path = "users/$uid/$subcollection"
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeProfile(uid: String): Flow<UserProfile?> = flow {
        val docRef = userDoc(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeChapterStatuses(uid: String): Flow<Map<String, String>> =
        observeCollection(
            uid = uid,
            subcollection = "chapter_statuses",
            query = userDoc(uid).collection("chapter_statuses"),
            clazz = ChapterStatusDoc::class.java
        ).map { list -> list.associate { it.chapterId.ifBlank { it.id } to it.status } }

    fun observeTasks(uid: String): Flow<List<StudyTask>> =
        observeCollection(
            uid = uid,
            subcollection = "tasks",
            query = userDoc(uid).collection("tasks"),
            clazz = StudyTask::class.java
        )

    fun observeNotes(uid: String): Flow<List<StudyNote>> =
        observeCollection(
            uid = uid,
            subcollection = "notes",
            query = userDoc(uid).collection("notes"),
            clazz = StudyNote::class.java
        ).map { list ->
            list.sortedByDescending { it.updatedAt ?: it.createdAt }
        }

    fun observeNoteImages(uid: String): Flow<Map<String, String>> =
        observeCollection(
            uid = uid,
            subcollection = "note_images",
            query = userDoc(uid).collection("note_images"),
            clazz = NoteImage::class.java
        ).map { list -> list.associate { it.id to it.data } }

    fun observeAttempts(uid: String): Flow<List<PyqAttempt>> =
        observeCollection(
            uid = uid,
            subcollection = "attempts",
            query = userDoc(uid).collection("attempts"),
            clazz = PyqAttempt::class.java
        ).map { list -> list.sortedBy { it.createdAt } }

    fun observeErrors(uid: String): Flow<List<ErrorLogEntry>> =
        observeCollection(
            uid = uid,
            subcollection = "errors",
            query = userDoc(uid).collection("errors"),
            clazz = ErrorLogEntry::class.java
        ).map { list -> list.sortedByDescending { it.createdAt } }

    fun observeCardProgress(uid: String): Flow<Map<String, String>> =
        observeCollection(
            uid = uid,
            subcollection = "card_progress",
            query = userDoc(uid).collection("card_progress"),
            clazz = FlashcardProgress::class.java
        ).map { list -> list.associate { it.cardId.ifBlank { it.id } to it.status } }

    fun observeSessions(uid: String): Flow<List<StudySession>> =
        observeCollection(
            uid = uid,
            subcollection = "study_sessions",
            query = userDoc(uid).collection("study_sessions"),
            clazz = StudySession::class.java
        ).map { list -> list.sortedByDescending { it.createdAt } }

    fun observeCustomPyqs(uid: String): Flow<List<PyqQuestion>> =
        observeCollection(
            uid = uid,
            subcollection = "custom_pyqs",
            query = userDoc(uid).collection("custom_pyqs"),
            clazz = CustomPyq::class.java
        ).map { list -> list.map { it.toPyqQuestion() } }

    suspend fun ensureProfile(uid: String, defaultName: String) {
        val docRef = userDoc(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = defaultName.trim().take(40)
                val payload = mapOf(
                    "userId" to uid,
                    "name" to safeName,
                    "cls" to "11",
                    "exam" to "JEE",
                    "goalMinutes" to 180L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun saveProfile(
        uid: String,
        name: String,
        cls: String,
        exam: String,
        goalMinutes: Long
    ) {
        val docRef = userDoc(uid)
        val safeName = name.trim().take(40)
        val safeCls = if (cls in StudyConstants.CLASSES) cls else "11"
        val safeExam = if (exam in StudyConstants.EXAMS) exam else "JEE"
        val safeGoal = goalMinutes.coerceIn(15L, 900L)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "name" to safeName,
                        "cls" to safeCls,
                        "exam" to safeExam,
                        "goalMinutes" to safeGoal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to safeName,
                        "cls" to safeCls,
                        "exam" to safeExam,
                        "goalMinutes" to safeGoal,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun setChapterStatus(uid: String, chapterId: String, status: String) {
        require(chapterId.isNotBlank()) { "Chapter ID must not be blank" }
        val safeStatus = if (status in StudyConstants.CHAPTER_STATUSES) status else "Not Started"
        val docRef = userDoc(uid).collection("chapter_statuses").document(chapterId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "status" to safeStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "chapterId" to chapterId,
                        "status" to safeStatus,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun addTask(uid: String, text: String, subject: String): String {
        val cleanText = text.trim().take(120)
        require(cleanText.isNotEmpty()) { "Task text must not be empty" }
        val safeSub = if (subject in StudyConstants.TASK_SUBJECTS) subject else "General"
        val id = "tsk_${UUID.randomUUID()}"
        val docRef = userDoc(uid).collection("tasks").document(id)
        try {
            docRef.set(
                mapOf(
                    "text" to cleanText,
                    "subject" to safeSub,
                    "done" to false,
                    "date" to StudyAnalytics.todayKey(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleTask(uid: String, task: StudyTask) {
        val docRef = userDoc(uid).collection("tasks").document(task.id)
        val newDone = !task.done
        try {
            docRef.update(
                mapOf(
                    "done" to newDone,
                    "doneOn" to if (newDone) StudyAnalytics.todayKey() else null,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTask(uid: String, taskId: String) {
        val docRef = userDoc(uid).collection("tasks").document(taskId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun clearCompletedTasks(uid: String, tasks: List<StudyTask>) {
        val completed = tasks.filter { it.done }
        if (completed.isEmpty()) return
        try {
            completed.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { t -> batch.delete(userDoc(uid).collection("tasks").document(t.id)) }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/tasks")
            throw e
        }
    }

    suspend fun createNote(uid: String, title: String = "", subject: String = "General", body: String = ""): String {
        val id = "note_${UUID.randomUUID()}"
        val docRef = userDoc(uid).collection("notes").document(id)
        val safeSub = if (subject in StudyConstants.TASK_SUBJECTS) subject else "General"
        try {
            docRef.set(
                mapOf(
                    "title" to title.take(120),
                    "subject" to safeSub,
                    "body" to body.take(20000),
                    "imageIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateNote(uid: String, noteId: String, title: String, subject: String, body: String, imageIds: List<String>) {
        val docRef = userDoc(uid).collection("notes").document(noteId)
        val safeSub = if (subject in StudyConstants.TASK_SUBJECTS) subject else "General"
        try {
            docRef.update(
                mapOf(
                    "title" to title.take(120),
                    "subject" to safeSub,
                    "body" to body.take(20000),
                    "imageIds" to imageIds.take(6),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun addNoteImage(uid: String, note: StudyNote, dataUrl: String): String {
        require(note.imageIds.size < 6) { "Max 6 images per note" }
        val imgId = "img_${UUID.randomUUID()}"
        val imgRef = userDoc(uid).collection("note_images").document(imgId)
        val noteRef = userDoc(uid).collection("notes").document(note.id)
        try {
            val batch = db.batch()
            batch.set(
                imgRef,
                mapOf(
                    "data" to dataUrl,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                noteRef,
                mapOf(
                    "imageIds" to (note.imageIds + imgId).take(6),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            return imgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, noteRef.path)
            throw e
        }
    }

    suspend fun removeNoteImage(uid: String, note: StudyNote, imageId: String) {
        val imgRef = userDoc(uid).collection("note_images").document(imageId)
        val noteRef = userDoc(uid).collection("notes").document(note.id)
        try {
            val batch = db.batch()
            batch.update(
                noteRef,
                mapOf(
                    "imageIds" to note.imageIds.filterNot { it == imageId },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.delete(imgRef)
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, noteRef.path)
            throw e
        }
    }

    suspend fun deleteNote(uid: String, note: StudyNote) {
        val noteRef = userDoc(uid).collection("notes").document(note.id)
        try {
            val batch = db.batch()
            batch.delete(noteRef)
            note.imageIds.forEach { imgId ->
                batch.delete(userDoc(uid).collection("note_images").document(imgId))
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, noteRef.path)
            throw e
        }
    }

    suspend fun logAttemptAndSyncErrors(
        uid: String,
        question: PyqQuestion,
        selectedOption: Int,
        secs: Int,
        confidence: String,
        existingOpenErrorsForQid: List<ErrorLogEntry>
    ): Pair<String, String?> {
        val attemptId = "att_${UUID.randomUUID()}"
        val attemptRef = userDoc(uid).collection("attempts").document(attemptId)
        val isCorrect = selectedOption == question.answer
        val safeSecs = secs.coerceIn(1, 7200).toLong()
        val safeConf = if (confidence in StudyConstants.CONFIDENCE_LEVELS) confidence else "MEDIUM"
        val dateKey = StudyAnalytics.todayKey()
        var createdErrorId: String? = null

        try {
            val batch = db.batch()
            batch.set(
                attemptRef,
                mapOf(
                    "qid" to question.id,
                    "chapterId" to question.chapterId,
                    "subject" to question.subject,
                    "selectedOption" to selectedOption.coerceIn(0, 3).toLong(),
                    "correct" to isCorrect,
                    "secs" to safeSecs,
                    "confidence" to safeConf,
                    "errorCategory" to "",
                    "dateKey" to dateKey,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (!isCorrect && existingOpenErrorsForQid.isEmpty()) {
                val errId = "err_${UUID.randomUUID()}"
                createdErrorId = errId
                val errRef = userDoc(uid).collection("errors").document(errId)
                batch.set(
                    errRef,
                    mapOf(
                        "qid" to question.id,
                        "resolved" to false,
                        "category" to "",
                        "dateKey" to dateKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else if (isCorrect && existingOpenErrorsForQid.isNotEmpty()) {
                existingOpenErrorsForQid.forEach { entry ->
                    val errRef = userDoc(uid).collection("errors").document(entry.id)
                    batch.update(
                        errRef,
                        mapOf(
                            "resolved" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            return attemptId to createdErrorId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, attemptRef.path)
            throw e
        }
    }

    suspend fun setAttemptErrorCategory(
        uid: String,
        attemptId: String?,
        errorId: String?,
        category: String
    ) {
        val cleanCat = category.take(40)
        try {
            val batch = db.batch()
            if (!attemptId.isNullOrBlank()) {
                batch.update(
                    userDoc(uid).collection("attempts").document(attemptId),
                    mapOf(
                        "errorCategory" to cleanCat,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (!errorId.isNullOrBlank()) {
                batch.update(
                    userDoc(uid).collection("errors").document(errorId),
                    mapOf(
                        "category" to cleanCat,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/attempts")
            throw e
        }
    }

    suspend fun toggleErrorResolved(uid: String, entry: ErrorLogEntry) {
        val docRef = userDoc(uid).collection("errors").document(entry.id)
        try {
            docRef.update(
                mapOf(
                    "resolved" to !entry.resolved,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteErrorEntry(uid: String, errorId: String) {
        val docRef = userDoc(uid).collection("errors").document(errorId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun setFlashcardStatus(uid: String, cardId: String, status: String) {
        val safeStatus = if (status == "known") "known" else "review"
        val docRef = userDoc(uid).collection("card_progress").document(cardId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "status" to safeStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "cardId" to cardId,
                        "status" to safeStatus,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun logStudySession(uid: String, minutes: Int, tag: String): String {
        val safeMins = minutes.coerceIn(1, 900).toLong()
        val cleanTag = tag.trim().ifEmpty { "Untagged" }.take(80)
        val id = "ses_${UUID.randomUUID()}"
        val docRef = userDoc(uid).collection("study_sessions").document(id)
        try {
            docRef.set(
                mapOf(
                    "dateKey" to StudyAnalytics.todayKey(),
                    "minutes" to safeMins,
                    "tag" to cleanTag,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun importCustomQuestions(uid: String, questions: List<PyqQuestion>) {
        if (questions.isEmpty()) return
        try {
            questions.chunked(200).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { q ->
                    val docRef = userDoc(uid).collection("custom_pyqs").document(q.id)
                    val payload = buildMap<String, Any> {
                        put("exam", q.exam)
                        if (q.year != null) put("year", q.year.toLong())
                        put("subject", q.subject)
                        put("chapterId", q.chapterId)
                        put("difficulty", q.difficulty)
                        put("tags", q.tags)
                        put("question", q.q)
                        put("options", q.options)
                        put("answerIndex", q.answer.toLong())
                        put("explain", q.explain)
                        put("createdAt", FieldValue.serverTimestamp())
                    }
                    batch.set(docRef, payload)
                }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/custom_pyqs")
            throw e
        }
    }

    suspend fun clearCustomQuestions(uid: String, questionIds: List<String>) {
        if (questionIds.isEmpty()) return
        try {
            questionIds.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id -> batch.delete(userDoc(uid).collection("custom_pyqs").document(id)) }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/custom_pyqs")
            throw e
        }
    }

    suspend fun resetAllUserData(uid: String) {
        val subcollections = listOf(
            "chapter_statuses", "tasks", "notes", "note_images",
            "attempts", "errors", "card_progress", "study_sessions", "custom_pyqs"
        )
        try {
            for (sub in subcollections) {
                val docs = userDoc(uid).collection(sub).get().await().documents
                docs.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }
            saveProfile(uid, "", "11", "JEE", 180L)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            throw e
        }
    }
}
